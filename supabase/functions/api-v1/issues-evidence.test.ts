import assert from "node:assert/strict"
import { createApiHandler } from "./index.ts"

const actor = "11111111-1111-4111-8111-111111111111"
const organization = "22222222-2222-4222-8222-222222222222"
const campaign = "33333333-3333-4333-8333-333333333333"
const record = "44444444-4444-4444-8444-444444444444"
const issue = "55555555-5555-4555-8555-555555555555"
const evidence = "66666666-6666-4666-8666-666666666666"
const operation = "77777777-7777-4777-8777-777777777777"
const key = "88888888-8888-4888-8888-888888888888"

type Dependencies = NonNullable<Parameters<typeof createApiHandler>[0]>
type Call = { sql: string; values: unknown[] }

function jsonRequest(path: string, method: string, body: Record<string, unknown>, authenticated = true) {
  return new Request(`https://example.test/functions/v1/api-v1${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": key,
      "X-Correlation-Id": "issues-evidence-test",
      ...(authenticated ? { Authorization: "Bearer test" } : {}),
    },
    body: JSON.stringify(body),
  })
}

function harness(resolver: (sql: string, values: unknown[]) => unknown[]) {
  const calls: Call[] = []
  const uploads: { path: string; bytes: Uint8Array }[] = []
  const downloads: string[] = []
  const database = ((strings: TemplateStringsArray, ...values: unknown[]) => {
    const sql = strings.join("?")
    calls.push({ sql, values })
    return Promise.resolve(resolver(sql, values))
  }) as unknown as Dependencies["database"]
  const handler = createApiHandler({
    authenticate: (req) => Promise.resolve(
      req.headers.get("Authorization") ? ({ id: actor } as never) : null,
    ),
    database,
    evidenceStore: {
      upload(path, bytes) {
        uploads.push({ path, bytes })
        return Promise.resolve()
      },
      download(path) {
        downloads.push(path)
        return Promise.resolve(new Uint8Array([1, 2, 3]))
      },
    },
  })
  return { handler, calls, uploads, downloads }
}

Deno.test("Issue create requires authentication before database access", async () => {
  const { handler, calls } = harness(() => [])
  const response = await handler(jsonRequest(
    `/organizations/${organization}/campaigns/${campaign}/stocktaking/${record}/issues`,
    "POST",
    {
      issueId: issue,
      category: "OTHER",
      description: "Observed condition",
      clientOperationId: operation,
    },
    false,
  ))
  assert.equal(response.status, 401)
  assert.equal((await response.json()).code, "AUTH_SESSION_EXPIRED")
  assert.equal(calls.length, 0)
})

Deno.test("Issue create validates and delegates one idempotent PostgreSQL command", async () => {
  const expected = { issueId: issue, status: "OPEN", entityVersion: 1 }
  const { handler, calls } = harness(() => [{ result: { status: 201, body: expected } }])
  const response = await handler(jsonRequest(
    `/organizations/${organization}/campaigns/${campaign}/stocktaking/${record}/issues`,
    "POST",
    {
      issueId: issue,
      category: "MEASUREMENT_PROBLEM",
      description: "Reading cannot be verified",
      clientOperationId: operation,
    },
  ))
  assert.equal(response.status, 201)
  assert.deepEqual(await response.json(), expected)
  assert.match(calls[0].sql, /silomath\.issue_create_command/)
  assert.deepEqual(calls[0].values.slice(0, 6), [
    actor, organization, campaign, record, issue, "MEASUREMENT_PROBLEM",
  ])
})

Deno.test("Issue create rejects incomplete domain input without database effects", async () => {
  const { handler, calls } = harness(() => [])
  const response = await handler(jsonRequest(
    `/organizations/${organization}/campaigns/${campaign}/stocktaking/${record}/issues`,
    "POST",
    {
      issueId: issue,
      category: "UNKNOWN",
      description: " ",
      clientOperationId: operation,
    },
  ))
  assert.equal(response.status, 422)
  assert.equal(calls.length, 0)
})

Deno.test("Issue transition preserves entityVersion and close-follow-up context", async () => {
  for (const [suffix, target, context] of [
    [":resolve", "RESOLVED", "NORMAL"],
    [":reopen", "OPEN", null],
    [":accept-follow-up", "RESOLVED", "CLOSE_FOLLOW_UP_ACCEPTANCE"],
  ] as const) {
    const { handler, calls } = harness(() => [{
      result: { status: 200, body: { issueId: issue, status: target, entityVersion: 2 } },
    }])
    const response = await handler(jsonRequest(
      `/organizations/${organization}/campaigns/${campaign}/issues/${issue}${suffix}`,
      "POST",
      {
        reason: "Explicit reason",
        baseEntityVersion: 1,
        clientOperationId: operation,
      },
    ))
    assert.equal(response.status, 200)
    assert.match(calls[0].sql, /silomath\.issue_transition_command/)
    assert.equal(calls[0].values[4], target)
    assert.equal(calls[0].values[6], context)
    assert.equal(calls[0].values[7], 1)
  }
})

Deno.test("NOTE Evidence metadata delegates without binary fields", async () => {
  const expected = { evidenceId: evidence, evidenceType: "NOTE", syncState: "LOCAL_PENDING" }
  const { handler, calls } = harness(() => [{ result: { status: 201, body: expected } }])
  const response = await handler(jsonRequest(
    `/organizations/${organization}/campaigns/${campaign}/evidence`,
    "POST",
    {
      evidenceId: evidence,
      stocktakingRecordId: record,
      issueId: null,
      stocktakingRevisionId: null,
      evidenceType: "NOTE",
      noteText: "Condition recorded",
      clientOperationId: operation,
    },
  ))
  assert.equal(response.status, 201)
  assert.deepEqual(await response.json(), expected)
  assert.match(calls[0].sql, /silomath\.evidence_create_command/)
  assert.equal(calls[0].values[7], "NOTE")
  assert.equal(calls[0].values[8], "Condition recorded")
})

Deno.test("PHOTO upload verifies bytes before private object transfer and finalization", async () => {
  const bytes = new Uint8Array([1, 2, 3, 4])
  const digest = await crypto.subtle.digest("SHA-256", Uint8Array.from(bytes).buffer)
  const hash = Array.from(new Uint8Array(digest))
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("")
  const { handler, calls, uploads } = harness((sql) => {
    if (sql.includes("select sha256")) return [{ sha256: hash, byte_size: bytes.length }]
    return [{ result: { status: 200, body: { evidenceId: evidence, syncState: "SYNCED" } } }]
  })
  const response = await handler(new Request(
    `https://example.test/functions/v1/api-v1/organizations/${organization}/campaigns/${campaign}/evidence/${evidence}/upload`,
    {
      method: "POST",
      headers: {
        Authorization: "Bearer test",
        "Content-Type": "image/jpeg",
        "Idempotency-Key": key,
        "X-Correlation-Id": "issues-evidence-test",
      },
      body: Uint8Array.from(bytes).buffer,
    },
  ))
  assert.equal(response.status, 200)
  assert.equal(uploads.length, 1)
  assert.equal(uploads[0].path, `${organization}/${campaign}/${evidence}/${hash}.jpg`)
  assert.match(calls[1].sql, /silomath\.evidence_finalize_upload_command/)
})

