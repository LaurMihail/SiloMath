package com.silomath.core.data.network.source

import com.silomath.core.data.network.api.*
import kotlinx.serialization.json.*
import com.silomath.core.data.network.dto.*
import com.silomath.core.data.network.auth.AuthenticatedApiCallExecutor
import javax.inject.Inject

/** DTOs remain infrastructure-only; repository maps them at its boundary. */
interface StorageUnitRemoteSource {
    suspend fun list(org:String,location:String):List<com.silomath.core.model.storage.StorageUnitTransfer>
    suspend fun parent(org:String,id:String):LocationResponseDto
    suspend fun organization(org:String):OrganizationResponseDto
    suspend fun get(org:String,id:String):com.silomath.core.model.storage.StorageUnitTransfer
    suspend fun push(request:StorageUnitSyncBatchDto):StorageUnitSyncBatchResultDto
}
class StorageUnitTransportFailure(val code:String,val retryable:Boolean,val retryAfter:String?=null):java.io.IOException(code)

class RestStorageUnitRemoteSource @Inject constructor(private val api:StorageUnitApi,private val locations:LocationApi,
    private val organizations:OrganizationApi,private val calls:AuthenticatedApiCallExecutor):StorageUnitRemoteSource {
    override suspend fun list(org:String,location:String):List<com.silomath.core.model.storage.StorageUnitTransfer> {
        val rows=mutableListOf<com.silomath.core.model.storage.StorageUnitTransfer>();var cursor:String?=null;val seen=mutableSetOf<String>()
        do {
            val page=protectedCall{api.list(org,location,cursor)}
            rows+=page.items.map{it.transfer()}
            cursor=if(page.hasMore)checkNotNull(page.nextCursor?.takeIf{it.isNotBlank() && seen.add(it)}) else null
        }while(cursor!=null)
        return rows
    }
    override suspend fun parent(org:String,id:String)=protectedCall{locations.getLocation(org,id)}
    override suspend fun organization(org:String)=protectedCall{organizations.getOrganization(org)}
    override suspend fun get(org:String,id:String)=protectedCall{api.get(org,id)}.transfer()
    override suspend fun push(request:StorageUnitSyncBatchDto):StorageUnitSyncBatchResultDto = protectedCall{api.push(request.deviceInstallationId,request)}
    private suspend fun <T> protectedCall(block:suspend()->T):T = try {
        calls.execute(block)
    } catch(e:retrofit2.HttpException) {
        val body=runCatching{kotlinx.serialization.json.Json.parseToJsonElement(e.response()?.errorBody()?.string().orEmpty()).jsonObject}.getOrNull()
        val known=setOf("AUTH_SESSION_EXPIRED","AUTH_USER_DISABLED","AUTH_FORBIDDEN","TENANT_SCOPE_VIOLATION","VALIDATION_FAILED",
            "RESOURCE_NOT_FOUND","ENTITY_VERSION_CONFLICT","IDEMPOTENCY_KEY_REUSE_MISMATCH","SYNC_DEPENDENCY_PENDING","RATE_LIMITED","SERVICE_UNAVAILABLE","INTERNAL_ERROR")
        val code=body?.get("code")?.jsonPrimitive?.content?.takeIf{it in known}
            ?: when(e.code()){401->"AUTH_SESSION_EXPIRED";403->"AUTH_FORBIDDEN";429->"RATE_LIMITED";in 500..599->"SERVICE_UNAVAILABLE";else->"VALIDATION_FAILED"}
        val retry=e.code()==429 || e.code() in 500..599
        throw StorageUnitTransportFailure(code,retry,e.response()?.headers()?.get("Retry-After"))
    }
}

fun StorageUnitResponseDto.transfer()=com.silomath.core.model.storage.StorageUnitTransfer(id,organizationId,locationId,name,code,description,
    lifecycleStatus,createdAt,createdBy,updatedAt,updatedBy,entityVersion)
