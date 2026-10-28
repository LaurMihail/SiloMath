package com.silomath.backend.routes

import com.silomath.backend.api.*
import com.silomath.backend.security.*
import com.silomath.backend.storageunit.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import java.time.Instant
import java.sql.SQLException
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

fun Application.configureStorageUnitRouting(verifier:AccessTokenVerifier,organizations:OrganizationAuthorizationResolver,service:StorageUnitService?) {
    routing {
        route("/api/v1/organizations/{organizationId}/locations/{locationId}/storage-units", io.ktor.http.HttpMethod.Get) {
            withActiveBearerAuthentication(verifier) { withOrganizationAuthorization(organizations) { requireAdminOrStandard {
                get {call.unitAction(service){s ->
                    val page=withContext(Dispatchers.IO){s.list(call.parameters["organizationId"]!!,call.parameters["locationId"]!!,
                        call.request.queryParameters["cursor"],call.request.queryParameters["search"],call.request.queryParameters["status"],
                        call.request.queryParameters["limit"]?.let{it.toIntOrNull() ?: invalid("limit")})}
                    call.respond(page)
                }}
            } } }
        }
        route("/api/v1/organizations/{organizationId}/storage-units/{storageUnitId}", io.ktor.http.HttpMethod.Get) {
            withActiveBearerAuthentication(verifier) { withOrganizationAuthorization(organizations) { requireAdminOrStandard {
                get {call.unitAction(service){s ->
                    val row=withContext(Dispatchers.IO){s.get(call.parameters["organizationId"]!!,call.parameters["storageUnitId"]!!)}
                        ?: throw StorageUnitFailure(ApiErrorCode.RESOURCE_NOT_FOUND,HttpStatusCode.NotFound)
                    call.respond(row)
                }}
            } } }
        }
        route("/api/v1/organizations/{organizationId}/locations/{locationId}/storage-units", io.ktor.http.HttpMethod.Post) {
            withActiveBearerAuthentication(verifier) { withOrganizationAuthorization(organizations) { requireAdmin {
                post {call.unitAction(service){s ->
                    val body=call.receive<JsonObject>();val key=call.checkedUnitKey(body)
                    if(body["baseEntityVersion"]?.let{it!=JsonNull}==true || call.request.headers[ENTITY_VERSION_HEADER]!=null)invalid("baseEntityVersion")
                    val row=withContext(Dispatchers.IO){s.mutate(call.parameters["organizationId"]!!,call.parameters["locationId"]!!,null,
                        call.requireAuthenticatedSubject().userId,call.correlationId(),key,body.requiredUnitString("clientOperationId"),null,body,
                        body.optionalUnitString("occurredAtUtc"))}
                    call.respond(HttpStatusCode.Created,row)
                }}
            } } }
        }
        route("/api/v1/organizations/{organizationId}/storage-units/{storageUnitId}", io.ktor.http.HttpMethod.Patch) {
            withActiveBearerAuthentication(verifier) { withOrganizationAuthorization(organizations) { requireAdmin {
                patch {call.unitAction(service){s ->
                    val body=call.receive<JsonObject>();val key=call.checkedUnitKey(body)
                    val base=call.baseEntityVersionOrNull() ?: invalid(ENTITY_VERSION_HEADER)
                    if(body.containsKey("baseEntityVersion") && body.optionalUnitVersion("baseEntityVersion")!=base)invalid("baseEntityVersion")
                    val row=withContext(Dispatchers.IO){s.mutate(call.parameters["organizationId"]!!,null,call.parameters["storageUnitId"]!!,
                        call.requireAuthenticatedSubject().userId,call.correlationId(),key,body.requiredUnitString("clientOperationId"),base,body,
                        body.optionalUnitString("occurredAtUtc"))}
                    call.respond(row)
                }}
            } } }
        }
        route("/api/v1/sync/batch") { withActiveBearerAuthentication(verifier) {
        post {call.unitAction(service){s ->
            val body=call.receive<JsonObject>()
            if((body.keys-setOf("deviceInstallationId","clientVersion","checkpointContext","operations")).isNotEmpty())invalid("request")
            val device=canonicalUuid(body.requiredUnitString("deviceInstallationId"),"deviceInstallationId")
            if(call.request.headers["X-Device-Installation-Id"]?.let{it!=device}==true)invalid("deviceInstallationId")
            if(body.requiredUnitString("clientVersion").isBlank())invalid("clientVersion")
            val operations=body["operations"] as? JsonArray ?: invalid("operations")
            if(operations.size>100)invalid("operations") // Server operational batch bound, never a business state.
            val ids=operations.map{it.jsonObject.requiredUnitString("syncOperationId")}
            if(ids.distinct().size!=ids.size)invalid("syncOperationId")
            val results=operations.map { item ->
                val op=item.jsonObject;val operationId=canonicalUuid(op.requiredUnitString("syncOperationId"),"syncOperationId")
                try {
                    if(op.requiredUnitString("entityType")!="STORAGE_UNIT")throw StorageUnitFailure(ApiErrorCode.SERVICE_UNAVAILABLE,HttpStatusCode.ServiceUnavailable)
                    val org=canonicalUuid(op.requiredUnitString("organizationId"),"organizationId")
                    val member=organizations.authorize(call.requireAuthenticatedSubject().userId,org)
                    if(member !is OrganizationAuthorizationResult.Authorized || member.membership.role!=OrganizationRole.ADMIN)
                        throw StorageUnitFailure(ApiErrorCode.AUTH_FORBIDDEN,HttpStatusCode.Forbidden)
                    if(op["campaignId"]?.let{it!=JsonNull}==true)invalid("campaignId") // This master-data owner has no Campaign mutation.
                    val entity=canonicalUuid(op.requiredUnitString("entityId"),"entityId")
                    val payload=op["payload"] as? JsonObject ?: invalid("payload")
                    val kind=op.requiredUnitString("operationType")
                    if(kind !in setOf("CREATE","PATCH"))invalid("operationType")
                    val base=op.optionalUnitVersion("baseEntityVersion")
                    val location=if(kind=="CREATE")canonicalUuid(payload.requiredUnitString("locationId"),"locationId") else null
                    val fields=if(kind=="CREATE")JsonObject(payload-"locationId") else payload
                    val dependencies=(op["dependencyIds"] as? JsonArray)?.map{canonicalUuid(it.jsonPrimitive.content,"dependencyIds")} ?: emptyList()
                    val result=withContext(Dispatchers.IO){s.mutate(org,location,entity,call.requireAuthenticatedSubject().userId,
                        call.correlationId(),op.requiredUnitString("idempotencyKey"),operationId,base,fields,
                        op.requiredUnitString("eventAtUtc"),device,dependencies)}
                    buildJsonObject {put("syncOperationId",operationId);put("outcome","ACKNOWLEDGED");put("entityVersion",result.entityVersion);put("error",JsonNull);put("conflict",JsonNull)}
                } catch(cancel:CancellationException){throw cancel}
                catch(e:StorageUnitFailure){
                    val conflict=e.code==ApiErrorCode.ENTITY_VERSION_CONFLICT
                    val retry=e.code in setOf(ApiErrorCode.SERVICE_UNAVAILABLE,ApiErrorCode.SYNC_DEPENDENCY_PENDING)
                    val current=if(conflict)withContext(Dispatchers.IO){s.get(op.requiredUnitString("organizationId"),op.requiredUnitString("entityId"))} else null
                    buildJsonObject {
                        put("syncOperationId",operationId);put("outcome",if(conflict)"CONFLICT" else if(retry)"RETRYABLE_ERROR" else "PERMANENT_ERROR")
                        put("entityVersion",current?.entityVersion ?: e.version)
                        put("error",buildJsonObject{put("code",e.code.name);put("message","Storage Unit operation was not accepted.");put("retryable",retry);put("correlationId",call.correlationId());put("fieldErrors",JsonArray(emptyList()))})
                        put("conflict",current?.let{Json.encodeToJsonElement(StorageUnitResponse.serializer(),it)} ?: JsonNull)
                    }
                }
            }
            call.respond(buildJsonObject{put("results",JsonArray(results));put("serverTimeUtc",Instant.now().toString());put("correlationId",call.correlationId())})
        }}
        } }
    }
}