Deno.test("PHOTO hash mismatch never uploads or finalizes", async () => {
  const { handler, calls, uploads } = harness(() => [{
    sha256: "0".repeat(64),
    byte_size: 4,
  }])
  const response = await handler(new Request(
    `https://example.test/functions/v1/api-v1/organizations/${organization}/campaigns/${campaign}/evidence/${evidence}/upload`,
    {
      method: "POST",
      headers: {
        Authorization: "Bearer test",
        "Content-Type": "image/jpeg",
        "Idempotency-Key": key,
      },
      body: new Uint8Array([1, 2, 3, 4]).buffer,
    },
  ))
  assert.equal(response.status, 422)
  assert.equal((await response.json()).code, "EVIDENCE_HASH_MISMATCH")
  assert.equal(uploads.length, 0)
  assert.equal(calls.length, 1)
})

Deno.test("Evidence content is private and returned only after campaign authorization", async () => {
  const objectRef = `${organization}/${campaign}/${evidence}/${"a".repeat(64)}.jpg`
  const { handler, calls, downloads } = harness((sql) => {
    if (sql.includes("select exists")) return [{ allowed: true }]
    return [{ remote_object_ref: objectRef }]
  })
  const response = await handler(new Request(
    `https://example.test/functions/v1/api-v1/organizations/${organization}/campaigns/${campaign}/evidence/${evidence}/content`,
    { headers: { Authorization: "Bearer test" } },
  ))
  assert.equal(response.status, 200)
  assert.equal(response.headers.get("Content-Type"), "image/jpeg")
  assert.equal(response.headers.get("Cache-Control"), "private, no-store")
  assert.deepEqual(downloads, [objectRef])
  assert.equal(calls.length, 2)
})
