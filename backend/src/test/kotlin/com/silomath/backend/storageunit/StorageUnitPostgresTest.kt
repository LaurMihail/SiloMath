package com.silomath.backend.storageunit

import com.silomath.backend.api.*
import com.silomath.backend.security.*
import com.silomath.backend.routes.configureStorageUnitRouting
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import io.ktor.server.routing.*
import io.ktor.server.response.respondText
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.test.*
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue

/** Real PostgreSQL, isolated loopback test cluster only. Never targets live Supabase. */
class StorageUnitPostgresTest {
    private lateinit var url:String
    private lateinit var service:JdbcStorageUnitService
    private val org=uuid();private val actor=uuid();private val location=uuid();private val device=uuid()
    private fun uuid()=UUID.randomUUID().toString()
    private fun sql(text:String)=DriverManager.getConnection(url).use{it.createStatement().use{s->s.execute(text)}}
    private fun count(table:String,where:String)=DriverManager.getConnection(url).use{it.createStatement().use{s->s.executeQuery("select count(*) from silomath.$table where $where").use{r->r.next();r.getInt(1)}}}
    @BeforeTest fun setup() {
        url=System.getProperty("silomath.md003.jdbc","")
        assumeTrue("Supply -Pmd003JdbcUrl for real PostgreSQL validation",url.isNotBlank())
        require(url.startsWith("jdbc:postgresql://127.0.0.1:55483/silomath_md003_"))
        service=JdbcStorageUnitService{DriverManager.getConnection(url)}
        sql("""insert into silomath.users(user_id,full_name,email,email_normalized,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
            values('$actor','Test','$actor@example.invalid','$actor@example.invalid','ACTIVE',now(),'$actor',now(),'$actor',1);
            insert into silomath.organizations(organization_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
            values('$org','Test','${org.uppercase()}','ACTIVE',now(),'$actor',now(),'$actor',1);
            insert into silomath.organization_memberships(membership_id,organization_id,user_id,role,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
            values('${uuid()}','$org','$actor','ADMIN','ACTIVE',now(),'$actor',now(),'$actor',1);
            insert into silomath.locations(location_id,organization_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version)
            values('$location','$org','Test','L','ACTIVE',now(),'$actor',now(),'$actor',1);
            insert into silomath.device_installations(device_installation_id,user_id,created_at) values('$device','$actor',now());""")
    }
    private fun fields(code:String="U")=buildJsonObject{put("name"," Unit ");put("code",code);put("description","original")}
    private fun create(payload:JsonObject=fields(),key:String=uuid(),operation:String=uuid(),chosen:String?=null)=service.mutate(org,location,chosen,actor,"test-correlation",key,operation,null,payload,"2000-01-01T00:00:00Z",if(chosen!=null)device else null)
    private fun patch(id:String,version:Long,payload:JsonObject,key:String=uuid(),operation:String=uuid())=service.mutate(org,null,id,actor,"test-correlation",key,operation,version,payload,null)
    @Test fun `create update preserve canonical identity metadata null and authoritative version`() {
        val before=Instant.now().minusSeconds(1);val a=create()
        assertEquals(UUID.fromString(a.id).toString(),a.id);assertEquals(org,a.organizationId);assertEquals(location,a.locationId)
        assertEquals("Unit",a.name);assertEquals("U",a.code);assertEquals("ACTIVE",a.lifecycleStatus);assertEquals(1L,a.entityVersion)
        assertTrue(Instant.parse(a.createdAt)>before);assertEquals(actor,a.createdBy)
        val b=patch(a.id,1,buildJsonObject{put("name","New");put("lifecycleStatus","INACTIVE")})
        assertEquals(2L,b.entityVersion);assertEquals("original",b.description);assertEquals(a.createdAt,b.createdAt)
        assertEquals(a.createdBy,b.createdBy);assertEquals(actor,b.updatedBy);assertEquals("INACTIVE",b.lifecycleStatus)
        val c=patch(a.id,2,buildJsonObject{put("description",JsonNull)})
        assertNull(c.description);assertEquals(3L,c.entityVersion)
        assertEquals(3,count("sync_operations","entity_id='${a.id}'"));assertEquals(3,count("audit_events","entity_id='${a.id}'"))
        assertEquals(1,count("sync_operations","entity_id='${a.id}' and event_at='2000-01-01T00:00:00Z'"))
    }
    @Test fun `idempotency replays original result mismatch does not mutate and concurrent version wins once`() {
        val key=uuid();val op=uuid();val a=create(key=key,operation=op)
        patch(a.id,1,buildJsonObject{put("name","Changed")})
        assertEquals(a,create(key=key,operation=op))
        assertEquals(ApiErrorCode.IDEMPOTENCY_KEY_REUSE_MISMATCH,assertFailsWith<StorageUnitFailure>{create(fields("OTHER"),key,op)}.code)
        val executor=Executors.newFixedThreadPool(2)
        try {
            val futures=(1..2).map {n->executor.submit<Boolean>{try{patch(a.id,2,buildJsonObject{put("name","Concurrent$n")});true}catch(e:StorageUnitFailure){assertEquals(ApiErrorCode.ENTITY_VERSION_CONFLICT,e.code);false}}}
            assertEquals(1,futures.count{it.get()})
        } finally{executor.shutdown()}
        assertEquals(3,count("sync_operations","entity_id='${a.id}'"));assertEquals(3,count("audit_events","entity_id='${a.id}'"))
    }
    @Test fun `uniqueness scope immutable parent and new cross tenant references fail closed`() {
        val a=create();assertEquals(ApiErrorCode.VALIDATION_FAILED,assertFailsWith<StorageUnitFailure>{create(fields(" u "))}.code)
        assertNull(service.get(uuid(),a.id));assertFailsWith<StorageUnitFailure>{service.list(uuid(),location,null,null,null,null)}
        assertFailsWith<StorageUnitFailure>{patch(a.id,1,buildJsonObject{put("locationId",uuid())})}
        val second=uuid();sql("insert into silomath.locations(location_id,organization_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) values('$second','$org','Second','L2','ACTIVE',now(),'$actor',now(),'$actor',1)")
        service.mutate(org,second,null,actor,"test",uuid(),uuid(),null,fields(),null)
        assertFailsWith<SQLException>{sql("update silomath.storage_units set location_id='$second' where storage_unit_id='${a.id}'")}
        val wrongOrg=uuid()
        sql("insert into silomath.organizations(organization_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) values('$wrongOrg','Other','${wrongOrg.uppercase()}','ACTIVE',now(),'$actor',now(),'$actor',1)")
        val crossTenant=assertFailsWith<SQLException>{sql("insert into silomath.storage_units(storage_unit_id,organization_id,location_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) values('${uuid()}','$wrongOrg','$location','Bad','BAD','ACTIVE',now(),'$actor',now(),'$actor',1)")}
        assertTrue(crossTenant.message!!.contains("fk_storage_units_location_organization_md003"))
    }
    @Test fun `server audit failure rolls back business and sync operation together`() {
        val suffix=org.replace("-","")
        sql("""create function silomath.reject_md003_$suffix() returns trigger language plpgsql as ${'$'}${'$'} begin if NEW.organization_id='$org'::uuid then raise exception 'controlled audit failure'; end if; return NEW; end ${'$'}${'$'};
            create trigger reject_md003_$suffix before insert on silomath.audit_events for each row execute function silomath.reject_md003_$suffix();""")
        val op=uuid();assertFailsWith<StorageUnitFailure>{create(operation=op)}
        assertEquals(0,count("storage_units","organization_id='$org'"));assertEquals(0,count("sync_operations","sync_operation_id='$op'"))
    }
    @Test fun `historical code parent identity and references remain non destructive`() {
        val ho="90000000-0000-0000-0000-000000000001";val ha="90000000-0000-0000-0000-000000000002";val hu="90000000-0000-0000-0000-000000000004"
        val old=service.get(ho,hu)!!
        assertFailsWith<StorageUnitFailure>{service.mutate(ho,null,hu,ha,"history",uuid(),uuid(),old.entityVersion,buildJsonObject{put("code","CHANGED")},null)}
        val updated=service.mutate(ho,null,hu,ha,"history",uuid(),uuid(),old.entityVersion,buildJsonObject{put("name","Renamed history");put("lifecycleStatus","INACTIVE")},null)
        assertEquals("REF",updated.code);assertEquals("INACTIVE",updated.lifecycleStatus)
        assertFailsWith<SQLException>{sql("delete from silomath.storage_units where storage_unit_id='$hu'")}
        assertEquals(1,count("campaign_snapshot_scope_items","storage_unit_id='$hu' and storage_unit_code='REF'"))
    }
    @Test fun `sync dependencies device binding and transactional authorization recheck`() {
        val chosen=uuid();val dependency=uuid();val operation=uuid();val key=uuid()
        assertEquals(ApiErrorCode.SYNC_DEPENDENCY_PENDING,assertFailsWith<StorageUnitFailure>{
            service.mutate(org,location,chosen,actor,"test",key,operation,null,fields("OFF"),null,device,listOf(dependency))
        }.code)
        assertNull(service.get(org,chosen))
        create(fields("DEP"),operation=dependency)
        val result=service.mutate(org,location,chosen,actor,"test",key,operation,null,fields("OFF"),null,device,listOf(dependency))
        assertEquals(chosen,result.id);assertEquals(1,count("sync_operation_dependencies","parent_operation_id='$dependency' and dependent_operation_id='$operation'"))
        assertEquals(result,service.mutate(org,location,chosen,actor,"test",key,operation,null,fields("OFF"),null,device,listOf(dependency)))
        assertEquals(ApiErrorCode.IDEMPOTENCY_KEY_REUSE_MISMATCH,assertFailsWith<StorageUnitFailure>{service.mutate(org,location,chosen,actor,"test",key,operation,null,fields("OFF"),null,device,emptyList())}.code)
        assertEquals(ApiErrorCode.AUTH_FORBIDDEN,assertFailsWith<StorageUnitFailure>{service.mutate(org,location,uuid(),actor,"test",uuid(),uuid(),null,fields("WRONG"),null,uuid())}.code)
        sql("update silomath.organization_memberships set role='STANDARD' where organization_id='$org' and user_id='$actor'")
        assertEquals(ApiErrorCode.AUTH_FORBIDDEN,assertFailsWith<StorageUnitFailure>{create(fields("DENIED"))}.code)
        sql("update silomath.organization_memberships set role='ADMIN' where organization_id='$org' and user_id='$actor'; update silomath.users set lifecycle_status='DISABLED' where user_id='$actor'")
        assertEquals(ApiErrorCode.AUTH_FORBIDDEN,assertFailsWith<StorageUnitFailure>{create(fields("DISABLED"))}.code)
    }
    @Test fun `HTTP endpoint authorization headers mapping pagination and stable UUID batch`()=testApplication {
        val standard=uuid()
        sql("""insert into silomath.users(user_id,full_name,email,email_normalized,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) values('$standard','Standard','$standard@example.invalid','$standard@example.invalid','ACTIVE',now(),'$actor',now(),'$actor',1);
            insert into silomath.organization_memberships(membership_id,organization_id,user_id,role,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) values('${uuid()}','$org','$standard','STANDARD','ACTIVE',now(),'$actor',now(),'$actor',1);""")
        val resolver=OrganizationAuthorizationResolver { user,o ->
            DriverManager.getConnection(url).use{c->c.prepareStatement("select membership_id,role from silomath.organization_memberships where user_id=?::uuid and organization_id=?::uuid and lifecycle_status='ACTIVE'").use{s->s.setString(1,user);s.setString(2,o);s.executeQuery().use{v->
                if(v.next())OrganizationAuthorizationResult.Authorized(AuthorizedOrganizationMembership(v.getString(1),o,OrganizationRole.valueOf(v.getString(2)))) else OrganizationAuthorizationResult.Forbidden
            }}}
        }
        application {
            configureApiSerialization();configureCorrelationIds()
            routing{get("/api/v1/auth/md003-isolation-test"){call.respondText("independent")}}
            configureStorageUnitRouting(AccessTokenVerifier{token->when(token){"admin"->AccessTokenVerificationResult.Authenticated(AuthenticatedSubject(actor));"standard"->AccessTokenVerificationResult.Authenticated(AuthenticatedSubject(standard));else->AccessTokenVerificationResult.InvalidOrExpired}},resolver,service)
        }
        assertEquals(HttpStatusCode.OK,client.get("/api/v1/auth/md003-isolation-test").status)
        val path="/api/v1/organizations/$org/locations/$location/storage-units"
        assertEquals(HttpStatusCode.Unauthorized,client.get(path).status)
        assertEquals(HttpStatusCode.OK,client.get(path){bearerAuth("standard")}.status)
        val key=uuid();val op=uuid();val body=buildJsonObject{put("name"," Test ");put("code"," a ");put("idempotencyKey",key);put("clientOperationId",op)}.toString()
        assertEquals(HttpStatusCode.Forbidden,client.post(path){bearerAuth("standard");contentType(ContentType.Application.Json);header("Idempotency-Key",key);setBody(body)}.status)
        assertEquals(HttpStatusCode.UnprocessableEntity,client.post(path){bearerAuth("admin");contentType(ContentType.Application.Json);header("Idempotency-Key",key);setBody(buildJsonObject{put("name","Bad");put("code","BAD");put("clientOperationId",uuid());put("remoteId","legacy")}.toString())}.status)
        val response=client.post(path){bearerAuth("admin");contentType(ContentType.Application.Json);header("Idempotency-Key",key);setBody(body)}
        assertEquals(HttpStatusCode.Created,response.status,response.bodyAsText());val data=Json.parseToJsonElement(response.bodyAsText()).jsonObject;val entity=data["id"]!!.jsonPrimitive.content
        assertEquals("A",data["code"]!!.jsonPrimitive.content);assertFalse(data.containsKey("remoteId"));assertEquals(1L,data["entityVersion"]!!.jsonPrimitive.long)
        assertEquals(HttpStatusCode.OK,client.get("/api/v1/organizations/$org/storage-units/$entity"){bearerAuth("standard")}.status)
        val patchPath="/api/v1/organizations/$org/storage-units/$entity"
        suspend fun change(version:Long)=client.patch(patchPath){bearerAuth("admin");contentType(ContentType.Application.Json);header("Idempotency-Key",uuid());header("If-Match-Entity-Version",version);setBody(buildJsonObject{put("clientOperationId",uuid());put("description",JsonNull);put("lifecycleStatus","INACTIVE")}.toString())}
        assertEquals(HttpStatusCode.OK,change(1).status);assertEquals(HttpStatusCode.Conflict,change(1).status)
        assertEquals(HttpStatusCode.Forbidden,client.get("/api/v1/organizations/${uuid()}/storage-units/$entity"){bearerAuth("admin")}.status)
        val chosen=uuid();val batch=buildJsonObject {
            put("deviceInstallationId",device);put("clientVersion","test-only");put("checkpointContext",JsonNull)
            put("operations",buildJsonArray{add(buildJsonObject{put("syncOperationId",uuid());put("idempotencyKey",uuid());put("organizationId",org);put("campaignId",JsonNull);put("entityType","STORAGE_UNIT");put("entityId",chosen);put("operationType","CREATE");put("baseEntityVersion",JsonNull);put("eventAtUtc","2000-01-01T00:00:00Z");put("dependencyIds",JsonArray(emptyList()));put("payload",buildJsonObject{put("locationId",location);put("name","Offline");put("code","OFF")})})})
        }.toString()
        suspend fun push()=client.post("/api/v1/sync/batch"){bearerAuth("admin");contentType(ContentType.Application.Json);header("X-Device-Installation-Id",device);setBody(batch)}
        val ack=push();assertEquals(HttpStatusCode.OK,ack.status,ack.bodyAsText());assertTrue(ack.bodyAsText().contains("ACKNOWLEDGED"));assertEquals(chosen,service.get(org,chosen)!!.id)
        assertTrue(push().bodyAsText().contains("ACKNOWLEDGED"));assertEquals(1,count("audit_events","entity_id='$chosen'"))
        assertEquals(HttpStatusCode.OK,client.get("$path?search=off&status=ACTIVE&limit=1"){bearerAuth("standard")}.status)
    }
}
