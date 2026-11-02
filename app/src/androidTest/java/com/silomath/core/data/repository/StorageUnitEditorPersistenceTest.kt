package com.silomath.core.data.repository

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.entity.canonical.*
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.*
import com.silomath.core.data.network.source.StorageUnitRemoteSource
import com.silomath.core.data.network.dto.StorageUnitSyncBatchDto
import com.silomath.core.model.*
import com.silomath.feature.masterdata.storageunit.*
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Real MD-004 ViewModel -> MD-003 Room business/outbox transaction. No network/mocks in Production. */
@RunWith(AndroidJUnit4::class)
class StorageUnitEditorPersistenceTest {
    private lateinit var db:SiloMathDatabase
    private lateinit var repo:OfflineStorageUnitRepository
    private lateinit var auth:AuthRepository
    private lateinit var locations:LocationRepository
    private fun id(n:Int)="20000000-0000-0000-0000-"+n.toString().padStart(12,'0')
    private val org=OrganizationId(id(1));private val loc=LocationId(id(2));private val actor=id(9)
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),SiloMathDatabase::class.java).build()
        val session=MutableStateFlow(SessionState(userId=actor,token="test-only",activeOrganizationId=org.value,
            authorizedOrganizations=listOf(AuthorizedOrganizationContext(id(8),org.value,"Fixture","FIX","ADMIN"))))
        auth=object:AuthRepository {
            override val sessionState:Flow<SessionState> get()=session
            override suspend fun signIn(email:String,password:String)=error("Unused")
            override suspend fun signOut()=error("Unused")
            override suspend fun resetPassword(email:String)=error("Unused")
            override suspend fun updatePassword(recoveryContext:String,newPassword:String)=error("Unused")
            override suspend fun setActiveOrganization(organizationId:String)=error("Unused")
            override suspend fun restoreRemoteSession()=error("Unused")
        }
        locations=object:LocationRepository {
            override fun observeLocations(organizationId:OrganizationId)=flowOf(listOf(Location(loc,org,"Fixture","L",null,null,null,null,MasterStatus.ACTIVE,Instant.EPOCH,actor,Instant.EPOCH,actor,1)))
            override suspend fun hasHistoricalReferences(organizationId:OrganizationId,locationId:LocationId)=error("Unused")
            override suspend fun saveLocation(location:Location)=error("Unused")
        }
        val remote=object:StorageUnitRemoteSource {
            override suspend fun list(org:String,location:String)=error("No network permitted")
            override suspend fun parent(org:String,id:String)=error("No network permitted")
            override suspend fun organization(org:String)=error("No network permitted")
            override suspend fun get(org:String,id:String)=error("No network permitted")
            override suspend fun push(request:StorageUnitSyncBatchDto)=error("No network permitted")
        }
        repo=OfflineStorageUnitRepository(db,auth,remote)
        db.storageUnitCanonicalDao().insertOrganization(V6OrganizationsRow(org.value,"Fixture","FIX",lifecycleStatus="ACTIVE",createdAt=0,createdBy=actor,updatedAt=0,updatedBy=actor,entityVersion=1))
        db.storageUnitCanonicalDao().insertParent(V6LocationsRow(loc.value,org.value,"Fixture","L",lifecycleStatus="ACTIVE",createdAt=0,createdBy=actor,updatedAt=0,updatedBy=actor,entityVersion=1))
    }
    @After fun close(){db.close()}
    private suspend fun editor():StorageUnitEditorViewModel {
        val vm=withContext(Dispatchers.Main){StorageUnitEditorViewModel(auth,locations,repo,SavedStateHandle()).also{it.bind(org,loc,null)}}
        withTimeout(5000){vm.uiState.first{it.allowed}}
        return vm
    }
    @Test fun savedSignalFollowsAtomicBusinessAndOutboxCommit()=runBlocking {
        val vm=editor()
        val event=async{vm.savedEvents.first()}
        withContext(Dispatchers.Main){vm.update(StorageUnitDraft(" New "," new "));vm.save()}
        withTimeout(5000){event.await()}
        val op=db.syncOperationDao().getPendingOperationsList().single()
        val row=db.storageUnitCanonicalDao().get(op.entityId)!!
        assertEquals("New",row.name);assertEquals("NEW",row.code);assertEquals(loc.value,row.locationId);assertEquals(org.value,row.organizationId)
        assertNull(op.baseEntityVersion);assertEquals("CREATE",op.operationType);assertEquals(1L,row.entityVersion)
        assertEquals(actor,row.createdBy);assertTrue(op.payloadJson.contains("\"locationId\":\"${loc.value}\""))
    }
    @Test fun outboxFailureRetainsDraftAndRollsBackBusinessWithoutSuccessSignal()=runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_editor_outbox BEFORE INSERT ON sync_operations BEGIN SELECT RAISE(ABORT, 'controlled'); END")
        val vm=editor();val event=async{vm.savedEvents.first()}
        withContext(Dispatchers.Main){vm.update(StorageUnitDraft("Keep draft","KEEP"));vm.save()}
        withTimeout(5000){vm.uiState.first{it.error!=null && !it.saving}}
        assertEquals("Keep draft",vm.uiState.value.draft.name);assertFalse(event.isCompleted);event.cancelAndJoin()
        assertTrue(db.syncOperationDao().getPendingOperationsList().isEmpty())
        assertTrue(repo.observeStorageUnits(loc).first().isEmpty())
    }
}
