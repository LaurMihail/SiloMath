package com.silomath.backend.storagespace

import com.silomath.backend.api.ApiErrorCode
import java.sql.*
import java.util.UUID
import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue

class StorageSpacePostgresTest {
    private lateinit var url:String;private lateinit var service:JdbcStorageSpaceService
    private fun uuid()=UUID.randomUUID().toString()
    // Reuse the retained canonical graph created by migration fixture 011 before FK hardening.
    private val org="90000000-0000-0000-0000-000000000001"
    private val actor="90000000-0000-0000-0000-000000000002"
    private val profile="90000000-0000-0000-0000-000000000007"
    private val location=uuid();private val unit=uuid()
    private fun sql(text:String)=DriverManager.getConnection(url).use{it.createStatement().use{s->s.execute(text)}}
    private fun count(table:String,where:String)=DriverManager.getConnection(url).use{it.createStatement().use{s->s.executeQuery("select count(*) from silomath.$table where $where").use{r->r.next();r.getInt(1)}}}
    private fun scalar(text:String)=DriverManager.getConnection(url).use{it.createStatement().use{s->s.executeQuery(text).use{r->r.next();r.getString(1)}}}
    @BeforeTest fun setup(){
        url=System.getProperty("silomath.md005.jdbc","");assumeTrue("Supply disposable localhost PostgreSQL",url.isNotBlank());require(url.startsWith("jdbc:postgresql://127.0.0.1:"))
        service=JdbcStorageSpaceService{DriverManager.getConnection(url)}
        assertEquals("1",scalar("""select count(*) from silomath.geometry_profiles p join silomath.geometry_profile_versions v
            on v.geometry_profile_version_id=p.current_version_id and v.geometry_profile_id=p.geometry_profile_id
            where p.geometry_profile_id='$profile' and p.organization_id='$org' and p.lifecycle_status='ACTIVE'"""))
        sql("""begin;
            update silomath.organization_memberships set role='ADMIN' where organization_id='$org' and user_id='$actor';
            insert into silomath.locations(location_id,organization_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) values('$location','$org','Location','${location.uppercase()}','ACTIVE',now(),'$actor',now(),'$actor',1);
            insert into silomath.storage_units(storage_unit_id,organization_id,location_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) values('$unit','$org','$location','Unit','U','ACTIVE',now(),'$actor',now(),'$actor',1);
            commit;""")
    }
    private fun fields(code:String="S1")=buildJsonObject{put("code",code);put("description","Original");put("geometryProfileId",profile)}
    private fun create(key:String=uuid(),op:String=uuid(),chosen:String?=null)=service.mutate(org,unit,chosen,actor,"test",key,op,null,fields(),null)
    private fun patch(id:String,version:Long,body:JsonObject,key:String=uuid(),op:String=uuid())=service.mutate(org,null,id,actor,"test",key,op,version,body,null)
    @Test fun createUpdateProjectionAndAuthoritativeVersion(){
        val a=create();assertEquals(org,a.organizationId);assertEquals(location,a.locationId);assertEquals(unit,a.storageUnitId);assertEquals(profile,a.geometryProfileId);assertEquals(1,a.entityVersion)
        val b=patch(a.id,1,buildJsonObject{put("description",JsonNull);put("lifecycleStatus","INACTIVE")})
        assertNull(b.description);assertEquals("INACTIVE",b.lifecycleStatus);assertEquals(2,b.entityVersion);assertEquals(a.createdAt,b.createdAt)
    }
    @Test fun idempotencyStaleVersionAndDuplicateCodeFailWithoutDuplicateEffects(){
        val key=uuid();val op=uuid();val a=create(key,op);assertEquals(a,service.mutate(org,unit,null,actor,"test",key,op,null,fields(),null))
        assertEquals(ApiErrorCode.IDEMPOTENCY_KEY_REUSE_MISMATCH,assertFailsWith<StorageSpaceFailure>{service.mutate(org,unit,null,actor,"test",key,op,null,fields("OTHER"),null)}.code)
        patch(a.id,1,buildJsonObject{put("description","Changed")})
        assertEquals(ApiErrorCode.ENTITY_VERSION_CONFLICT,assertFailsWith<StorageSpaceFailure>{patch(a.id,1,buildJsonObject{put("description","Stale")})}.code)
        assertEquals(ApiErrorCode.VALIDATION_FAILED,assertFailsWith<StorageSpaceFailure>{create()}.code)
        assertEquals(2,count("sync_operations","entity_id='${a.id}'"));assertEquals(2,count("audit_events","entity_id='${a.id}'"))
    }
    @Test fun wrongParentGeometryTenantAndStandardMutationAreDenied(){
        assertEquals(ApiErrorCode.RESOURCE_NOT_FOUND,assertFailsWith<StorageSpaceFailure>{service.mutate(org,uuid(),null,actor,"x",uuid(),uuid(),null,fields(),null)}.code)
        val bad=JsonObject(fields()+("geometryProfileId" to JsonPrimitive(uuid())))
        assertEquals(ApiErrorCode.RESOURCE_NOT_FOUND,assertFailsWith<StorageSpaceFailure>{service.mutate(org,unit,null,actor,"x",uuid(),uuid(),null,bad,null)}.code)
        assertNull(service.get(uuid(),uuid()));sql("update silomath.organization_memberships set role='STANDARD' where organization_id='$org' and user_id='$actor'")
        assertEquals(ApiErrorCode.AUTH_FORBIDDEN,assertFailsWith<StorageSpaceFailure>{create()}.code)
    }
    @Test fun historicalIdentityAndReferencesRemainNonDestructive(){
        val a=create();val campaign=uuid();val scope=uuid()
        sql("""insert into silomath.inventory_campaigns(campaign_id,organization_id,code,campaign_year,sequence,name,inventory_type,start_date,planned_end_date,status,launched_at,launched_by,created_at,created_by,updated_at,updated_by,entity_version)
            values('$campaign','$org','INV-2026-0001',2026,1,'C','ANNUAL',current_date,current_date,'ACTIVE',now(),'$actor',now(),'$actor',now(),'$actor',1);
            insert into silomath.campaign_scopes(campaign_scope_id,campaign_id,storage_space_id,created_at,created_by) values('$scope','$campaign','${a.id}',now(),'$actor');""")
        assertEquals(ApiErrorCode.ENTITY_VERSION_CONFLICT,assertFailsWith<StorageSpaceFailure>{patch(a.id,1,buildJsonObject{put("code","OTHER")})}.code)
        assertEquals("INACTIVE",patch(a.id,1,buildJsonObject{put("lifecycleStatus","INACTIVE")}).lifecycleStatus)
        assertFailsWith<SQLException>{sql("update silomath.storage_spaces set storage_unit_id='${uuid()}' where storage_space_id='${a.id}'")}
        assertFailsWith<SQLException>{sql("delete from silomath.storage_spaces where storage_space_id='${a.id}'")}
    }
    @Test fun batchCreateIsAtomicAndIdempotent(){
        val failedKey=uuid();val failedOp=uuid()
        assertEquals(ApiErrorCode.VALIDATION_FAILED,assertFailsWith<StorageSpaceFailure>{
            service.batchCreate(org,unit,actor,"batch",failedKey,failedOp,listOf(fields("C01"),fields("C01")),null)
        }.code)
        assertEquals(0,count("storage_spaces","storage_unit_id='$unit'"));assertEquals(0,count("sync_operations","sync_operation_id='$failedOp'"))
        val key=uuid();val op=uuid();val payload=listOf(fields("C01"),fields("C02"))
        val created=service.batchCreate(org,unit,actor,"batch",key,op,payload,null)
        assertEquals(2,created.items.size);assertEquals(created,service.batchCreate(org,unit,actor,"batch",key,op,payload,null))
        assertEquals(2,count("storage_spaces","storage_unit_id='$unit'"));assertEquals(1,count("sync_operations","sync_operation_id='$op' and operation_type='BATCH_CREATE'"))
        assertEquals(ApiErrorCode.IDEMPOTENCY_KEY_REUSE_MISMATCH,assertFailsWith<StorageSpaceFailure>{
            service.batchCreate(org,unit,actor,"batch",key,op,listOf(fields("C03")),null)
        }.code)
    }
    @Test fun auditFailureRollsBackBusinessAndIdempotencyRecord(){
        val suffix=unit.replace("-","");sql("""create function silomath.reject_md005_$suffix() returns trigger language plpgsql as ${'$'}${'$'} begin if NEW.entity_id in (select storage_space_id from silomath.storage_spaces where storage_unit_id='$unit'::uuid) then raise exception 'controlled'; end if; return NEW; end ${'$'}${'$'};
            create trigger reject_md005_$suffix before insert on silomath.audit_events for each row execute function silomath.reject_md005_$suffix();""")
        val op=uuid();assertFailsWith<StorageSpaceFailure>{create(op=op)}
        assertEquals(0,count("storage_spaces","storage_unit_id='$unit'"));assertEquals(0,count("sync_operations","sync_operation_id='$op'"))
    }
}
