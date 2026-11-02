import assert from "node:assert/strict"
import { createApiHandler } from "./index.ts"
const actor="11111111-1111-4111-8111-111111111111"
const org="22222222-2222-4222-8222-222222222222"
const id="33333333-3333-4333-8333-333333333333"
const key="44444444-4444-4444-8444-444444444444"
type Dependencies=NonNullable<Parameters<typeof createApiHandler>[0]>
function harness(admin=true,familyCode="F02") {
 const calls:{sql:string;values:unknown[]}[]=[]
 const database=((strings:TemplateStringsArray,...values:unknown[])=>{
  const sql=strings.join("?");calls.push({sql,values})
  if(sql.includes("location_actor_is_active_member")) return Promise.resolve([{allowed:true,admin}])
  if(sql.includes("select family_code")) return Promise.resolve([{family_code:familyCode}])
  if(sql.includes("geometry_profile_mutate_command")) return Promise.resolve([{result:{status:201,body:{id,entityVersion:1}}}])
  return Promise.resolve([{body:{rows:[],campaignId:id},geometry_profile_id:id,code:"PROFILE"}])
 }) as unknown as Dependencies["database"]
 return {calls,handler:createApiHandler({database,
 authenticate:()=>Promise.resolve({id:actor} as never),
 configuration:(name)=>name==="SILOMATH_GEOMETRY_SCHEMA_VERSION"?"GEOMETRY_TEST_V1":undefined})}
}
function request(path:string,method="GET",body?:unknown) {
 return new Request(`https://example.test/functions/v1/api-v1/organizations/${org}/${path}`,{
 method,headers:{"Content-Type":"application/json","Idempotency-Key":key},
 ...(body===undefined?{}:{body:JSON.stringify(body)})})
}
Deno.test("Geometry create calculates exact capacity server-side before PostgreSQL",async()=>{
 const h=harness()
 const response=await h.handler(request("geometry-profiles","POST",{
 geometryProfileId:id,clientOperationId:key,name:"Profile",code:"PROFILE",geometryFamilyId:key,
 dimensions:{diameterM:"2",heightM:"3"},measurementReferenceHeightM:"3",maximumFillHeightM:"3",geometryOrigin:"BASE"}))
 assert.equal(response.status,201)
 const call=h.calls.find(c=>c.sql.includes("geometry_profile_mutate_command"))!
 assert.equal(call.values[4],"9.424778")
 assert.equal(call.values[5],"GEOMETRY_TEST_V1")
})
Deno.test("F08 Geometry preserves opaque structured provenance while calculating capacity",async()=>{
 const h=harness(true,"F08")
 const areaProvenance={approvedReference:"Certified drawing A-101",verification:{method:"ENGINEERING_CALCULATION"}}
 const response=await h.handler(request("geometry-profiles","POST",{
 geometryProfileId:id,clientOperationId:key,name:"Verified area profile",code:"VERIFIED-AREA",geometryFamilyId:key,
 dimensions:{areaM2:"5",heightM:"3"},areaProvenance,hasConstantCrossSection:true,
 measurementReferenceHeightM:"3",maximumFillHeightM:"3",geometryOrigin:"BASE"}))
 assert.equal(response.status,201)
 const call=h.calls.find(c=>c.sql.includes("geometry_profile_mutate_command"))!
 assert.equal(call.values[4],"15.000000")
 const patch=JSON.parse(String(call.values[3]))
 assert.deepEqual(patch.areaProvenance,areaProvenance)
 assert.equal(patch.hasConstantCrossSection,true)
})

Deno.test("Geometry mutation denies Standard before calculation or writes",async()=>{
 const h=harness(false)
 assert.equal((await h.handler(request("geometry-profiles","POST",{}))).status,403)
 assert.equal(h.calls.length,1)
})
Deno.test("Geometry invalid capacity parameters are rejected before mutation",async()=>{
 const h=harness()
 assert.equal((await h.handler(request("geometry-profiles","POST",{
 geometryProfileId:id,clientOperationId:key,name:"Profile",code:"PROFILE",geometryFamilyId:key,
 dimensions:{diameterM:"-2",heightM:"3"},measurementReferenceHeightM:"3",maximumFillHeightM:"3",geometryOrigin:"BASE"}))).status,422)
 assert.ok(!h.calls.some(c=>c.sql.includes("geometry_profile_mutate_command")))
})
Deno.test("Geometry detail and version list use canonical immutable projections",async()=>{
 for(const path of [`geometry-profiles/${id}`,`geometry-profiles/${id}/versions`]){
 const h=harness();assert.equal((await h.handler(request(path))).status,200)
 assert.ok(h.calls.some(c=>c.sql.includes("silomath.geometry_profile")))
 }
})
Deno.test("Report catalog contains exactly nine frozen report types for Admin",async()=>{
 const h=harness()
 const response=await h.handler(request("reports"))
 assert.equal((await response.json()).items.length,9)
})
Deno.test("Report catalog excludes Admin-only Audit Trail for Standard",async()=>{
 const h=harness(false)
 const response=await h.handler(request("reports"))
 const data=await response.json()
 assert.equal(data.items.length,8)
 assert.ok(!data.items.some((x:{reportType:string})=>x.reportType==="AUDIT_TRAIL"))
})
Deno.test("All nine report dataset routes delegate to canonical source functions",async()=>{
 const types=["PHYSICAL_INVENTORY","INVENTORY_BY_LOCATION","INVENTORY_BY_COMMODITY","INVENTORY_BY_OWNERSHIP",
 "STORAGE_SPACE_WORKSHEET","CAPACITY_REPORT","INVENTORY_HISTORY","DIGITAL_INVENTORY_DOSSIER","AUDIT_TRAIL"]
 for(const type of types){
 const h=harness();const response=await h.handler(request(`reports/${type}:dataset`,"POST",{campaignId:id,campaignIds:[id,key]}))
 assert.equal(response.status,200)
 assert.ok(h.calls.length>0)
 }
})
Deno.test("History and Dossier routes delegate frozen snapshot semantics",async()=>{
 for(const [path,method,body] of [
 ["inventory-history:compare","POST",{campaignIds:[id,key]}],
 [`campaigns/${id}/dossier`,"GET",undefined],
 ] as const){
 const h=harness()
 assert.equal((await h.handler(request(path,method,body))).status,200)
 assert.ok(h.calls[0].sql.includes("silomath.inventory_"))
 }
})