private suspend fun ApplicationCall.unitAction(service:StorageUnitService?,block:suspend(StorageUnitService)->Unit) {
    try {
        if(service==null)throw StorageUnitFailure(ApiErrorCode.SERVICE_UNAVAILABLE,HttpStatusCode.ServiceUnavailable)
        block(service)
    } catch(e:CancellationException){throw e}
    catch(e:StorageUnitFailure){respondApiError(e.status,e.code,"Storage Unit request was not accepted.",
        retryable=e.code==ApiErrorCode.SERVICE_UNAVAILABLE,
        fieldErrors=e.field?.let{listOf(ApiFieldError(it,"INVALID","Invalid value."))} ?: emptyList(),
        details=e.version?.let{buildJsonObject{put("authoritativeEntityVersion",it)}})}
    catch(_:SQLException){respondApiError(HttpStatusCode.ServiceUnavailable,ApiErrorCode.SERVICE_UNAVAILABLE,"Storage Unit service unavailable.",retryable=true)}
    catch(_:IllegalArgumentException){respondApiError(HttpStatusCode.UnprocessableEntity,ApiErrorCode.VALIDATION_FAILED,"Invalid Storage Unit request.")}
}
private fun JsonObject.requiredUnitString(key:String):String {
    val p=this[key] as? JsonPrimitive ?: invalid(key)
    if(!p.isString)invalid(key)
    return p.content
}
private fun JsonObject.optionalUnitString(key:String):String?=if(this[key]==null || this[key]==JsonNull)null else requiredUnitString(key)
private fun ApplicationCall.checkedUnitKey(body:JsonObject):String {
    val key=canonicalUuid(idempotencyKeyOrNull() ?: invalid(IDEMPOTENCY_KEY_HEADER),IDEMPOTENCY_KEY_HEADER)
    if(body.containsKey("idempotencyKey") && body.optionalUnitString("idempotencyKey")!=key)invalid("idempotencyKey")
    return key
}

private fun JsonObject.optionalUnitVersion(key:String):Long? {
    val value=this[key] ?: return null
    if(value==JsonNull)return null
    val p=value as? JsonPrimitive ?: invalid(key)
    if(p.isString)invalid(key)
    return p.longOrNull ?: invalid(key)
}
