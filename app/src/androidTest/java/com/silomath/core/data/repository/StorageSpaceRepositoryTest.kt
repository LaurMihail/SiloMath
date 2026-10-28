package com.silomath.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.data.network.dto.*
import com.silomath.core.data.network.source.*
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.model.*
import com.silomath.core.model.SyncOperationState
import java.time.Instant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageSpaceRepositoryTest {
    private lateinit var db:SiloMathDatabase;private lateinit var repo:OfflineStorageSpaceRepository
    private lateinit var session:MutableStateFlow<SessionState>;private lateinit var remote:FakeRemote
    private fun id(n:Int)="40000000-0000-0000-0000-"+n.toString().padStart(12,'0')
    private val org=id(1);private val loc=id(2);private val unit=id(3);private val profile=id(5);private val actor=id(9)
    private val device=DeviceInstallationId(id(10))
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),SiloMathDatabase::class.java).build()
        session=MutableStateFlow(SessionState(userId=actor,token="test-only",activeOrganizationId=org,
            authorizedOrganizations=listOf(AuthorizedOrganizationContext(id(8),org,"Fixture","FIX","ADMIN"))))
        val auth=object:AuthRepository {
            override val sessionState:Flow<SessionState> get()=session
            override suspend fun signIn(email:String,password:String)=error("Unused");override suspend fun signOut()=error("Unused")
            override suspend fun resetPassword(email:String)=error("Unused");override suspend fun updatePassword(recoveryContext:String,newPassword:String)=error("Unused")
            override suspend fun setActiveOrganization(organizationId:String)=error("Unused");override suspend fun restoreRemoteSession()=error("Unused")
        }
        db.withTransaction {seed(0);seed(100)};remote=FakeRemote();repo=OfflineStorageSpaceRepository(db,auth,remote)
    }
    @After fun close(){db.close()}
    private fun seed(offset:Int){
        val o=id(1+offset);val l=id(2+offset);val u=id(3+offset);val f=id(4+offset);val p=id(5+offset);val v=id(6+offset)
        val sql=db.openHelper.writableDatabase
        sql.execSQL("INSERT INTO v6_organizations VALUES('$o','Fixture','$o',NULL,NULL,NULL,'ACTIVE',10,'$actor',20,'$actor',1)")
        sql.execSQL("INSERT INTO v6_locations VALUES('$l','$o','Location','L',NULL,NULL,NULL,NULL,NULL,NULL,'ACTIVE',10,'$actor',20,'$actor',1)")
        sql.execSQL("INSERT INTO v6_storage_units VALUES('$u','$o','$l','Unit','U',NULL,'ACTIVE',10,'$actor',20,'$actor',1)")
        sql.execSQL("INSERT INTO v6_geometry_families VALUES('$f','F02','Fixture','1','TEST')")
        sql.execSQL("INSERT INTO v6_geometry_profiles VALUES('$p','$o','Profile','G','$f','ACTIVE','$v',10,'$actor',20,'$actor',1)")
        sql.execSQL("""INSERT INTO v6_geometry_profile_versions(geometry_profile_version_id,geometry_profile_id,version_number,geometry_family_id,measurement_reference_height_m,maximum_fill_height_m,geometry_origin,max_capacity_m3,formula_version,schema_version,created_at,created_by) VALUES('$v','$p',1,'$f','0','1','TEST','1','1','TEST',10,'$actor')""")
    }
    private fun model(n:Int=20,code:String="S1",parent:String=unit,geometry:String=profile,status:MasterStatus=MasterStatus.ACTIVE,entityVersion:Long=1)=StorageSpace(
        StorageSpaceId(id(n)),StorageUnitId(parent),code,null,GeometryProfileId(geometry),status,Instant.EPOCH,actor,Instant.EPOCH,actor,entityVersion)
    private suspend fun op()=db.canonicalStorageSpaceReadDao().pending(org).single()
    @Test fun adminCreateIsLocalFirstAndBusinessOutboxAtomic()=runBlocking {
        val row=model();repo.saveStorageSpace(row).getOrThrow()
        assertEquals(row.storageSpaceId,repo.observeStorageSpaces(StorageUnitId(unit)).first().single().storageSpaceId)
        val op=op();assertEquals("STORAGE_SPACE",op.entityType);assertEquals("CREATE",op.operationType);assertNull(op.baseEntityVersion)
        assertTrue(op.payloadJson.contains("\"storageUnitId\":\"$unit\""))
    }
    @Test fun standardWrongTenantParentAndGeometryAreDenied()=runBlocking {
        session.value=session.value.copy(authorizedOrganizations=session.value.authorizedOrganizations.map{it.copy(role="STANDARD")})
        assertTrue(repo.saveStorageSpace(model()).isFailure)
        session.value=session.value.copy(authorizedOrganizations=session.value.authorizedOrganizations.map{it.copy(role="ADMIN")})
        assertTrue(repo.saveStorageSpace(model(parent=id(103))).isFailure)
        assertTrue(repo.saveStorageSpace(model(21,geometry=id(105))).isFailure)
        session.value=session.value.copy(activeOrganizationId=id(101),authorizedOrganizations=listOf(AuthorizedOrganizationContext(id(108),id(101),"Other","O","ADMIN")))
        assertTrue(repo.getStorageSpace(StorageSpaceId(id(20))).isFailure)
    }
    @Test fun uniquenessFixedParentVersionAndInactiveLifecycleAreEnforced()=runBlocking {
        repo.saveStorageSpace(model()).getOrThrow()
        assertTrue(repo.saveStorageSpace(model(21)).isFailure)
        val current=repo.getStorageSpace(StorageSpaceId(id(20))).getOrThrow()
        assertTrue(repo.saveStorageSpace(current.copy(storageUnitId=StorageUnitId(id(103)))).isFailure)
        assertTrue(repo.saveStorageSpace(current.copy(entityVersion=2)).isFailure)
        repo.saveStorageSpace(current.copy(lifecycleStatus=MasterStatus.INACTIVE)).getOrThrow()
        assertEquals(MasterStatus.INACTIVE,repo.getStorageSpace(current.storageSpaceId).getOrThrow().lifecycleStatus)
    }
    @Test fun historicalReferenceLocksCodeAndGeometryButPreservesLifecycle()=runBlocking {
        repo.saveStorageSpace(model()).getOrThrow();val scope=id(30);val campaign=id(31);val space=id(20)
        db.openHelper.writableDatabase.execSQL("""INSERT INTO v6_inventory_campaigns(campaign_id,organization_id,code,campaign_year,sequence,name,inventory_type,start_date,planned_end_date,status,launched_at,launched_by,created_at,created_by,updated_at,updated_by,entity_version)
            VALUES('$campaign','$org','C',2026,1,'C','ANNUAL','2026-01-01','2026-12-31','ACTIVE',10,'$actor',10,'$actor',20,'$actor',1)""")
        db.openHelper.writableDatabase.execSQL("INSERT INTO v6_campaign_scopes VALUES('$scope','$campaign','$space',10,'$actor')")
        val current=repo.getStorageSpace(StorageSpaceId(space)).getOrThrow()
        assertTrue(repo.hasHistoricalReferences(current.storageSpaceId).getOrThrow())
        assertTrue(repo.saveStorageSpace(current.copy(code="S2")).isFailure)
        assertTrue(repo.saveStorageSpace(current.copy(geometryProfileId=GeometryProfileId(id(105)))).isFailure)
        repo.saveStorageSpace(current.copy(lifecycleStatus=MasterStatus.INACTIVE)).getOrThrow()
    }
    @Test fun outboxFailureRollsBackBusinessMutation()=runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_space_outbox BEFORE INSERT ON sync_operations WHEN NEW.entity_type='STORAGE_SPACE' BEGIN SELECT RAISE(ABORT,'controlled'); END")
        assertTrue(repo.saveStorageSpace(model()).isFailure)
        assertTrue(repo.observeStorageSpaces(StorageUnitId(unit)).first().isEmpty())
    }
    @Test fun retryPreservesIdentityThenAckReconcilesAuthoritativeVersion()=runBlocking {
        val row=model();repo.saveStorageSpace(row).getOrThrow();val original=op()
        remote.failure=StorageSpaceTransportFailure("SERVICE_UNAVAILABLE",true)
        assertTrue(repo.synchronizeStorageSpaces(device,"test").isFailure)
        val retry=op();assertEquals(original.idempotencyKey,retry.idempotencyKey)
        db.syncOperationDao().updateOperation(retry.copy(retryMetadata=buildJsonObject{put("capturedByUserId",actor);put("nextAttemptAt",0)}.toString()))
        remote.failure=null;remote.server=remote.dto(row.storageSpaceId.value,2)
        repo.synchronizeStorageSpaces(device,"test").getOrThrow()
        val ack=db.syncOperationDao().getOperationById(original.syncOperationId)!!;assertEquals(SyncOperationState.ACKNOWLEDGED,ack.state)
        assertEquals(2,repo.getStorageSpace(row.storageSpaceId).getOrThrow().entityVersion)
        assertEquals(original.syncOperationId,remote.requests.last().operations.single().syncOperationId)
    }
    @Test fun conflictAndDirtyRefreshNeverSilentlyOverwrite()=runBlocking {
        val row=model();repo.saveStorageSpace(row).getOrThrow();remote.server=remote.dto(row.storageSpaceId.value,2).copy(code="REMOTE")
        remote.outcome="CONFLICT";repo.synchronizeStorageSpaces(device,"test").getOrThrow()
        assertEquals(SyncOperationState.CONFLICT,op().state);assertEquals("S1",repo.getStorageSpace(row.storageSpaceId).getOrThrow().code)
        remote.outcome="ACKNOWLEDGED";repo.refreshStorageSpaces(StorageUnitId(unit)).getOrThrow()
        assertEquals("S1",repo.getStorageSpace(row.storageSpaceId).getOrThrow().code)
    }
    private inner class FakeRemote:StorageSpaceRemoteSource {
        var server:StorageSpaceResponseDto?=null;var failure:Exception?=null;var outcome="ACKNOWLEDGED"
        val requests=mutableListOf<StorageUnitSyncBatchDto>()
        fun dto(id:String,versionValue:Long=1)=StorageSpaceResponseDto(id,org,loc,unit,"S1",null,profile,"ACTIVE",
            "1970-01-01T00:00:01Z",actor,"1970-01-01T00:00:02Z",actor,versionValue)
        override suspend fun list(org:String,unit:String)=listOfNotNull(server)
        override suspend fun get(org:String,id:String)=checkNotNull(server)
        override suspend fun push(request:StorageUnitSyncBatchDto):StorageUnitSyncBatchResultDto {
            requests+=request;failure?.let{throw it}
            return StorageUnitSyncBatchResultDto(listOf(StorageUnitSyncResultDto(request.operations.single().syncOperationId,outcome,server?.entityVersion ?: 1,
                conflict=buildJsonObject{put("code","REMOTE")})),"1970-01-01T00:00:00Z","test")
        }
    }
}
