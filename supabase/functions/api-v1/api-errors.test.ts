import assert from "node:assert/strict"
import { createApiHandler } from "./index.ts"
const actor="11111111-1111-4111-8111-111111111111"
const org="22222222-2222-4222-8222-222222222222"
const campaign="33333333-3333-4333-8333-333333333333"
const key="44444444-4444-4444-8444-444444444444"
type Dependencies=NonNullable<Parameters<typeof createApiHandler>[0]>
function handler(message:string,code:string,detail?:string){
 return createApiHandler({
 authenticate:()=>Promise.resolve({id:actor} as never),
 configuration:()=>"FINAL_TEST_V1",
 database:(()=>{throw Object.assign(new Error(message),{code,detail})}) as unknown as Dependencies["database"],
 })
}
function req(){
 return new Request(`https://example.test/functions/v1/api-v1/organizations/${org}/campaigns/${campaign}:close`,{
 method:"POST",headers:{"Content-Type":"application/json","Idempotency-Key":key,"X-Correlation-Id":"error-test"},
 body:JSON.stringify({clientOperationId:key,baseEntityVersion:1})})
}
Deno.test("Close guard error is canonical 409 with sanitized structured guard details",async()=>{
 const response=await handler("CLOSE_GUARDS_FAILED","P0001",
 JSON.stringify({canClose:false,toDoCount:2,sql:"SECRET_SQL"}))(req())
 assert.equal(response.status,409)
 const body=await response.json()
 assert.equal(body.code,"CLOSE_GUARDS_FAILED")
 assert.deepEqual(body.details,{guards:{toDoCount:2,canClose:false}})
 assert.equal(body.correlationId,"error-test")
 assert.ok(!JSON.stringify(body).includes("SECRET_SQL"))
})
Deno.test("PostgreSQL authorization, validation and completed-state errors stay typed",async()=>{
 for(const [message,sqlstate,status,code] of [
 ["Admin required","42501",403,"AUTH_FORBIDDEN"],
 ["invalid input syntax internal SQL detail","22P02",422,"VALIDATION_FAILED"],
 ["completed Campaign is immutable","55000",409,"CAMPAIGN_COMPLETED_IMMUTABLE"],
 ] as const){
 const response=await handler(message,sqlstate)(req())
 assert.equal(response.status,status)
 const body=await response.json();assert.equal(body.code,code)
 assert.ok(!JSON.stringify(body).includes("internal SQL"))
 }
})
