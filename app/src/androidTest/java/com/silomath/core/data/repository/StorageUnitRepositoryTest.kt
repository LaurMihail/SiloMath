package com.silomath.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.entity.*
import com.silomath.core.database.entity.canonical.*
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.data.network.dto.*
import com.silomath.core.data.network.source.*
import com.silomath.core.model.DeviceInstallationId
import com.silomath.core.model.LocationId
import com.silomath.core.model.StorageUnitId
import com.silomath.core.model.SyncOperationState
import com.silomath.core.model.storage.StorageUnitTransfer
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Controlled in-memory test DB only. Production SQLCipher factory remains mandatory. */
@RunWith(AndroidJUnit4::class)
class StorageUnitRepositoryTest {
    private lateinit var db:SiloMathDatabase
    private lateinit var repo:OfflineStorageUnitRepository
    private val org="10000000-0000-0000-0000-000000000001"
    private val actor="10000000-0000-0000-0000-000000000002"
    private val location="10000000-0000-0000-0000-000000000003"
    private val otherLocation="10000000-0000-0000-0000-000000000004"
    private val otherOrg="20000000-0000-0000-0000-000000000001"
    private val device=DeviceInstallationId("10000000-0000-0000-0000-000000000005")
    private lateinit var session:MutableStateFlow<SessionState>
    private lateinit var remote:FakeRemote
    private fun id()=UUID.randomUUID().toString()
    private fun unit(code:String="U",parent:String=location)=StorageUnit(StorageUnitId(id()),LocationId(parent),"Unit",code,null,
        MasterStatus.ACTIVE,Instant.EPOCH,actor,Instant.EPOCH,actor,1)
    private suspend fun read(id:String)=checkNotNull(db.storageUnitCanonicalDao().get(id))
    private suspend fun domain(id:String)=repo.observeStorageUnits(LocationId(location)).first().single{it.storageUnitId.value==id}
    private suspend fun makeDue(id:String) {
        val op=db.syncOperationDao().getOperationById(id)!!
        val metadata=Json.parseToJsonElement(op.retryMetadata!!).jsonObject
        db.syncOperationDao().updateOperation(op.copy(retryMetadata=JsonObject(metadata+mapOf("nextAttemptAt" to JsonPrimitive(0))).toString()))
    }
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),SiloMathDatabase::class.java).build()
        session=MutableStateFlow(SessionState(userId=actor,token="test-only",activeOrganizationId=org,
            authorizedOrganizations=listOf(AuthorizedOrganizationContext(id(),org,"Fixture","FIX","ADMIN"))))
        val auth=object:AuthRepository {
            override val sessionState:Flow<SessionState> get()=session
            override suspend fun signIn(email:String,password:String)=error("Not used")
            override suspend fun signOut()=error("Not used")
            override suspend fun resetPassword(email:String)=error("Not used")
            override suspend fun updatePassword(recoveryContext:String,newPassword:String)=error("Not used")
            override suspend fun setActiveOrganization(organizationId:String)=error("Not used")
            override suspend fun restoreRemoteSession()=error("Not used")
        }
        remote=FakeRemote();repo=OfflineStorageUnitRepository(db,auth,remote)
        for(o in listOf(org,otherOrg))db.storageUnitCanonicalDao().insertOrganization(V6OrganizationsRow(o,"Fixture",o,
            lifecycleStatus="ACTIVE",createdAt=0,createdBy=actor,updatedAt=0,updatedBy=actor,entityVersion=1))
        for((l,o) in listOf(location to org,otherLocation to org,"20000000-0000-0000-0000-000000000003" to otherOrg))
            db.storageUnitCanonicalDao().insertParent(V6LocationsRow(l,o,"Location",l,lifecycleStatus="ACTIVE",createdAt=0,
                createdBy=actor,updatedAt=0,updatedBy=actor,entityVersion=1))
    }
    @After fun close(){db.close()}
    @Test fun durableSuccessUsesCanonicalUuidAndAtomicOutbox()=runBlocking {
        val u=unit();repo.saveStorageUnit(u).getOrThrow()
        val row=read(u.storageUnitId.value);val op=db.syncOperationDao().getPendingOperationsList().single()
        assertEquals(u.storageUnitId.value,row.storageUnitId);assertEquals(org,row.organizationId)
        assertEquals(row.storageUnitId,op.entityId);assertNull(op.baseEntityVersion);assertEquals("CREATE",op.operationType)
        assertEquals(location,Json.parseToJsonElement(op.payloadJson).jsonObject["locationId"]!!.jsonPrimitive.content)
        assertFalse(op.payloadJson.contains("createdAt"));assertFalse(op.payloadJson.contains("remoteId"));assertEquals(0,remote.calls)
        assertEquals(actor,row.createdBy);assertTrue(row.createdAt>0)
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use{assertEquals(0,it.count)}
        db.openHelper.readableDatabase.query("PRAGMA table_info(v6_storage_units)").use { c ->
            val cols=buildList{while(c.moveToNext())add(c.getString(1))};assertFalse("id" in cols);assertFalse("remote_id" in cols)
        }
    }
    @Test fun failingOutboxRollsBackBusinessMutation()=runBlocking {
        val u=unit()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_unit_outbox BEFORE INSERT ON sync_operations BEGIN SELECT RAISE(ABORT, 'controlled outbox failure'); END")
        assertTrue(repo.saveStorageUnit(u).isFailure);assertNull(db.storageUnitCanonicalDao().get(u.storageUnitId.value))
        assertTrue(db.syncOperationDao().getPendingOperationsList().isEmpty())
    }
    @Test fun parentScopeUniquenessLifecycleAndOptimisticVersion()=runBlocking {
        val u=unit();repo.saveStorageUnit(u).getOrThrow();val current=domain(u.storageUnitId.value)
        assertTrue(repo.saveStorageUnit(unit()).isFailure)
        repo.saveStorageUnit(unit(parent=otherLocation)).getOrThrow() // Code uniqueness is per Location.
        assertTrue(repo.saveStorageUnit(unit(parent="20000000-0000-0000-0000-000000000003")).isFailure)
        assertTrue(repo.saveStorageUnit(current.copy(locationId=LocationId(otherLocation))).isFailure)
        assertTrue(repo.saveStorageUnit(current.copy(entityVersion=2)).isFailure)
        assertTrue(repo.saveStorageUnit(current.copy(createdBy=id())).isFailure)
        repo.saveStorageUnit(current.copy(lifecycleStatus=MasterStatus.INACTIVE)).getOrThrow()
        assertEquals("INACTIVE",read(u.storageUnitId.value).lifecycleStatus)
        assertEquals(1L,read(u.storageUnitId.value).entityVersion) // Server-owned; local edit does not increment.
        session.value=session.value.copy(authorizedOrganizations=session.value.authorizedOrganizations.map{it.copy(role="STANDARD")})
        assertEquals(1,repo.observeStorageUnits(LocationId(location)).first().size)
        assertTrue(repo.saveStorageUnit(current).isFailure)
        session.value=session.value.copy(activeOrganizationId=otherOrg,authorizedOrganizations=emptyList())
        assertTrue(repo.observeStorageUnits(LocationId(location)).first().isEmpty())
    }
    @Test fun ackReconcilesAuthoritativeMetadataAndRetainsJournal()=runBlocking {
        val u=unit();repo.saveStorageUnit(u).getOrThrow();remote.server=remote.transfer(u.storageUnitId.value,7)
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow()
        assertEquals(7L,read(u.storageUnitId.value).entityVersion);assertEquals(1000L,read(u.storageUnitId.value).createdAt)
        assertEquals(SyncOperationState.ACKNOWLEDGED,db.syncOperationDao().getOperationById(remote.requests.single().operations.single().syncOperationId)!!.state)
        assertEquals(u.storageUnitId.value,remote.requests.single().operations.single().entityId)
    }
    @Test fun retryAndLostAckFetchReuseSameKeyAndNeverDequeue()=runBlocking {
        val u=unit();repo.saveStorageUnit(u).getOrThrow();remote.server=remote.transfer(u.storageUnitId.value)
        remote.failure=StorageUnitTransportFailure("SERVICE_UNAVAILABLE",true)
        assertTrue(repo.synchronizeStorageUnits(device,"test-build").isFailure)
        val first=remote.requests.single().operations.single()
        assertEquals(SyncOperationState.RETRYABLE_ERROR,db.syncOperationDao().getOperationById(first.syncOperationId)!!.state)
        remote.failure=null
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow();assertEquals(1,remote.requests.size) // Backoff cannot be bypassed.
        makeDue(first.syncOperationId);remote.failGet=true
        assertTrue(repo.synchronizeStorageUnits(device,"test-build").isFailure)
        assertEquals(SyncOperationState.RETRYABLE_ERROR,db.syncOperationDao().getOperationById(first.syncOperationId)!!.state)
        makeDue(first.syncOperationId);remote.failGet=false;repo.synchronizeStorageUnits(device,"test-build").getOrThrow()
        assertEquals(1,remote.requests.map{it.operations.single().idempotencyKey}.distinct().size)
        assertEquals(SyncOperationState.ACKNOWLEDGED,db.syncOperationDao().getOperationById(first.syncOperationId)!!.state)
    }
    @Test fun fifoConflictsNeverRebaseLaterIntentOrOverwriteIt()=runBlocking {
        val u=unit();repo.saveStorageUnit(u).getOrThrow();remote.server=remote.transfer(u.storageUnitId.value)
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow();remote.requests.clear()
        val current=domain(u.storageUnitId.value)
        repo.saveStorageUnit(current.copy(name="First")).getOrThrow();repo.saveStorageUnit(current.copy(name="Second")).getOrThrow()
        val ops=db.storageUnitCanonicalDao().pending(org);assertTrue(ops[0].createdAt<ops[1].createdAt)
        remote.outcomes.addAll(listOf("ACKNOWLEDGED","CONFLICT"));remote.server=remote.transfer(u.storageUnitId.value,2).copy(name="First")
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow()
        assertEquals(listOf(1L,1L),remote.requests.map{it.operations.single().baseEntityVersion})
        assertEquals("Second",read(u.storageUnitId.value).name)
        assertEquals(SyncOperationState.CONFLICT,db.syncOperationDao().getOperationById(ops[1].syncOperationId)!!.state)
        db.openHelper.readableDatabase.query("SELECT * FROM sync_conflicts").use{assertEquals(1,it.count)}
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow();assertEquals(2,remote.requests.size)
    }
    @Test fun parentDependencyBlocksUntilAckAndCapturedActorCannotBeReattributed()=runBlocking {
        val parent=SyncOperationEntity(id(),id(),org,entityType="LOCATION",entityId=location,operationType="CREATE",baseEntityVersion=null,eventAt=0,payloadJson="{}",createdAt=0)
        db.syncOperationDao().insertOperation(parent)
        val u=unit();repo.saveStorageUnit(u).getOrThrow();remote.server=remote.transfer(u.storageUnitId.value)
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow();assertEquals(0,remote.requests.size)
        db.syncOperationDao().updateOperation(parent.copy(state=SyncOperationState.ACKNOWLEDGED))
        session.value=session.value.copy(userId=id())
        assertTrue(repo.synchronizeStorageUnits(device,"test-build").isFailure);assertEquals(0,remote.requests.size)
        session.value=session.value.copy(userId=actor);repo.synchronizeStorageUnits(device,"test-build").getOrThrow()
        assertEquals(listOf(parent.syncOperationId),remote.requests.single().operations.single().dependencyIds)
    }
    @Test fun permanentFailureIsDurableAndRefreshCannotOverwritePendingWork()=runBlocking {
        val u=unit();repo.saveStorageUnit(u).getOrThrow();remote.server=remote.transfer(u.storageUnitId.value,1).copy(name="Server")
        repo.refreshStorageUnits(LocationId(location)).getOrThrow();assertEquals("Unit",read(u.storageUnitId.value).name)
        remote.failure=StorageUnitTransportFailure("VALIDATION_FAILED",false)
        assertTrue(repo.synchronizeStorageUnits(device,"test-build").isFailure)
        assertEquals(SyncOperationState.PERMANENT_ERROR,db.storageUnitCanonicalDao().pending(org).single().state)
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow();assertEquals(1,remote.requests.size)
    }
    @Test fun explicitLocalParentReconciliationRequiresJournalAndFullProvenance()=runBlocking {
        val local=id();val u=unit(parent=local)
        db.organizationDao().insertOrganization(OrganizationEntity(id=1,organizationId=org,name="Legacy retained",code="LEG"))
        db.locationDao().insertLocation(LocationEntity(organizationId=1,locationId=local,canonicalOrganizationId=org,name="Captured",code="CAP",
            createdAt=10,createdBy=actor,updatedAt=20,updatedBy=actor,entityVersion=1))
        assertTrue(repo.saveStorageUnit(u).isFailure) // Complete-looking legacy columns alone confer no provenance.
        val op=SyncOperationEntity(id(),id(),org,entityType="LOCATION",entityId=local,operationType="CREATE",baseEntityVersion=null,eventAt=10,payloadJson="{}",createdAt=10)
        db.syncOperationDao().insertOperation(op)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_bridge_outbox BEFORE INSERT ON sync_operations WHEN NEW.entity_type='STORAGE_UNIT' BEGIN SELECT RAISE(ABORT, 'controlled'); END")
        assertTrue(repo.saveStorageUnit(u).isFailure)
        assertNull(db.storageUnitCanonicalDao().parent(org,local));assertNull(db.storageUnitCanonicalDao().get(u.storageUnitId.value))
        // The positive path is covered in the following independent test, without dropping a failure trigger.
        assertEquals(1,db.syncOperationDao().getPendingOperationsList().size)
    }
    @Test fun capturedParentIsReconciledLosslesslyWithChildAndDependency()=runBlocking {
        val local=id();val u=unit(parent=local)
        db.organizationDao().insertOrganization(OrganizationEntity(id=1,organizationId=org,name="Retained",code="LEG"))
        db.locationDao().insertLocation(LocationEntity(organizationId=1,locationId=local,canonicalOrganizationId=org,name="Captured",code="CAP",
            country="RO",createdAt=10,createdBy=actor,updatedAt=20,updatedBy=actor,entityVersion=1))
        val op=SyncOperationEntity(id(),id(),org,entityType="LOCATION",entityId=local,operationType="CREATE",baseEntityVersion=null,eventAt=10,payloadJson="{}",createdAt=10)
        db.syncOperationDao().insertOperation(op);repo.saveStorageUnit(u).getOrThrow()
        val parent=db.storageUnitCanonicalDao().parent(org,local)!!
        assertEquals(10L,parent.createdAt);assertEquals(20L,parent.updatedAt);assertEquals(actor,parent.createdBy);assertEquals("RO",parent.country)
        val child=db.storageUnitCanonicalDao().pending(org).single()
        assertEquals(op.syncOperationId,db.syncDependencyConflictDao().getDependencies(child.syncOperationId).single().parentOperationId)
        assertNotNull(db.locationDao().getLocationByLocationId(local)) // Retained projection never removed.
    }
    @Test fun authorizationChangeDiscardsSuspendedRemoteResponse()=runBlocking {
        remote.server=remote.transfer(id())
        remote.onList={session.value=session.value.copy(token=null)}
        assertTrue(repo.refreshStorageUnits(LocationId(location)).isFailure)
        assertNull(db.storageUnitCanonicalDao().get(remote.server!!.id))
    }
    @Test fun newerRemoteVersionOverDirtyEntityCreatesConflictWithoutOverwrite()=runBlocking {
        val u=unit();repo.saveStorageUnit(u).getOrThrow()
        remote.server=remote.transfer(u.storageUnitId.value,2).copy(name="Changed remotely")
        repo.refreshStorageUnits(LocationId(location)).getOrThrow()
        assertEquals("Unit",read(u.storageUnitId.value).name)
        assertEquals(SyncOperationState.CONFLICT,db.storageUnitCanonicalDao().pending(org).single().state)
        db.openHelper.readableDatabase.query("SELECT server_entity_version,server_value_json FROM sync_conflicts").use {
            assertTrue(it.moveToFirst());assertEquals(2L,it.getLong(0));assertTrue(it.getString(1).contains("Changed remotely"))
        }
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow();assertEquals(0,remote.requests.size)
    }
    @Test fun interruptedInFlightRecoveryReplaysSameIdentity()=runBlocking {
        val u=unit();repo.saveStorageUnit(u).getOrThrow();remote.server=remote.transfer(u.storageUnitId.value)
        val op=db.storageUnitCanonicalDao().pending(org).single()
        db.syncOperationDao().updateOperation(op.copy(state=SyncOperationState.IN_FLIGHT,attemptCount=1))
        repo.synchronizeStorageUnits(device,"test-build").getOrThrow()
        assertEquals(op.idempotencyKey,remote.requests.single().operations.single().idempotencyKey)
        val ack=db.syncOperationDao().getOperationById(op.syncOperationId)!!
        assertEquals(SyncOperationState.ACKNOWLEDGED,ack.state);assertEquals(2,ack.attemptCount)
    }
    private inner class FakeRemote:StorageUnitRemoteSource {
        var onList:(()->Unit)?=null
        var calls=0;var server:StorageUnitTransfer?=null;var failure:Exception?=null;var failGet=false
        val requests=mutableListOf<StorageUnitSyncBatchDto>();val outcomes=java.util.ArrayDeque<String>()
        fun transfer(id:String,version:Long=1)=StorageUnitTransfer(id,org,location,"Unit","U",null,"ACTIVE",
            "1970-01-01T00:00:01Z",actor,"1970-01-01T00:00:02Z",actor,version)
        override suspend fun list(org:String,location:String)=listOfNotNull(server).also{calls++;onList?.invoke()}
        override suspend fun parent(org:String,id:String)=LocationResponseDto(id,org,"Location","L",lifecycleStatus="ACTIVE",
            createdAt="1970-01-01T00:00:00Z",createdBy=actor,updatedAt="1970-01-01T00:00:00Z",updatedBy=actor,entityVersion=1).also{calls++}
        override suspend fun organization(org:String)=OrganizationResponseDto(org,"Fixture","FIX",lifecycleStatus="ACTIVE",
            createdAt="1970-01-01T00:00:00Z",createdBy=actor,updatedAt="1970-01-01T00:00:00Z",updatedBy=actor,entityVersion=1).also{calls++}
        override suspend fun get(org:String,id:String):StorageUnitTransfer {calls++;if(failGet)throw java.io.IOException("controlled");return checkNotNull(server)}
        override suspend fun push(request:StorageUnitSyncBatchDto):StorageUnitSyncBatchResultDto {
            calls++;requests+=request
            assertEquals(SyncOperationState.IN_FLIGHT,db.syncOperationDao().getOperationById(request.operations.single().syncOperationId)!!.state)
            failure?.let{throw it}
            return StorageUnitSyncBatchResultDto(listOf(StorageUnitSyncResultDto(request.operations.single().syncOperationId,
                outcomes.poll() ?: "ACKNOWLEDGED",server?.entityVersion,conflict=buildJsonObject{put("name","Server")})),"1970-01-01T00:00:02Z","fixture")
        }
    }
}
