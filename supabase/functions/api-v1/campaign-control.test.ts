import assert from "node:assert/strict"
import { createApiHandler } from "./index.ts"
const actor = "11111111-1111-4111-8111-111111111111"
const org = "22222222-2222-4222-8222-222222222222"
const campaign = "33333333-3333-4333-8333-333333333333"
const key = "44444444-4444-4444-8444-444444444444"
type Dependencies = NonNullable<Parameters<typeof createApiHandler>[0]>
function harness(allowed = true) {
 const calls: { sql: string; values: unknown[] }[] = []
 const database = ((strings: TemplateStringsArray, ...values: unknown[]) => {
  const sql = strings.join("?")
  calls.push({ sql, values })
  if (sql.includes("location_actor_is_active_admin")) return Promise.resolve([{ allowed }])
  if (sql.includes("from silomath.audit_events")) return Promise.resolve([])
  return Promise.resolve([{ body: { campaignId: campaign }, result: { status: 200, body: { campaignId: campaign } } }])
 }) as unknown as Dependencies["database"]
 return { calls, handler: createApiHandler({ database,
  authenticate: () => Promise.resolve({ id: actor } as never),
  configuration: (name) => name === "SILOMATH_FINAL_SNAPSHOT_SCHEMA_VERSION" ? "FINAL_TEST_V1" : undefined,
 }) }
}
function request(path: string, method = "GET", body?: unknown) {
 return new Request(`https://example.test/functions/v1/api-v1/organizations/${org}/${path}`, {
  method, headers: { "Content-Type": "application/json", "Idempotency-Key": key },
  ...(body === undefined ? {} : { body: JSON.stringify(body) }),
 })
}
Deno.test("Campaign Team add delegates current Team without rewriting launch snapshot", async () => {
 const h = harness()
 const response = await h.handler(request(`campaigns/${campaign}/team`, "POST", { userId: actor, clientOperationId: key }))
 assert.equal(response.status, 200)
 assert.match(h.calls[0].sql, /campaign_team_mutate_command/)
 assert.equal(h.calls[0].values[4], false)
})
Deno.test("Campaign Team remove requires base version and preserves target identity", async () => {
 const h = harness()
 const response = await h.handler(request(`campaigns/${campaign}/team/${actor}:remove`, "POST",
 { clientOperationId: key, baseEntityVersion: 2 }))
 assert.equal(response.status, 200)
 assert.deepEqual(h.calls[0].values.slice(0, 6), [actor, org, campaign, actor, true, 2])
})
Deno.test("Campaign snapshot delegates immutable canonical projection", async () => {
 const h = harness()
 assert.equal((await h.handler(request(`campaigns/${campaign}/snapshot`))).status, 200)
 assert.match(h.calls[0].sql, /campaign_snapshot_get/)
})
Deno.test("Campaign Close delegates version, idempotency and server schema", async () => {
 const h = harness()
 const response = await h.handler(request(`campaigns/${campaign}:close`, "POST", { clientOperationId: key, baseEntityVersion: 2 }))
 assert.equal(response.status, 200)
 assert.match(h.calls[0].sql, /campaign_close_command/)
 assert.equal(h.calls[0].values[3], 2)
 assert.equal(h.calls[0].values.at(-1), "FINAL_TEST_V1")
})
Deno.test("Campaign Close readiness and final snapshot remain authoritative reads", async () => {
 for (const [suffix, primitive] of [["close-readiness", "campaign_close_readiness"], ["final-snapshot", "final_inventory_snapshot_get"]]) {
  const h = harness()
  assert.equal((await h.handler(request(`campaigns/${campaign}/${suffix}`))).status, 200)
  assert.ok(h.calls[0].sql.includes(primitive))
 }
})
Deno.test("Audit Trail denies Standard and cross-tenant callers before rows are read", async () => {
 const h = harness(false)
 assert.equal((await h.handler(request("audit-events"))).status, 403)
 assert.equal(h.calls.length, 1)
})
Deno.test("Audit Trail returns canonical empty page for authorized Admin", async () => {
 const h = harness()
 const response = await h.handler(request("audit-events"))
 assert.equal(response.status, 200)
 assert.deepEqual(await response.json(), { items: [], nextCursor: null, hasMore: false })
 assert.match(h.calls[1].sql, /a.organization_id/)
})
