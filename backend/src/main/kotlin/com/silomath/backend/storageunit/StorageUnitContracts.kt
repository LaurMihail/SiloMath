package com.silomath.backend.storageunit

import com.silomath.backend.api.ApiErrorCode
import io.ktor.http.HttpStatusCode
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class StorageUnitResponse(val id:String,val organizationId:String,val locationId:String,val name:String,val code:String,
    val description:String?=null,val lifecycleStatus:String,val createdAt:String,val createdBy:String,val updatedAt:String,val updatedBy:String,val entityVersion:Long)
@Serializable data class StorageUnitPage(val items:List<StorageUnitResponse>,val nextCursor:String?=null,val hasMore:Boolean)
class StorageUnitFailure(val code:ApiErrorCode,val status:HttpStatusCode,val field:String?=null,val version:Long?=null):RuntimeException(code.name)
fun invalid(field:String):Nothing=throw StorageUnitFailure(ApiErrorCode.VALIDATION_FAILED,HttpStatusCode.UnprocessableEntity,field)
fun canonicalUuid(value:String,field:String):String {
    if(!value.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))invalid(field)
    return try{UUID.fromString(value).toString()}catch(_:IllegalArgumentException){invalid(field)}
}
object StorageUnitFields {
    val mutable=setOf("name","code","description","lifecycleStatus")
    val controls=setOf("idempotencyKey","clientOperationId","occurredAtUtc","baseEntityVersion")
    fun parse(json:JsonObject,create:Boolean):JsonObject {
        if((json.keys-mutable-controls).isNotEmpty())invalid("request")
        if(create && (!json.containsKey("name") || !json.containsKey("code")))invalid("name/code")
        return buildJsonObject {
            for(key in mutable) {
                if(!json.containsKey(key)){if(create && key=="lifecycleStatus")put(key,"ACTIVE");if(create && key=="description")put(key,JsonNull);continue}
                val raw=json[key]
                if(raw==JsonNull){if(key!="description")invalid(key);put(key,JsonNull);continue}
                val p=raw as? JsonPrimitive ?: invalid(key)
                if(!p.isString)invalid(key)
                val value=p.content
                val normalized=when(key){"name"->value.trim();"code"->value.trim().uppercase(Locale.ROOT);else->value}
                when(key){
                    "name"->if(normalized.isEmpty() || normalized.length>150)invalid(key)
                    "code"->if(normalized.isEmpty() || normalized.length>50)invalid(key)
                    "description"->if(normalized.length>500)invalid(key)
                    "lifecycleStatus"->if(normalized !in setOf("ACTIVE","INACTIVE"))invalid(key)
                }
                put(key,normalized)
            }
        }
    }
}
interface StorageUnitService {
    fun list(org:String,location:String,cursor:String?,search:String?,status:String?,limit:Int?):StorageUnitPage
    fun get(org:String,id:String):StorageUnitResponse?
    fun mutate(org:String,location:String?,id:String?,actor:String,correlation:String,key:String,operationId:String,
        base:Long?,payload:JsonObject,eventAt:String?,device:String?=null,dependencies:List<String> = emptyList()):StorageUnitResponse
}
