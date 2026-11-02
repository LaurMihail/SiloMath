import test from "node:test";
import assert from "node:assert/strict";

import {
  ApiFailure,
  apiErrorResponse,
  canonicalRequestFingerprint,
  correlationIdForRequest,
  executeIdempotently,
  publicConfiguration,
  requireAuthenticatedSubject,
  requireOrganizationContext,
  safeLog,
  validationFailure,
} from "./mod.ts";
import type {
  IdempotencyOutcome,
  IdempotencyRequest,
  TransactionalIdempotencyPort,
} from "./mod.ts";

const request = (headers: Record<string, string> = {}) =>
  new Request("https://example.test/api/v1/resource", { headers });

test("authentication rejects a missing bearer", async () => {
  await assert.rejects(
    requireAuthenticatedSubject(request(), async () => ({ sub: "ignored" })),
    (error: unknown) =>
      error instanceof ApiFailure &&
      error.status === 401 &&
      error.code === "AUTH_INVALID_CREDENTIALS",
  );
});

test("authentication rejects an invalid bearer", async () => {
  await assert.rejects(
    requireAuthenticatedSubject(
      request({ Authorization: "Bearer invalid" }),
      async () => {
        throw new Error("JWT details must not escape");
      },
    ),
    (error: unknown) =>
      error instanceof ApiFailure &&
      error.status === 401 &&
      !error.message.includes("JWT details"),
  );
});

test("authentication uses only the verified JWT subject", async () => {
  const subject = await requireAuthenticatedSubject(
    request({
      Authorization: "Bearer valid",
      "X-User-Id": "forged-user",
      "X-Role": "ADMIN",
      "X-Organization-Id": "forged-org",
    }),
    async () => ({ sub: "verified-user", role: "STANDARD" }),
  );
  assert.deepEqual(subject, { userId: "verified-user" });
});

test("tenant context uses server membership and ignores client authority", async () => {
  const context = await requireOrganizationContext(
    "requested-org",
    { userId: "verified-user" },
    async (organizationId, userId) => {
      assert.equal(organizationId, "requested-org");
      assert.equal(userId, "verified-user");
      return { role: "STANDARD", active: true };
    },
  );
  assert.deepEqual(context, {
    organizationId: "requested-org",
    userId: "verified-user",
    role: "STANDARD",
  });
});

test("tenant context denies absent server membership", async () => {
  await assert.rejects(
    requireOrganizationContext(
      "forged-org",
      { userId: "verified-user" },
      async () => null,
    ),
    (error: unknown) =>
      error instanceof ApiFailure &&
      error.code === "TENANT_SCOPE_VIOLATION" &&
      error.status === 403,
  );
});

test("correlation ID propagates supplied value and generates fallback", () => {
  assert.equal(
    correlationIdForRequest(request({ "X-Correlation-Id": " client-id " })),
    "client-id",
  );
  assert.equal(correlationIdForRequest(request(), () => "generated-id"), "generated-id");
});

test("canonical validation error preserves correlation in header and body", async () => {
  const response = apiErrorResponse(validationFailure("code"), "corr-validation");
  assert.equal(response.status, 422);
  assert.equal(response.headers.get("X-Correlation-Id"), "corr-validation");
  assert.deepEqual(await response.json(), {
    code: "VALIDATION_FAILED",
    message: "Request validation failed.",
    fieldErrors: [{ field: "code", code: "INVALID", message: "Invalid value." }],
    correlationId: "corr-validation",
    retryable: false,
  });
});

test("canonical unauthorized forbidden and conflict errors retain taxonomy", async () => {
  for (const [status, code] of [
    [401, "AUTH_INVALID_CREDENTIALS"],
    [403, "AUTH_FORBIDDEN"],
    [409, "IDEMPOTENCY_KEY_REUSE_MISMATCH"],
  ] as const) {
    const response = apiErrorResponse(
      new ApiFailure(status, code, "Safe message."),
      `corr-${status}`,
    );
    const body = await response.json() as { code: string; correlationId: string };
    assert.equal(body.code, code);
    assert.equal(body.correlationId, `corr-${status}`);
  }
});

