import assert from "node:assert/strict"
import { createApiHandler } from "./index.ts"
const actor="11111111-1111-4111-8111-111111111111"
const org="22222222-2222-4222-8222-222222222222"
const campaign="33333333-3333-4333-8333-333333333333"
type Dependencies=NonNullable<Parameters<typeof createApiHandler>[0]>
function harness(allowed=true) {
 const calls:string[]=[]
 const database=((strings:TemplateStringsArray)=>{
  const sql=strings.join("?");calls.push(sql)
  if(sql.includes("location_actor_is_active_member")) return Promise.resolve([{allowed}])
  if(sql.includes("organization_configurations")) return Promise.resolve([])
  if(sql.includes("report_export_record")) return Promise.resolve([])
  return Promise.resolve([{body:{organizationId:org,reportType:"PHYSICAL_INVENTORY",
   campaignId:campaign,sourceState:"FINAL",generatedAt:"2026-10-31T00:00:00Z",generatedBy:actor,
   filters:{},rows:[{storageSpaceCode:'Space, "A"',physicalQuantityKg:"12345678901234.123456"}]}}])
 }) as unknown as Dependencies["database"]
 return {calls,handler:createApiHandler({database,authenticate:()=>Promise.resolve({id:actor} as never)})}
}
function req(path:string,body?:unknown){
 return new Request(`https://example.test/functions/v1/api-v1/organizations/${org}/${path}`,{
 method:body===undefined?"GET":"POST",headers:{"Content-Type":"application/json"},
 ...(body===undefined?{}:{body:JSON.stringify(body)})})
}
Deno.test("CSV export derives all values from canonical dataset and audits success",async()=>{
 const h=harness();const response=await h.handler(req("reports/PHYSICAL_INVENTORY:export",{campaignId:campaign,format:"CSV"}))
 assert.equal(response.status,200);assert.match(response.headers.get("Content-Type")! ,/text\/csv/)
 const csv=await response.text()
 assert.ok(csv.includes('"Space, ""A"""'))
 assert.ok(csv.includes("12345678901234.123456"))
 assert.equal(h.calls.filter(c=>c.includes("report_dataset_get")).length,1)
 assert.equal(h.calls.filter(c=>c.includes("report_export_record")).length,1)
})
Deno.test("PDF unavailable renderer is explicit and emits no successful export audit",async()=>{
 const h=harness();const response=await h.handler(req("reports/PHYSICAL_INVENTORY:export",{campaignId:campaign,format:"PDF"}))
 assert.equal(response.status,503)
 assert.equal((await response.json()).code,"SERVICE_UNAVAILABLE")
 assert.ok(!h.calls.some(c=>c.includes("report_export_record")))
})
Deno.test("Dossier CSV is rejected by frozen format contract",async()=>{
 const h=harness()
 assert.equal((await h.handler(req("reports/DIGITAL_INVENTORY_DOSSIER:export",{campaignId:campaign,format:"CSV"}))).status,422)
 assert.equal(h.calls.length,0)
})
Deno.test("Configuration absence is valid and does not invent registry values",async()=>{
 const h=harness()
 const response=await h.handler(req("configuration"))
 assert.deepEqual(await response.json(),{organizationId:org,items:[]})
})
Deno.test("Configuration read denies wrong tenant before reading rows",async()=>{
 const h=harness(false)
 assert.equal((await h.handler(req("configuration"))).status,404)
 assert.equal(h.calls.length,1)
})
