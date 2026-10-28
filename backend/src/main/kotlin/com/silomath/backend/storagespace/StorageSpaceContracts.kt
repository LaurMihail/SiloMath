package com.silomath.backend.storagespace

import com.silomath.backend.api.ApiErrorCode
import io.ktor.http.HttpStatusCode
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class StorageSpaceResponse(val id:String,val organizationId:String,val locationId:String,val storageUnitId:String,
    val code:String,val description:String?=null,val geometryProfileId:String,val lifecycleStatus:String,
    val createdAt:String,val createdBy:String,val updatedAt:String,val updatedBy:String,val entityVersion:Long)
@Serializable data class StorageSpacePage(val items:List<StorageSpaceResponse>,val nextCursor:String?=null,val hasMore:Boolean)
@Serializable data class StorageSpaceBatchResponse(val items:List<StorageSpaceResponse>)
class StorageSpaceFailure(val code:ApiErrorCode,val status:HttpStatusCode,val field:String?=null,val version:Long?=null):RuntimeException(code.name)
fun invalidSpace(field:String):Nothing=throw StorageSpaceFailure(ApiErrorCode.VALIDATION_FAILED,HttpStatusCode.UnprocessableEntity,field)
fun spaceUuid(value:String,field:String):String {
    if(!value.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))invalidSpace(field)
    return try{UUID.fromString(value).toString()}catch(_:IllegalArgumentException){invalidSpace(field)}
}
object StorageSpaceFields {
    val mutable=setOf("code","description","geometryProfileId","lifecycleStatus")
    val controls=setOf("idempotencyKey","clientOperationId","occurredAtUtc","baseEntityVersion")
    fun parse(json:JsonObject,create:Boolean):JsonObject {
        if((json.keys-mutable-controls).isNotEmpty())invalidSpace("request")
        if(create && (!json.containsKey("code") || !json.containsKey("geometryProfileId")))invalidSpace("code/geometryProfileId")
        return buildJsonObject {
            for(key in mutable) {
                if(!json.containsKey(key)){if(create && key=="lifecycleStatus")put(key,"ACTIVE");if(create && key=="description")put(key,JsonNull);continue}
                val raw=json[key]
                if(raw==JsonNull){if(key!="description")invalidSpace(key);put(key,JsonNull);continue}
                val value=(raw as? JsonPrimitive)?.takeIf{it.isString}?.content ?: invalidSpace(key)
                val normalized=if(key=="code")value.trim().uppercase(Locale.ROOT) else value
                when(key) {
                    "code"->if(normalized.isEmpty())invalidSpace(key)
                    "geometryProfileId"->spaceUuid(normalized,key)
                    "lifecycleStatus"->if(normalized !in setOf("ACTIVE","INACTIVE"))invalidSpace(key)
                }
                put(key,normalized)
            }
        }
    }
}
interface StorageSpaceService {
    fun list(org:String,unit:String,cursor:String?,search:String?,status:String?,limit:Int?):StorageSpacePage
    fun get(org:String,id:String):StorageSpaceResponse?
    fun mutate(org:String,unit:String?,id:String?,actor:String,correlation:String,key:String,operationId:String,
        base:Long?,payload:JsonObject,eventAt:String?,device:String?=null,dependencies:List<String> = emptyList()):StorageSpaceResponse
    fun batchCreate(org:String,unit:String,actor:String,correlation:String,key:String,operationId:String,
        payloads:List<JsonObject>,eventAt:String?):StorageSpaceBatchResponse
}
