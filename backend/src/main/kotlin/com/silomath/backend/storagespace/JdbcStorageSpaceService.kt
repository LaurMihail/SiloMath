package com.silomath.backend.storagespace

import com.silomath.backend.api.*
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.sql.*
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class JdbcStorageSpaceService(private val connections:()->Connection):StorageSpaceService {
    constructor(url:String,user:String?,password:String?):this({DriverManager.getConnection(url,java.util.Properties().apply{
        user?.let{setProperty("user",it)};password?.let{setProperty("password",it)}
    })})
    private val json=Json{encodeDefaults=true;explicitNulls=true}
    private val cursors=CursorCodec()
    private val columns="s.storage_space_id,s.organization_id,u.location_id,s.storage_unit_id,s.code,s.description,s.geometry_profile_id,s.lifecycle_status,s.created_at,s.created_by,s.updated_at,s.updated_by,s.entity_version"
    private fun Connection.query(sql:String,args:List<Any?>):List<StorageSpaceResponse> = prepareStatement(sql).use{s->
        args.forEachIndexed{i,v->s.setObject(i+1,v)};s.executeQuery().use{r->buildList{while(r.next())add(r.record())}}
    }
    override fun list(org:String,unit:String,cursor:String?,search:String?,status:String?,limit:Int?):StorageSpacePage {
        spaceUuid(org,"organizationId");spaceUuid(unit,"storageUnitId")
        if(status!=null && status !in setOf("ACTIVE","INACTIVE"))invalidSpace("status")
        val after=cursor?.let{cursors.decode(it) ?: invalidSpace("cursor")};after?.second?.let{spaceUuid(it,"cursor")}
        val size=PageLimitPolicy(50,200).normalize(limit)
        return connections().use{c->
            if(!c.exists("""select 1 from silomath.storage_units u join silomath.locations l on l.location_id=u.location_id and l.organization_id=u.organization_id
                where u.storage_unit_id=?::uuid and u.organization_id=?::uuid""",unit,org))notFound()
            val args=mutableListOf<Any?>(org,unit)
            val where=buildString {
                append("s.organization_id=?::uuid and s.storage_unit_id=?::uuid")
                if(status!=null){append(" and s.lifecycle_status=?");args+=status}
                if(!search.isNullOrBlank()){append(" and strpos(lower(s.code),lower(?))>0");args+=search.trim()}
                if(after!=null){append(" and (s.code,s.storage_space_id)>(?,?::uuid)");args+=after.first;args+=after.second}
            }
            args+=size+1
            val rows=c.query("select $columns from silomath.storage_spaces s join silomath.storage_units u on u.storage_unit_id=s.storage_unit_id and u.organization_id=s.organization_id where $where order by s.code,s.storage_space_id limit ?",args)
            val page=rows.take(size);val more=rows.size>size
            StorageSpacePage(page,if(more)cursors.encode(page.last().code,page.last().id) else null,more)
        }
    }
    override fun get(org:String,id:String):StorageSpaceResponse? {
        spaceUuid(org,"organizationId");spaceUuid(id,"storageSpaceId")
        return connections().use{it.query("select $columns from silomath.storage_spaces s join silomath.storage_units u on u.storage_unit_id=s.storage_unit_id and u.organization_id=s.organization_id where s.organization_id=?::uuid and s.storage_space_id=?::uuid",listOf(org,id)).singleOrNull()}
    }
    override fun mutate(org:String,unit:String?,id:String?,actor:String,correlation:String,key:String,operationId:String,
        base:Long?,payload:JsonObject,eventAt:String?,device:String?,dependencies:List<String>):StorageSpaceResponse {
        spaceUuid(org,"organizationId");spaceUuid(actor,"actor");spaceUuid(key,"idempotencyKey");spaceUuid(operationId,"clientOperationId")
        unit?.let{spaceUuid(it,"storageUnitId")};id?.let{spaceUuid(it,"storageSpaceId")}
        val create=unit!=null
        if(!create && (id==null || base==null || base<1))invalidSpace("baseEntityVersion")
        if(create && base!=null)invalidSpace("baseEntityVersion")
        val fields=StorageSpaceFields.parse(payload,create)
        val event=eventAt?.let{try{Instant.parse(it)}catch(_:Exception){invalidSpace("occurredAtUtc")}}
        val fingerprint=hash(buildJsonObject {
            put("organizationId",org);put("storageUnitId",unit);put("id",id);put("actor",actor);put("operationId",operationId);put("base",base)
            put("event",event?.toString());put("device",device);put("dependencies",JsonArray(dependencies.distinct().sorted().map(::JsonPrimitive)))
            put("fields",JsonObject(fields.toSortedMap()))
        }.toString())
        return connections().use{c->
            c.autoCommit=false
            try {
                c.prepareStatement("select pg_advisory_xact_lock(hashtextextended(?,0))").use{it.setString(1,key);it.execute()}
                if(!c.exists("""select 1 from silomath.organization_memberships m join silomath.users u on u.user_id=m.user_id
                    join silomath.organizations o on o.organization_id=m.organization_id where o.lifecycle_status='ACTIVE' and
                    m.organization_id=?::uuid and m.user_id=?::uuid and m.role='ADMIN' and m.lifecycle_status='ACTIVE' and u.lifecycle_status='ACTIVE' for share of m,u,o""",org,actor))
                    throw StorageSpaceFailure(ApiErrorCode.AUTH_FORBIDDEN,HttpStatusCode.Forbidden)
                if(device!=null){spaceUuid(device,"deviceInstallationId");if(!c.exists("select 1 from silomath.device_installations where device_installation_id=?::uuid and user_id=?::uuid and revoked_at is null",device,actor))throw StorageSpaceFailure(ApiErrorCode.AUTH_FORBIDDEN,HttpStatusCode.Forbidden)}
                val replay=c.prepareStatement("select payload::text from silomath.sync_operations where idempotency_key=?").use{s->s.setString(1,key);s.executeQuery().use{r->if(r.next())json.parseToJsonElement(r.getString(1)).jsonObject else null}}
                if(replay!=null){if(replay["requestFingerprint"]?.jsonPrimitive?.content!=fingerprint)throw StorageSpaceFailure(ApiErrorCode.IDEMPOTENCY_KEY_REUSE_MISMATCH,HttpStatusCode.Conflict)
                    val result=json.decodeFromJsonElement<StorageSpaceResponse>(checkNotNull(replay["result"]));c.commit();return@use result}
                dependencies.forEach {dep->spaceUuid(dep,"dependencyIds");if(!c.exists("select 1 from silomath.sync_operations where sync_operation_id=?::uuid and organization_id=?::uuid and state='ACKNOWLEDGED'",dep,org))
                    throw StorageSpaceFailure(ApiErrorCode.SYNC_DEPENDENCY_PENDING,HttpStatusCode.Conflict)}
                val before=if(create)null else c.query("select $columns from silomath.storage_spaces s join silomath.storage_units u on u.storage_unit_id=s.storage_unit_id and u.organization_id=s.organization_id where s.organization_id=?::uuid and s.storage_space_id=?::uuid for update of s",listOf(org,id)).singleOrNull() ?: notFound()
                if(before!=null && before.entityVersion!=base)throw StorageSpaceFailure(ApiErrorCode.ENTITY_VERSION_CONFLICT,HttpStatusCode.Conflict,version=before.entityVersion)
                val parent=unit ?: checkNotNull(before).storageUnitId
                if(!c.exists("""select 1 from silomath.storage_units u join silomath.locations l on l.location_id=u.location_id and l.organization_id=u.organization_id
                    where u.storage_unit_id=?::uuid and u.organization_id=?::uuid for share of u,l""",parent,org))notFound()
                val geometry=if(fields.containsKey("geometryProfileId"))fields.text("geometryProfileId")
                    else before?.geometryProfileId ?: invalidSpace("geometryProfileId")
                val changing=before==null || before.geometryProfileId!=geometry
                if(!c.exists("select 1 from silomath.geometry_profiles where geometry_profile_id=?::uuid and organization_id=?::uuid and (?=false or lifecycle_status='ACTIVE') for share",geometry,org,changing))notFound()
                val now=Instant.now();val entity=id ?: UUID.randomUUID().toString()
                if(create) {
                    if(c.exists("select 1 from silomath.storage_spaces where storage_space_id=?::uuid",entity))throw StorageSpaceFailure(ApiErrorCode.ENTITY_VERSION_CONFLICT,HttpStatusCode.Conflict)
                    c.prepareStatement("""insert into silomath.storage_spaces(storage_space_id,organization_id,storage_unit_id,code,description,geometry_profile_id,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
                        values(?::uuid,?::uuid,?::uuid,?,?,?::uuid,?,?::timestamptz,?::uuid,?::timestamptz,?::uuid,1)""").use{s->
                        listOf(entity,org,parent,fields.text("code"),fields.nullable("description"),geometry,fields.text("lifecycleStatus"),now.toString(),actor,now.toString(),actor).forEachIndexed{i,v->s.setObject(i+1,v)};s.executeUpdate()}
                } else c.prepareStatement("""update silomath.storage_spaces set code=?,description=?,geometry_profile_id=?::uuid,lifecycle_status=?,updated_at=?::timestamptz,updated_by=?::uuid
                    where organization_id=?::uuid and storage_space_id=?::uuid""").use{s->
                    listOf(fields.textOr("code",before!!.code),if(fields.containsKey("description"))fields.nullable("description") else before.description,geometry,
                        fields.textOr("lifecycleStatus",before.lifecycleStatus),now.toString(),actor,org,id).forEachIndexed{i,v->s.setObject(i+1,v)};check(s.executeUpdate()==1)}
                val result=c.query("select $columns from silomath.storage_spaces s join silomath.storage_units u on u.storage_unit_id=s.storage_unit_id and u.organization_id=s.organization_id where s.organization_id=?::uuid and s.storage_space_id=?::uuid",listOf(org,entity)).single()
                val record=buildJsonObject{put("requestFingerprint",fingerprint);put("result",json.encodeToJsonElement(result))}.toString()
                c.prepareStatement("""insert into silomath.sync_operations(sync_operation_id,organization_id,entity_type,entity_id,operation_type,payload,base_entity_version,state,idempotency_key,attempt_count,event_at,acknowledged_at,authoritative_entity_version,created_at,updated_at,correlation_id)
                    values(?::uuid,?::uuid,'STORAGE_SPACE',?::uuid,?,?::jsonb,?,'ACKNOWLEDGED',?,0,?::timestamptz,?::timestamptz,?,?::timestamptz,?::timestamptz,?)""").use{s->
                    listOf(operationId,org,result.id,if(create)"CREATE" else "PATCH",record,base,key,(event?:now).toString(),now.toString(),result.entityVersion,now.toString(),now.toString(),correlation).forEachIndexed{i,v->s.setObject(i+1,v)};s.executeUpdate()}
                dependencies.distinct().forEach{dep->c.prepareStatement("insert into silomath.sync_operation_dependencies(dependency_id,parent_operation_id,dependent_operation_id,dependency_type,created_at) values(?::uuid,?::uuid,?::uuid,'REQUIRED_BEFORE',?::timestamptz)").use{s->
                    listOf(UUID.randomUUID().toString(),dep,operationId,now.toString()).forEachIndexed{i,v->s.setString(i+1,v)};s.executeUpdate()}}
                c.prepareStatement("""insert into silomath.audit_events(audit_event_id,organization_id,actor_user_id,action,entity_type,entity_id,occurred_at,before_data,after_data,correlation_id,source)
                    values(?::uuid,?::uuid,?::uuid,?,'STORAGE_SPACE',?::uuid,?::timestamptz,?::jsonb,?::jsonb,?,'USER')""").use{s->
                    listOf(UUID.randomUUID().toString(),org,actor,if(create)"STORAGE_SPACE_CREATED" else "STORAGE_SPACE_UPDATED",result.id,now.toString(),before?.let{json.encodeToString(it)},json.encodeToString(result),correlation).forEachIndexed{i,v->s.setObject(i+1,v)};s.executeUpdate()}
                c.commit();result
            }catch(e:Exception){c.rollback();if(e is SQLException)throw sqlFailure(e);throw e}
        }
    }
    override fun batchCreate(org:String,unit:String,actor:String,correlation:String,key:String,operationId:String,
        payloads:List<JsonObject>,eventAt:String?):StorageSpaceBatchResponse {
        spaceUuid(org,"organizationId");spaceUuid(unit,"storageUnitId");spaceUuid(actor,"actor")
        spaceUuid(key,"idempotencyKey");spaceUuid(operationId,"clientOperationId")
        if(payloads.isEmpty())invalidSpace("items")
        val fields=payloads.map{StorageSpaceFields.parse(it,true)}
        if(fields.map{it.text("code")}.distinct().size!=fields.size)invalidSpace("code")
        val event=eventAt?.let{try{Instant.parse(it)}catch(_:Exception){invalidSpace("occurredAtUtc")}}
        val fingerprint=hash(buildJsonObject {
            put("organizationId",org);put("storageUnitId",unit);put("actor",actor);put("operationId",operationId)
            put("event",event?.toString());put("items",JsonArray(fields.map{JsonObject(it.toSortedMap())}))
        }.toString())
        return connections().use{c->
            c.autoCommit=false
            try {
                c.prepareStatement("select pg_advisory_xact_lock(hashtextextended(?,0))").use{it.setString(1,key);it.execute()}
                if(!c.exists("""select 1 from silomath.organization_memberships m join silomath.users u on u.user_id=m.user_id
                    join silomath.organizations o on o.organization_id=m.organization_id where o.lifecycle_status='ACTIVE' and
                    m.organization_id=?::uuid and m.user_id=?::uuid and m.role='ADMIN' and m.lifecycle_status='ACTIVE' and u.lifecycle_status='ACTIVE' for share of m,u,o""",org,actor))
                    throw StorageSpaceFailure(ApiErrorCode.AUTH_FORBIDDEN,HttpStatusCode.Forbidden)
                val replay=c.prepareStatement("select payload::text from silomath.sync_operations where idempotency_key=?").use{s->s.setString(1,key);s.executeQuery().use{r->if(r.next())json.parseToJsonElement(r.getString(1)).jsonObject else null}}
                if(replay!=null){
                    if(replay["requestFingerprint"]?.jsonPrimitive?.content!=fingerprint)throw StorageSpaceFailure(ApiErrorCode.IDEMPOTENCY_KEY_REUSE_MISMATCH,HttpStatusCode.Conflict)
                    val result=json.decodeFromJsonElement<StorageSpaceBatchResponse>(checkNotNull(replay["result"]));c.commit();return@use result
                }
                if(!c.exists("""select 1 from silomath.storage_units u join silomath.locations l on l.location_id=u.location_id and l.organization_id=u.organization_id
                    where u.storage_unit_id=?::uuid and u.organization_id=?::uuid for share of u,l""",unit,org))notFound()
                fields.forEach { item ->
                    val geometry=item.text("geometryProfileId")
                    if(!c.exists("select 1 from silomath.geometry_profiles where geometry_profile_id=?::uuid and organization_id=?::uuid and lifecycle_status='ACTIVE' for share",geometry,org))notFound()
                    if(c.exists("select 1 from silomath.storage_spaces where storage_unit_id=?::uuid and code=?",unit,item.text("code")))
                        throw StorageSpaceFailure(ApiErrorCode.VALIDATION_FAILED,HttpStatusCode.UnprocessableEntity,"code")
                }
                val now=Instant.now();val ids=fields.map{UUID.randomUUID().toString()}
                fields.forEachIndexed { index,item ->
                    c.prepareStatement("""insert into silomath.storage_spaces(storage_space_id,organization_id,storage_unit_id,code,description,geometry_profile_id,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
                        values(?::uuid,?::uuid,?::uuid,?,?,?::uuid,?,?::timestamptz,?::uuid,?::timestamptz,?::uuid,1)""").use{s->
                        listOf(ids[index],org,unit,item.text("code"),item.nullable("description"),item.text("geometryProfileId"),item.text("lifecycleStatus"),now.toString(),actor,now.toString(),actor)
                            .forEachIndexed{i,v->s.setObject(i+1,v)};s.executeUpdate()}
                }
                val results=ids.map{id->c.query("select $columns from silomath.storage_spaces s join silomath.storage_units u on u.storage_unit_id=s.storage_unit_id and u.organization_id=s.organization_id where s.organization_id=?::uuid and s.storage_space_id=?::uuid",listOf(org,id)).single()}
                val result=StorageSpaceBatchResponse(results)
                val record=buildJsonObject{put("requestFingerprint",fingerprint);put("result",json.encodeToJsonElement(result))}.toString()
                c.prepareStatement("""insert into silomath.sync_operations(sync_operation_id,organization_id,entity_type,entity_id,operation_type,payload,base_entity_version,state,idempotency_key,attempt_count,event_at,acknowledged_at,authoritative_entity_version,created_at,updated_at,correlation_id)
                    values(?::uuid,?::uuid,'STORAGE_SPACE',?::uuid,'BATCH_CREATE',?::jsonb,null,'ACKNOWLEDGED',?,0,?::timestamptz,?::timestamptz,1,?::timestamptz,?::timestamptz,?)""").use{s->
                    listOf(operationId,org,results.first().id,record,key,(event?:now).toString(),now.toString(),now.toString(),now.toString(),correlation).forEachIndexed{i,v->s.setObject(i+1,v)};s.executeUpdate()}
                results.forEach { item ->
                    c.prepareStatement("""insert into silomath.audit_events(audit_event_id,organization_id,actor_user_id,action,entity_type,entity_id,occurred_at,before_data,after_data,correlation_id,source)
                        values(?::uuid,?::uuid,?::uuid,'STORAGE_SPACE_CREATED','STORAGE_SPACE',?::uuid,?::timestamptz,null,?::jsonb,?,'USER')""").use{s->
                        listOf(UUID.randomUUID().toString(),org,actor,item.id,now.toString(),json.encodeToString(item),correlation).forEachIndexed{i,v->s.setObject(i+1,v)};s.executeUpdate()}
                }
                c.commit();result
            }catch(e:Exception){c.rollback();if(e is SQLException)throw sqlFailure(e);throw e}
        }
    }
    private fun ResultSet.record()=StorageSpaceResponse(getString("storage_space_id"),getString("organization_id"),getString("location_id"),getString("storage_unit_id"),getString("code"),getString("description"),
        getString("geometry_profile_id"),getString("lifecycle_status"),getTimestamp("created_at").toInstant().toString(),getString("created_by"),getTimestamp("updated_at").toInstant().toString(),getString("updated_by"),getLong("entity_version"))
    private fun Connection.exists(sql:String,vararg args:Any?):Boolean=prepareStatement(sql).use{s->args.forEachIndexed{i,v->s.setObject(i+1,v)};s.executeQuery().use{it.next()}}
    private fun notFound():Nothing=throw StorageSpaceFailure(ApiErrorCode.RESOURCE_NOT_FOUND,HttpStatusCode.NotFound)
    private fun sqlFailure(e:SQLException)=when(e.sqlState){"23505"->StorageSpaceFailure(ApiErrorCode.VALIDATION_FAILED,HttpStatusCode.UnprocessableEntity,"code")
        "23503","23514","22023"->StorageSpaceFailure(ApiErrorCode.VALIDATION_FAILED,HttpStatusCode.UnprocessableEntity,"request")
        "55000"->StorageSpaceFailure(ApiErrorCode.ENTITY_VERSION_CONFLICT,HttpStatusCode.Conflict)
        else->StorageSpaceFailure(ApiErrorCode.SERVICE_UNAVAILABLE,HttpStatusCode.ServiceUnavailable)}
    private fun JsonObject.text(key:String)=checkNotNull(this[key]).jsonPrimitive.content
    private fun JsonObject.nullable(key:String)=this[key]?.takeIf{it!=JsonNull}?.jsonPrimitive?.content
    private fun JsonObject.textOr(key:String,fallback:String)=if(containsKey(key))text(key) else fallback
    private fun hash(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){"%02x".format(it)}
}
