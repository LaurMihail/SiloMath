import assert from "node:assert/strict"
import { createApiHandler } from "./index.ts"
const actor = "11111111-1111-4111-8111-111111111111"
const org = "22222222-2222-4222-8222-222222222222"
const key = "33333333-3333-4333-8333-333333333333"
type Dependencies = NonNullable<Parameters<typeof createApiHandler>[0]>
function harness(authenticated = true) {
  const calls: { sql: string; values: unknown[] }[] = []
  const database = ((strings: TemplateStringsArray, ...values: unknown[]) => {
    const sql = strings.join("?")
    calls.push({ sql, values })
    if (sql.includes("organization_patch_command")) {
      return Promise.resolve([{ result: { status: 200, body: { id: org, entityVersion: 2 } } }])
    }
    if (sql.includes("from silomath.users")) return Promise.resolve([{ active: true }])
    if (sql.includes("from silomath.notifications")) return Promise.resolve([])
    return Promise.resolve([{ body: { userId: actor, theme: "DARK" } }])
  }) as unknown as Dependencies["database"]
  return { calls, handler: createApiHandler({ database,
    authenticate: () => Promise.resolve(authenticated ? { id: actor } as never : null),
  }) }
}
function req(path: string, method = "GET", body?: unknown) {
  return new Request(`https://example.test/functions/v1/api-v1${path}`, {
    method, headers: { "Content-Type": "application/json", "Idempotency-Key": key },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
}
Deno.test("Organization PATCH preserves canonical mutation controls", async () => {
  const h = harness()
  const response = await h.handler(req(`/organizations/${org}`, "PATCH", {
    name: "Organization", country: "RO", clientOperationId: key, baseEntityVersion: 1,
  }))
  assert.equal(response.status, 200)
  assert.equal((await response.json()).entityVersion, 2)
  assert.match(h.calls[0].sql, /organization_patch_command/)
  assert.equal(h.calls[0].values[3], 1)
})
Deno.test("Organization PATCH rejects client supplied authority", async () => {
  const h = harness()
  const response = await h.handler(req(`/organizations/${org}`, "PATCH", {
    role: "ADMIN", clientOperationId: key, baseEntityVersion: 1,
  }))
  assert.equal(response.status, 422)
  assert.equal(h.calls.length, 0)
})
Deno.test("Preferences use authenticated actor and authoritative command", async () => {
  const h = harness()
  assert.equal((await h.handler(req("/preferences", "PATCH", { theme: "DARK" }))).status, 200)
  assert.match(h.calls[0].sql, /user_preferences_patch_command/)
  assert.equal(h.calls[0].values[0], actor)
})
Deno.test("Notifications empty page is canonical and user scoped", async () => {
  const h = harness()
  const response = await h.handler(req("/notifications"))
  assert.deepEqual(await response.json(), { items: [], nextCursor: null, hasMore: false })
  assert.match(h.calls[1].sql, /recipient_user_id/)
  assert.equal(h.calls[1].values[0], actor)
})
Deno.test("Notification mutation delegates recipient guard and stable read behavior", async () => {
  const h = harness()
  const response = await h.handler(req(`/notifications/${key}:mark-read`, "POST", { clientOperationId: key }))
  assert.equal(response.status, 200)
  assert.match(h.calls[0].sql, /notification_mark_read_command/)
  assert.deepEqual(h.calls[0].values.slice(0, 3), [actor, key, key])
  assert.match(String(h.calls[0].values[3]), /^[a-f0-9]{64}$/)
})
Deno.test("Settings and notification routes fail closed without authentication", async () => {
  for (const path of ["/preferences", "/notifications", `/organizations/${org}`]) {
    const h = harness(false)
    assert.equal((await h.handler(req(path))).status, 401)
    assert.equal(h.calls.length, 0)
  }
})
