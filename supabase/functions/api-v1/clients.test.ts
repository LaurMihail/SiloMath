import assert from "node:assert/strict"
import { createApiHandler } from "./index.ts"
const actor = "11111111-1111-4111-8111-111111111111"
const org = "22222222-2222-4222-8222-222222222222"
const id = "33333333-3333-4333-8333-333333333333"
const key = "44444444-4444-4444-8444-444444444444"
type Dependencies = NonNullable<Parameters<typeof createApiHandler>[0]>
function harness(allowed = true, authenticated = true) {
  const calls: { sql: string; values: unknown[] }[] = []
  const dto = { id, organizationId: org, name: "Client", code: "CLIENT", entityVersion: 2 }
  const database = ((strings: TemplateStringsArray, ...values: unknown[]) => {
    const sql = strings.join("?")
    calls.push({ sql, values })
    if (sql.includes("location_actor_is_active_member")) return Promise.resolve([{ allowed }])
    if (sql.includes("client_mutate_command")) return Promise.resolve([{ result: { status: 200, body: dto } }])
    return Promise.resolve([{ client_id: id, code: "CLIENT", body: dto }])
  }) as unknown as Dependencies["database"]
  return { calls, dto, handler: createApiHandler({
    database, authenticate: () => Promise.resolve(authenticated ? { id: actor } as never : null),
  }) }
}
function request(method: string, suffix = "", body?: unknown, mutationKey = key) {
  return new Request(`https://example.test/functions/v1/api-v1/organizations/${org}/clients${suffix}`, {
    method, headers: { "Content-Type": "application/json", "Idempotency-Key": mutationKey },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
}
Deno.test("Client collection returns canonical authorized page", async () => {
  const h = harness()
  const response = await h.handler(request("GET"))
  assert.equal(response.status, 200)
  assert.deepEqual((await response.json()).items, [h.dto])
  assert.match(h.calls[0].sql, /location_actor_is_active_member/)
})
Deno.test("Client detail denies wrong organization membership", async () => {
  const h = harness(false)
  assert.equal((await h.handler(request("GET", `/${id}`))).status, 404)
  assert.equal(h.calls.length, 1)
})
Deno.test("Client authentication fails before database access", async () => {
  const h = harness(true, false)
  assert.equal((await h.handler(request("GET"))).status, 401)
  assert.equal(h.calls.length, 0)
})
Deno.test("Client create delegates canonical identity, fingerprint and fields", async () => {
  const h = harness()
  const response = await h.handler(request("POST", "", {
    clientId: id, clientOperationId: key, name: "Client", code: "CLIENT",
  }))
  assert.equal(response.status, 200)
  assert.deepEqual(await response.json(), h.dto)
  assert.match(h.calls[0].sql, /client_mutate_command/)
  assert.deepEqual(h.calls[0].values.slice(0, 3), [actor, org, id])
  assert.equal(h.calls[0].values.at(-1), true)
})
Deno.test("Client patch preserves optimistic entityVersion", async () => {
  const h = harness()
  const response = await h.handler(request("PATCH", `/${id}`, {
    clientOperationId: key, baseEntityVersion: 1, lifecycleStatus: "INACTIVE",
  }))
  assert.equal(response.status, 200)
  assert.equal(h.calls[0].values[4], 1)
  assert.equal(h.calls[0].values.at(-1), false)
})
Deno.test("Client invalid mutation controls and unknown authority fields are rejected", async () => {
  for (const body of [
    { name: "Client", code: "CLIENT" },
    { clientId: id, clientOperationId: key, role: "ADMIN" },
  ]) {
    const h = harness()
    assert.equal((await h.handler(request("POST", "", body))).status, 422)
    assert.equal(h.calls.length, 0)
  }
})
