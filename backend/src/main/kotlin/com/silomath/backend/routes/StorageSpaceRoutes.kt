package com.silomath.backend.routes

import com.silomath.backend.api.*
import com.silomath.backend.security.*
import com.silomath.backend.storagespace.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.sql.SQLException
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

fun Application.configureStorageSpaceRouting(verifier:AccessTokenVerifier,organizations:OrganizationAuthorizationResolver,service:StorageSpaceService?) {
    routing {
        route("/api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces",HttpMethod.Get) {
            withActiveBearerAuthentication(verifier){withOrganizationAuthorization(organizations){requireAdminOrStandard {
                get{call.spaceAction(service){s->call.respond(withContext(Dispatchers.IO){s.list(call.parameters["organizationId"]!!,call.parameters["storageUnitId"]!!,
                    call.request.queryParameters["cursor"],call.request.queryParameters["search"],call.request.queryParameters["status"],
                    call.request.queryParameters["limit"]?.let{it.toIntOrNull() ?: invalidSpace("limit")})})}}
            }}}
        }
        route("/api/v1/organizations/{organizationId}/storage-spaces/{storageSpaceId}",HttpMethod.Get) {
            withActiveBearerAuthentication(verifier){withOrganizationAuthorization(organizations){requireAdminOrStandard {
                get{call.spaceAction(service){s->call.respond(withContext(Dispatchers.IO){s.get(call.parameters["organizationId"]!!,call.parameters["storageSpaceId"]!!)}
                    ?: throw StorageSpaceFailure(ApiErrorCode.RESOURCE_NOT_FOUND,HttpStatusCode.NotFound))}}
            }}}
        }
        route("/api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces",HttpMethod.Post) {
            withActiveBearerAuthentication(verifier){withOrganizationAuthorization(organizations){requireAdmin {
                post{call.spaceAction(service){s->
                    val body=call.receive<JsonObject>();val key=call.checkedSpaceKey(body)
                    if(body["baseEntityVersion"]?.let{it!=JsonNull}==true || call.request.headers[ENTITY_VERSION_HEADER]!=null)invalidSpace("baseEntityVersion")
                    val result=withContext(Dispatchers.IO){s.mutate(call.parameters["organizationId"]!!,call.parameters["storageUnitId"]!!,null,
                        call.requireAuthenticatedSubject().userId,call.correlationId(),key,body.requiredSpaceString("clientOperationId"),null,body,
                        body.optionalSpaceString("occurredAtUtc"))}
                    call.respond(HttpStatusCode.Created,result)
                }}
            }}}
        }
        route("/api/v1/organizations/{organizationId}/storage-units/{storageUnitId}/storage-spaces:batch-create",HttpMethod.Post) {
            withActiveBearerAuthentication(verifier){withOrganizationAuthorization(organizations){requireAdmin {
                post{call.spaceAction(service){s->
                    val body=call.receive<JsonObject>();val key=call.checkedSpaceKey(body)
                    if((body.keys-setOf("items","idempotencyKey","clientOperationId","occurredAtUtc")).isNotEmpty())invalidSpace("request")
                    val items=(body["items"] as? JsonArray)?.map{it as? JsonObject ?: invalidSpace("items")} ?: invalidSpace("items")
                    val result=withContext(Dispatchers.IO){s.batchCreate(call.parameters["organizationId"]!!,call.parameters["storageUnitId"]!!,
                        call.requireAuthenticatedSubject().userId,call.correlationId(),key,body.requiredSpaceString("clientOperationId"),items,
                        body.optionalSpaceString("occurredAtUtc"))}
                    call.respond(HttpStatusCode.Created,result)
                }}
            }}}
        }
        route("/api/v1/organizations/{organizationId}/storage-spaces/{storageSpaceId}",HttpMethod.Patch) {
            withActiveBearerAuthentication(verifier){withOrganizationAuthorization(organizations){requireAdmin {
                patch{call.spaceAction(service){s->
                    val body=call.receive<JsonObject>();val key=call.checkedSpaceKey(body);val base=call.baseEntityVersionOrNull() ?: invalidSpace(ENTITY_VERSION_HEADER)
                    if(body.containsKey("baseEntityVersion") && body.optionalSpaceVersion("baseEntityVersion")!=base)invalidSpace("baseEntityVersion")
                    call.respond(withContext(Dispatchers.IO){s.mutate(call.parameters["organizationId"]!!,null,call.parameters["storageSpaceId"]!!,
                        call.requireAuthenticatedSubject().userId,call.correlationId(),key,body.requiredSpaceString("clientOperationId"),base,body,
                        body.optionalSpaceString("occurredAtUtc"))})
                }}
            }}}
        }
    }
}
private suspend fun ApplicationCall.spaceAction(service:StorageSpaceService?,block:suspend(StorageSpaceService)->Unit) {
    try{if(service==null)throw StorageSpaceFailure(ApiErrorCode.SERVICE_UNAVAILABLE,HttpStatusCode.ServiceUnavailable);block(service)}
    catch(e:CancellationException){throw e}
    catch(e:StorageSpaceFailure){respondApiError(e.status,e.code,"Storage Space request was not accepted.",retryable=e.code==ApiErrorCode.SERVICE_UNAVAILABLE,
        fieldErrors=e.field?.let{listOf(ApiFieldError(it,"INVALID","Invalid value."))} ?: emptyList(),
        details=e.version?.let{buildJsonObject{put("authoritativeEntityVersion",it)}})}
    catch(_:SQLException){respondApiError(HttpStatusCode.ServiceUnavailable,ApiErrorCode.SERVICE_UNAVAILABLE,"Storage Space service unavailable.",retryable=true)}
    catch(_:IllegalArgumentException){respondApiError(HttpStatusCode.UnprocessableEntity,ApiErrorCode.VALIDATION_FAILED,"Invalid Storage Space request.")}
}
private fun JsonObject.requiredSpaceString(key:String):String=(this[key] as? JsonPrimitive)?.takeIf{it.isString}?.content ?: invalidSpace(key)
private fun JsonObject.optionalSpaceString(key:String):String?=if(this[key]==null || this[key]==JsonNull)null else requiredSpaceString(key)
private fun JsonObject.optionalSpaceVersion(key:String):Long? {
    val value=this[key] ?: return null;if(value==JsonNull)return null
    return (value as? JsonPrimitive)?.takeIf{!it.isString}?.longOrNull ?: invalidSpace(key)
}
private fun ApplicationCall.checkedSpaceKey(body:JsonObject):String {
    val key=spaceUuid(idempotencyKeyOrNull() ?: invalidSpace(IDEMPOTENCY_KEY_HEADER),IDEMPOTENCY_KEY_HEADER)
    if(body.containsKey("idempotencyKey") && body.optionalSpaceString("idempotencyKey")!=key)invalidSpace("idempotencyKey")
    return key
}