test("internal failures are sanitized", async () => {
  const response = apiErrorResponse(
    new ApiFailure(
      500,
      "INTERNAL_ERROR",
      "SQLSTATE 23505 secret-token stack trace",
      false,
      [],
      { sql: "private" },
    ),
    "corr-internal",
  );
  const serialized = JSON.stringify(await response.json());
  assert.equal(response.status, 500);
  assert.doesNotMatch(serialized, /SQLSTATE|secret-token|stack trace|private/);
  assert.match(serialized, /INTERNAL_ERROR/);
});

test("privileged secrets are neither returned nor accepted by safe logging", () => {
  const secret = "service-role-secret";
  const config = publicConfiguration({
    get(name) {
      return name === "SUPABASE_URL" ? "https://project.supabase.co" : secret;
    },
  });
  assert.deepEqual(config, { supabaseUrl: "https://project.supabase.co" });

  const events: unknown[] = [];
  safeLog((event) => events.push(event), {
    event: "request.completed",
    correlationId: "corr-log",
    outcome: "OK",
  });
  assert.doesNotMatch(JSON.stringify({ config, events }), /service-role-secret/);
});

class TransactionFixture implements TransactionalIdempotencyPort {
  private readonly durable = new Map<string, { fingerprint: string; value: unknown }>();

  async executeAtomically<T>(
    input: IdempotencyRequest,
    mutation: () => Promise<T>,
  ): Promise<IdempotencyOutcome<T>> {
    const composite = `${input.namespace}:${input.key}`;
    const existing = this.durable.get(composite);
    if (existing) {
      return existing.fingerprint === input.fingerprint
        ? { kind: "REPLAY", value: existing.value as T }
        : { kind: "CONFLICT" };
    }
    const value = await mutation();
    this.durable.set(composite, { fingerprint: input.fingerprint, value });
    return { kind: "EXECUTED", value };
  }
}

test("canonical fingerprint is stable across object key order", async () => {
  assert.equal(
    await canonicalRequestFingerprint({ b: 2, a: { y: 2, x: 1 } }),
    await canonicalRequestFingerprint({ a: { x: 1, y: 2 }, b: 2 }),
  );
});

test("idempotency exact replay returns durable authoritative result", async () => {
  const transaction = new TransactionFixture();
  let calls = 0;
  const first = await executeIdempotently(
    transaction,
    "storage-space:create",
    "stable-key",
    { code: "A", description: null },
    async () => ({ id: "authoritative", call: ++calls }),
  );
  const replay = await executeIdempotently(
    transaction,
    "storage-space:create",
    "stable-key",
    { description: null, code: "A" },
    async () => ({ id: "duplicate", call: ++calls }),
  );
  assert.equal(first.kind, "EXECUTED");
  assert.deepEqual(replay, {
    kind: "REPLAY",
    value: { id: "authoritative", call: 1 },
  });
  assert.equal(calls, 1);
});

test("idempotency same key with different fingerprint conflicts", async () => {
  const transaction = new TransactionFixture();
  await executeIdempotently(
    transaction,
    "mutation",
    "same-key",
    { value: 1 },
    async () => "first",
  );
  const mismatch = await executeIdempotently(
    transaction,
    "mutation",
    "same-key",
    { value: 2 },
    async () => "second",
  );
  assert.deepEqual(mismatch, { kind: "CONFLICT" });
});

test("idempotency production boundary requires one atomic transaction callback", async () => {
  let received: IdempotencyRequest | undefined;
  const port: TransactionalIdempotencyPort = {
    async executeAtomically(input, mutation) {
      received = input;
      return { kind: "EXECUTED", value: await mutation() };
    },
  };
  const outcome = await executeIdempotently(
    port,
    "campaign:launch",
    "operation-id",
    { campaignId: "canonical" },
    async () => "committed",
  );
  assert.equal(outcome.kind, "EXECUTED");
  assert.equal(received?.namespace, "campaign:launch");
  assert.match(received?.fingerprint ?? "", /^[a-f0-9]{64}$/);
});
