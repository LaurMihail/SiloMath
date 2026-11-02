import assert from "node:assert/strict"
import { createApiHandler } from "./index.ts"

const actor = "11111111-1111-4111-8111-111111111111"
const organization = "22222222-2222-4222-8222-222222222222"
const key = "33333333-3333-4333-8333-333333333333"
const body = {
  name: "Annual inventory", inventoryType: "ANNUAL",
  startDate: "2026-10-31", plannedEndDate: "2026-11-01",
  scopeStorageSpaceIds: ["44444444-4444-4444-8444-444444444444"],
  team: [{ userId: actor }],
}
const responseBody = {
  id: key,
  organizationId: organization,
  campaignCode: "INV-2026-0001",
  name: body.name,
  inventoryType: body.inventoryType,
  startDate: body.startDate,
  plannedEndDate: body.plannedEndDate,
  status: "ACTIVE",
  launchedAt: "2026-10-31T05:00:00.000Z",
  launchedBy: actor,
  completedAt: null,
  completedBy: null,
  campaignSnapshotId: "55555555-5555-4555-8555-555555555555",
  createdAt: "2026-10-31T05:00:00.000Z",
  createdBy: actor,
  updatedAt: "2026-10-31T05:00:00.000Z",
  updatedBy: actor,
  entityVersion: 2,
}
const config = (name: string) => ({
  SILOMATH_CAMPAIGN_SNAPSHOT_SCHEMA_VERSION: "CAMPAIGN_SNAPSHOT_V1",
  SILOMATH_CALCULATION_ENGINE_VERSION: "2.0",
} as Record<string, string>)[name]
function request(payload: unknown = body, headers: Record<string, string> = {}) {
  return new Request(`https://example.test/functions/v1/api-v1/organizations/${organization}/campaigns:launch`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Idempotency-Key": key, "X-Correlation-Id": "launch-test", ...headers },
    body: JSON.stringify(payload),
  })
}
type Dependencies = NonNullable<Parameters<typeof createApiHandler>[0]>
function harness(options: { error?: string; authenticated?: boolean; configured?: boolean } = {}) {
  const calls: { sql: string; values: unknown[] }[] = []
  const database = ((strings: TemplateStringsArray, ...values: unknown[]) => {
    calls.push({ sql: strings.join("?"), values })
    if (options.error) throw new Error(options.error)
    return Promise.resolve([{ result: { status: 201, body: responseBody } }])
  }) as unknown as Dependencies["database"]
  const handler = createApiHandler({
    database,
    authenticate: () => Promise.resolve(options.authenticated === false ? null : ({ id: actor } as never)),
    configuration: options.configured === false ? () => undefined : config,
  })
  return { handler, calls }
}
Deno.test("Campaign launch delegates exact canonical arguments and propagates authoritative response", async () => {
  const { handler, calls } = harness()
  const result = await handler(request())
  assert.equal(result.status, 201)
  assert.deepEqual(await result.json(), responseBody)
  assert.equal(result.headers.get("X-Correlation-Id"), "launch-test")
  assert.equal(calls.length, 1)
  assert.match(calls[0].sql, /silomath\.campaign_save_launch_command/)
  const values = calls[0].values
  assert.deepEqual(values.slice(0, 3), [actor, organization, key])
  assert.match(String(values[3]), /^[a-f0-9]{64}$/)
  assert.deepEqual(values.slice(4), [
    "launch-test", body.name, body.inventoryType, body.startDate, body.plannedEndDate,
    body.scopeStorageSpaceIds, [actor], "CAMPAIGN_SNAPSHOT_V1", "2.0",
  ])
})
Deno.test("Campaign launch rejects missing or invalid authentication before PostgreSQL", async () => {
  const { handler, calls } = harness({ authenticated: false })
  for (const headers of [{}, { Authorization: "Bearer invalid" }] as Record<string, string>[]) {
    const result = await handler(request(body, headers))
    assert.equal(result.status, 401)
    assert.equal((await result.json()).code, "AUTH_SESSION_EXPIRED")
  }
  assert.equal(calls.length, 0)
})
Deno.test("Campaign launch requires matching UUID idempotency keys", async () => {
  const { handler, calls } = harness()
  for (const [payload, header] of [[body, ""], [body, "invalid"], [{ ...body, idempotencyKey: actor }, key]] as const) {
    const result = await handler(request(payload, { "Idempotency-Key": header }))
    assert.equal(result.status, 422)
    assert.equal((await result.json()).code, "VALIDATION_FAILED")
  }
  assert.equal(calls.length, 0)
})
Deno.test("Campaign launch rejects invalid fields, dates, scope and team", async () => {
  const { handler, calls } = harness()
  for (const payload of [
    {}, { ...body, name: " " }, { ...body, inventoryType: "DRAFT" },
    { ...body, startDate: "2026-02-30" }, { ...body, plannedEndDate: "2026-01-01" },
    { ...body, scopeStorageSpaceIds: [] }, { ...body, scopeStorageSpaceIds: ["invalid"] },
    { ...body, team: [] }, { ...body, team: [{ userId: "invalid" }] },
    { ...body, team: [null] }, { ...body, scopeStorageSpaceIds: [key, key] },
    { ...body, team: [{ userId: actor }, { userId: actor }] },
  ]) assert.equal((await handler(request(payload))).status, 422)
  assert.equal(calls.length, 0)
})
Deno.test("Campaign launch rejects conflicting organization context", async () => {
  const { handler, calls } = harness()
  assert.equal((await handler(request(body, { "X-Organization-Id": actor }))).status, 403)
  assert.equal(calls.length, 0)
})
Deno.test("Campaign launch fails closed without server compatibility configuration", async () => {
  const { handler, calls } = harness({ configured: false })
  const result = await handler(request())
  assert.equal(result.status, 503)
  assert.equal((await result.json()).code, "SERVICE_UNAVAILABLE")
  assert.equal(calls.length, 0)
})
Deno.test("Campaign launch uses existing PostgreSQL error mapping", async () => {
  for (const [error, status, code] of [
    ["campaign launch forbidden", 403, "AUTH_FORBIDDEN"],
    ["invalid inventory type", 422, "VALIDATION_FAILED"],
    ["idempotency key payload conflict", 409, "IDEMPOTENCY_KEY_REUSE_MISMATCH"],
    ["unexpected database failure", 500, "INTERNAL_ERROR"],
  ] as const) {
    const { handler } = harness({ error })
    const result = await handler(request())
    assert.equal(result.status, status)
    const payload = await result.json()
    assert.equal(payload.code, code)
    assert.equal(payload.correlationId, "launch-test")
    if (status === 500) assert.ok(!payload.message.includes(error))
  }
})
Deno.test("Campaign launch retry preserves fingerprint and propagates replay result", async () => {
  const { handler, calls } = harness()
  const first = await handler(request())
  const replay = await handler(request())
  assert.deepEqual(await first.json(), await replay.json())
  assert.deepEqual(calls[0].values, calls[1].values)
})
