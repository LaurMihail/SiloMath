package com.silomath.core.data.network.source

import com.silomath.core.data.network.api.*
import com.silomath.core.data.network.auth.AuthenticatedApiCallExecutor
import com.silomath.core.data.network.dto.*
import kotlinx.serialization.json.*
import javax.inject.Inject

interface StorageSpaceRemoteSource {
    suspend fun list(org:String,unit:String):List<StorageSpaceResponseDto>
    suspend fun get(org:String,id:String):StorageSpaceResponseDto
    suspend fun push(request:StorageUnitSyncBatchDto):StorageUnitSyncBatchResultDto
}

class RestStorageSpaceRemoteSource @Inject constructor(private val api:StorageSpaceApi,private val calls:AuthenticatedApiCallExecutor):StorageSpaceRemoteSource {
    override suspend fun list(org:String,unit:String):List<StorageSpaceResponseDto> {
        val rows=mutableListOf<StorageSpaceResponseDto>();var cursor:String?=null;val seen=mutableSetOf<String>()
        do {
            val page=protectedCall{api.list(org,unit,cursor)}
            rows+=page.items
            cursor=if(page.hasMore)checkNotNull(page.nextCursor?.takeIf{it.isNotBlank() && seen.add(it)}) else null
        }while(cursor!=null)
        return rows
    }
    override suspend fun get(org:String,id:String)=protectedCall{api.get(org,id)}
    override suspend fun push(request:StorageUnitSyncBatchDto)=protectedCall{api.push(request.deviceInstallationId,request)}
    private suspend fun <T> protectedCall(block:suspend()->T):T=try{calls.execute(block)}catch(e:retrofit2.HttpException){
        val body=runCatching{Json.parseToJsonElement(e.response()?.errorBody()?.string().orEmpty()).jsonObject}.getOrNull()
        val known=setOf("AUTH_SESSION_EXPIRED","AUTH_USER_DISABLED","AUTH_FORBIDDEN","TENANT_SCOPE_VIOLATION","VALIDATION_FAILED",
            "RESOURCE_NOT_FOUND","ENTITY_VERSION_CONFLICT","IDEMPOTENCY_KEY_REUSE_MISMATCH","SYNC_DEPENDENCY_PENDING","RATE_LIMITED","SERVICE_UNAVAILABLE","INTERNAL_ERROR")
        val code=body?.get("code")?.jsonPrimitive?.content?.takeIf{it in known}
            ?: when(e.code()){401->"AUTH_SESSION_EXPIRED";403->"AUTH_FORBIDDEN";429->"RATE_LIMITED";in 500..599->"SERVICE_UNAVAILABLE";else->"VALIDATION_FAILED"}
        throw StorageSpaceTransportFailure(code,e.code()==429 || e.code() in 500..599,e.response()?.headers()?.get("Retry-After"))
    }
}
class StorageSpaceTransportFailure(val code:String,val retryable:Boolean,val retryAfter:String?=null):java.io.IOException(code)
