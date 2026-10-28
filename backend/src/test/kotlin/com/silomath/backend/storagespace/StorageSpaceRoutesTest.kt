package com.silomath.backend.storagespace

import com.silomath.backend.api.*
import com.silomath.backend.routes.configureStorageSpaceRouting
import com.silomath.backend.security.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.*
import kotlinx.serialization.json.*

class StorageSpaceRoutesTest {
    private fun id()=UUID.randomUUID().toString()
    private val org=id();private val unit=id();private val space=id();private val actor=id();private val profile=id()
    private val admin=AccessTokenVerifier{AccessTokenVerificationResult.Authenticated(AuthenticatedSubject(actor))}
    private fun resolver(role:OrganizationRole)=OrganizationAuthorizationResolver{_,requested->
        if(requested==org)OrganizationAuthorizationResult.Authorized(AuthorizedOrganizationMembership(id(),org,role)) else OrganizationAuthorizationResult.Forbidden}
    private fun row(version:Long=1)=StorageSpaceResponse(space,org,id(),unit,"S1",null,profile,"ACTIVE","2026-01-01T00:00:00Z",actor,"2026-01-01T00:00:00Z",actor,version)
    @Test fun standardReadsButCannotMutate()=testApplication {
        application{configureApiSerialization();configureCorrelationIds();configureStorageSpaceRouting(admin,resolver(OrganizationRole.STANDARD),FakeService(row()))}
        val path="/api/v1/organizations/$org/storage-units/$unit/storage-spaces"
        assertEquals(HttpStatusCode.OK,client.get(path){bearerAuth("token")}.status)
        assertEquals(HttpStatusCode.Forbidden,client.post(path){bearerAuth("token");contentType(ContentType.Application.Json);header("Idempotency-Key",id());setBody("{}")}.status)
    }
    @Test fun adminCreateAndPatchRequireCanonicalHeadersAndProjection()=testApplication {
        application{configureApiSerialization();configureCorrelationIds();configureStorageSpaceRouting(admin,resolver(OrganizationRole.ADMIN),FakeService(row()))}
        val key=id();val create=buildJsonObject{put("code","S1");put("geometryProfileId",profile);put("clientOperationId",id());put("idempotencyKey",key)}.toString()
        val response=client.post("/api/v1/organizations/$org/storage-units/$unit/storage-spaces"){bearerAuth("token");contentType(ContentType.Application.Json);header("Idempotency-Key",key);setBody(create)}
        assertEquals(HttpStatusCode.Created,response.status,response.bodyAsText());assertFalse(response.bodyAsText().contains("remoteId"))
        assertEquals(HttpStatusCode.UnprocessableEntity,client.patch("/api/v1/organizations/$org/storage-spaces/$space"){bearerAuth("token");contentType(ContentType.Application.Json);header("Idempotency-Key",id());setBody("{}")}.status)
    }
    @Test fun adminBatchCreateUsesOneLogicalRequestAndRejectsEmptyItems()=testApplication {
        val service=FakeService(row());application{configureApiSerialization();configureCorrelationIds();configureStorageSpaceRouting(admin,resolver(OrganizationRole.ADMIN),service)}
        val key=id();val operation=id();val item=buildJsonObject{put("code","C01");put("geometryProfileId",profile)}
        val body=buildJsonObject{put("items",JsonArray(listOf(item)));put("idempotencyKey",key);put("clientOperationId",operation)}.toString()
        val response=client.post("/api/v1/organizations/$org/storage-units/$unit/storage-spaces:batch-create"){bearerAuth("token");contentType(ContentType.Application.Json);header("Idempotency-Key",key);setBody(body)}
        assertEquals(HttpStatusCode.Created,response.status,response.bodyAsText());assertEquals(1,service.batchCalls)
        val emptyKey=id();val empty=buildJsonObject{put("items",JsonArray(emptyList()));put("idempotencyKey",emptyKey);put("clientOperationId",id())}.toString()
        assertEquals(HttpStatusCode.UnprocessableEntity,client.post("/api/v1/organizations/$org/storage-units/$unit/storage-spaces:batch-create"){bearerAuth("token");contentType(ContentType.Application.Json);header("Idempotency-Key",emptyKey);setBody(empty)}.status)
    }
    @Test fun crossTenantAndMissingBearerFailBeforeService()=testApplication {
        val service=FakeService(row());application{configureApiSerialization();configureCorrelationIds();configureStorageSpaceRouting(admin,resolver(OrganizationRole.ADMIN),service)}
        assertEquals(HttpStatusCode.Unauthorized,client.get("/api/v1/organizations/$org/storage-units/$unit/storage-spaces").status)
        val other=id()
        assertEquals(HttpStatusCode.Forbidden,client.get("/api/v1/organizations/$other/storage-units/$unit/storage-spaces"){bearerAuth("token")}.status)
        assertEquals(0,service.calls)
    }
    private class FakeService(private val response:StorageSpaceResponse):StorageSpaceService {
        var calls=0;var batchCalls=0
        override fun list(org:String,unit:String,cursor:String?,search:String?,status:String?,limit:Int?)=StorageSpacePage(listOf(response),null,false).also{calls++}
        override fun get(org:String,id:String)=response.also{calls++}
        override fun mutate(org:String,unit:String?,id:String?,actor:String,correlation:String,key:String,operationId:String,base:Long?,payload:JsonObject,eventAt:String?,device:String?,dependencies:List<String>)=response.also{calls++}
        override fun batchCreate(org:String,unit:String,actor:String,correlation:String,key:String,operationId:String,payloads:List<JsonObject>,eventAt:String?)=
            if(payloads.isEmpty())invalidSpace("items") else StorageSpaceBatchResponse(payloads.map{response}).also{batchCalls++}
    }
}
