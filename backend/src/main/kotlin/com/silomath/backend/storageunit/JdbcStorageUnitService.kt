package com.silomath.backend.storageunit

import com.silomath.backend.api.*
import io.ktor.http.HttpStatusCode
import java.sql.*
import java.time.Instant
import java.util.UUID
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class JdbcStorageUnitService(private val connections:()->Connection):StorageUnitService {
    constructor(url:String,user:String?,password:String?):this({DriverManager.getConnection(url,java.util.Properties().apply{
        user?.let{setProperty("user",it)};password?.let{setProperty("password",it)}
    })})
    private val json=Json{encodeDefaults=true;explicitNulls=true}
    private val cursors=CursorCodec()
    private val columns="storage_unit_id,organization_id,location_id,name,code,description,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version"
    private fun Connection.query(sql:String,args:List<Any?>):List<StorageUnitResponse> = prepareStatement(sql).use {s ->
        args.forEachIndexed{i,v->s.setObject(i+1,v)};s.executeQuery().use{r->buildList{while(r.next())add(r.record())}}
    }
    override fun list(org:String,location:String,cursor:String?,search:String?,status:String?,limit:Int?):StorageUnitPage {
        canonicalUuid(org,"organizationId");canonicalUuid(location,"locationId")
        if(status!=null && status !in setOf("ACTIVE","INACTIVE"))invalid("status")
        val after=cursor?.let{cursors.decode(it) ?: invalid("cursor")}
        after?.second?.let{canonicalUuid(it,"cursor")}
        val size=PageLimitPolicy(50,200).normalize(limit)
        return connections().use {c ->
            if(!c.exists("select 1 from silomath.locations where location_id=?::uuid and organization_id=?::uuid",location,org))notFound()
            val args=mutableListOf<Any?>(org,location)
            val where=buildString {
                append("organization_id=?::uuid and location_id=?::uuid")
                if(status!=null){append(" and lifecycle_status=?");args+=status}
                if(!search.isNullOrBlank()){append(" and (strpos(lower(name),lower(?))>0 or strpos(lower(code),lower(?))>0)");args+=search.trim();args+=search.trim()}
                if(after!=null){append(" and (code,storage_unit_id)>(?,?::uuid)");args+=after.first;args+=after.second}
            }
            args+=size+1
            val rows=c.query("select $columns from silomath.storage_units where $where order by code,storage_unit_id limit ?",args)
            val page=rows.take(size);val more=rows.size>size
            StorageUnitPage(page,if(more)cursors.encode(page.last().code,page.last().id) else null,more)
        }
    }
    override fun get(org:String,id:String):StorageUnitResponse? {
        canonicalUuid(org,"organizationId");canonicalUuid(id,"storageUnitId")
        return connections().use{it.query("select $columns from silomath.storage_units where organization_id=?::uuid and storage_unit_id=?::uuid",listOf(org,id)).singleOrNull()}
    }
    override fun mutate(org:String,location:String?,id:String?,actor:String,correlation:String,key:String,operationId:String,
        base:Long?,payload:JsonObject,eventAt:String?,device:String?,dependencies:List<String>):StorageUnitResponse {
        canonicalUuid(org,"organizationId");canonicalUuid(actor,"actor");canonicalUuid(key,"idempotencyKey");canonicalUuid(operationId,"clientOperationId")
        id?.let{canonicalUuid(it,"storageUnitId")};location?.let{canonicalUuid(it,"locationId")}
        val create=location!=null
        if(!create && (id==null || base==null || base<1))invalid("baseEntityVersion")
        if(create && base!=null)invalid("baseEntityVersion")
        val fields=StorageUnitFields.parse(payload,create)
        val event=eventAt?.let{try{Instant.parse(it)}catch(_:Exception){invalid("occurredAtUtc")}}
        val fingerprint=hash(buildJsonObject {
            put("organizationId",org);put("locationId",location);put("id",id);put("actor",actor);put("operationId",operationId)
            put("base",base);put("event",event?.toString());put("device",device);put("dependencies",JsonArray(dependencies.distinct().sorted().map(::JsonPrimitive)));put("fields",JsonObject(fields.toSortedMap()))
        }.toString())
        return connections().use {c ->
            c.autoCommit=false
            try {
                c.prepareStatement("select pg_advisory_xact_lock(hashtextextended(?,0))").use{it.setString(1,key);it.execute()}
                if(!c.exists("select 1 from silomath.organization_memberships m join silomath.users u on u.user_id=m.user_id join silomath.organizations o on o.organization_id=m.organization_id where o.lifecycle_status='ACTIVE' and m.organization_id=?::uuid and m.user_id=?::uuid and m.role='ADMIN' and m.lifecycle_status='ACTIVE' and u.lifecycle_status='ACTIVE' for share of m,u,o",org,actor))
                    throw StorageUnitFailure(ApiErrorCode.AUTH_FORBIDDEN,HttpStatusCode.Forbidden)
                if(device!=null) {
                    canonicalUuid(device,"deviceInstallationId")
                    if(!c.exists("select 1 from silomath.device_installations where device_installation_id=?::uuid and user_id=?::uuid and revoked_at is null",device,actor))throw StorageUnitFailure(ApiErrorCode.AUTH_FORBIDDEN,HttpStatusCode.Forbidden)
                }
                val replay=c.prepareStatement("select payload::text from silomath.sync_operations where idempotency_key=?").use{s->
                    s.setString(1,key);s.executeQuery().use{r->if(r.next())json.parseToJsonElement(r.getString(1)).jsonObject else null}
                }
                if(replay!=null) {
                    if(replay["requestFingerprint"]?.jsonPrimitive?.content!=fingerprint)throw StorageUnitFailure(ApiErrorCode.IDEMPOTENCY_KEY_REUSE_MISMATCH,HttpStatusCode.Conflict)
                    val result=json.decodeFromJsonElement<StorageUnitResponse>(checkNotNull(replay["result"]))
                    c.commit();return@use result
                }
                for(dep in dependencies) {
                    canonicalUuid(dep,"dependencyIds")
                    if(!c.exists("select 1 from silomath.sync_operations where sync_operation_id=?::uuid and organization_id=?::uuid and state='ACKNOWLEDGED'",dep,org))
                        throw StorageUnitFailure(ApiErrorCode.SYNC_DEPENDENCY_PENDING,HttpStatusCode.Conflict)
                }
                val now=Instant.now()
                val before=if(create)null else (c.query("select $columns from silomath.storage_units where organization_id=?::uuid and storage_unit_id=?::uuid for update",listOf(org,id)).singleOrNull() ?: notFound())
                if(before!=null && before.entityVersion!=base)throw StorageUnitFailure(ApiErrorCode.ENTITY_VERSION_CONFLICT,HttpStatusCode.Conflict,version=before.entityVersion)
                val result=if(create) {
                    if(!c.exists("select 1 from silomath.locations where location_id=?::uuid and organization_id=?::uuid for share",location,org))notFound()
                    val entity=id ?: UUID.randomUUID().toString()
                    if(c.exists("select 1 from silomath.storage_units where storage_unit_id=?::uuid",entity))throw StorageUnitFailure(ApiErrorCode.ENTITY_VERSION_CONFLICT,HttpStatusCode.Conflict)
                    c.query("insert into silomath.storage_units(storage_unit_id,organization_id,location_id,name,code,description,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) values(?::uuid,?::uuid,?::uuid,?,?,?,?,?::timestamptz,?::uuid,?::timestamptz,?::uuid,1) returning $columns",
                        listOf(entity,org,location,fields.text("name"),fields.text("code"),fields.nullable("description"),fields.text("lifecycleStatus"),now.toString(),actor,now.toString(),actor)).single()
                } else {
                    val old=checkNotNull(before)
                    c.query("update silomath.storage_units set name=?,code=?,description=?,lifecycle_status=?,updated_at=?::timestamptz,updated_by=?::uuid where organization_id=?::uuid and storage_unit_id=?::uuid returning $columns",
                        listOf(fields.textOr("name",old.name),fields.textOr("code",old.code),if(fields.containsKey("description"))fields.nullable("description") else old.description,
                            fields.textOr("lifecycleStatus",old.lifecycleStatus),now.toString(),actor,org,id)).single()
                }
                val record=buildJsonObject{put("requestFingerprint",fingerprint);put("result",json.encodeToJsonElement(result))}.toString()
                c.prepareStatement("insert into silomath.sync_operations(sync_operation_id,organization_id,entity_type,entity_id,operation_type,payload,base_entity_version,state,idempotency_key,attempt_count,event_at,acknowledged_at,authoritative_entity_version,created_at,updated_at,correlation_id) values(?::uuid,?::uuid,'STORAGE_UNIT',?::uuid,?,?::jsonb,?,'ACKNOWLEDGED',?,0,?::timestamptz,?::timestamptz,?,?::timestamptz,?::timestamptz,?)").use{s->
                    val args=listOf(operationId,org,result.id,if(create)"CREATE" else "PATCH",record,base,key,(event ?: now).toString(),now.toString(),result.entityVersion,now.toString(),now.toString(),correlation)
                    args.forEachIndexed{i,v->s.setObject(i+1,v)};s.executeUpdate()
                }
                for(dep in dependencies.distinct()) {
                    c.prepareStatement("insert into silomath.sync_operation_dependencies(dependency_id,parent_operation_id,dependent_operation_id,dependency_type,created_at) values(?::uuid,?::uuid,?::uuid,'REQUIRED_BEFORE',?::timestamptz)").use { statement ->
                        listOf(UUID.randomUUID().toString(),dep,operationId,now.toString()).forEachIndexed { index,value -> statement.setString(index+1,value) }
                        statement.executeUpdate()
                    }
                }
                c.prepareStatement("insert into silomath.audit_events(audit_event_id,organization_id,actor_user_id,action,entity_type,entity_id,occurred_at,before_data,after_data,correlation_id,source) values(?::uuid,?::uuid,?::uuid,?,'STORAGE_UNIT',?::uuid,?::timestamptz,?::jsonb,?::jsonb,?,'USER')").use{s->
                    val args=listOf(UUID.randomUUID().toString(),org,actor,if(create)"STORAGE_UNIT_CREATED" else "STORAGE_UNIT_UPDATED",result.id,now.toString(),before?.let{json.encodeToString(it)},json.encodeToString(result),correlation)
                    args.forEachIndexed{i,v->s.setObject(i+1,v)};s.executeUpdate()
                }
                c.commit();result
            } catch(e:Exception){c.rollback();if(e is SQLException)throw sqlFailure(e);throw e}
        }
    }
    private fun ResultSet.record()=StorageUnitResponse(getString("storage_unit_id"),getString("organization_id"),getString("location_id"),getString("name"),getString("code"),getString("description"),getString("lifecycle_status"),
        getTimestamp("created_at").toInstant().toString(),getString("created_by"),getTimestamp("updated_at").toInstant().toString(),getString("updated_by"),getLong("entity_version"))
    private fun Connection.exists(sql:String,vararg args:Any?):Boolean=prepareStatement(sql).use{s->args.forEachIndexed{i,v->s.setObject(i+1,v)};s.executeQuery().use{it.next()}}
    private fun notFound():Nothing=throw StorageUnitFailure(ApiErrorCode.RESOURCE_NOT_FOUND,HttpStatusCode.NotFound)
    private fun sqlFailure(e:SQLException)=when(e.sqlState){
        "23505"->StorageUnitFailure(ApiErrorCode.VALIDATION_FAILED,HttpStatusCode.UnprocessableEntity,"code")
        "23503","23514","22023"->StorageUnitFailure(ApiErrorCode.VALIDATION_FAILED,HttpStatusCode.UnprocessableEntity,"request")
        "55000"->StorageUnitFailure(ApiErrorCode.ENTITY_VERSION_CONFLICT,HttpStatusCode.Conflict)
        else->StorageUnitFailure(ApiErrorCode.SERVICE_UNAVAILABLE,HttpStatusCode.ServiceUnavailable)
    }
    private fun JsonObject.text(key:String)=checkNotNull(this[key]).jsonPrimitive.content
    private fun JsonObject.nullable(key:String)=this[key]?.takeIf{it!=JsonNull}?.jsonPrimitive?.content
    private fun JsonObject.textOr(key:String,fallback:String)=if(containsKey(key))text(key) else fallback
    private fun hash(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it)}
}
