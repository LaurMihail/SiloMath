package com.silomath.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.database.*
import com.silomath.core.database.entity.*
import com.silomath.core.domain.model.*
import com.silomath.core.model.*
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Explicit in-memory test configuration. Production encrypted Room factory is unchanged. */
@RunWith(AndroidJUnit4::class)
class LocationUiPersistenceTest {
    private lateinit var db:SiloMathDatabase
    private lateinit var repo:OfflineLocationRepository
    private val org=OrganizationId("00000000-0000-0000-0000-000000000001")
    private val id=LocationId("10000000-0000-0000-0000-000000000001")
    private fun row(country:String?="RO")=Location(id,org,"North","N",null,null,country,null,MasterStatus.ACTIVE,
        Instant.EPOCH,"fixture",Instant.EPOCH,"fixture",1)
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),SiloMathDatabase::class.java).build()
        db.organizationDao().insertOrganization(OrganizationEntity(id=1,organizationId=org.value,name="Fixture",code="FIX"))
        repo=OfflineLocationRepository(db.locationDao(),db.organizationDao(),AtomicMutationRunner(db))
    }
    @After fun close(){db.close()}
    @Test fun localSaveCommitsCanonicalReadModelAndOutboxTogether()=runBlocking {
        assertTrue(repo.saveLocation(row()).isSuccess)
        assertEquals(listOf(row()),repo.observeLocations(org).first())
        val op=db.syncOperationDao().getPendingOperationsList().single()
        assertEquals("CREATE",op.operationType);assertNull(op.baseEntityVersion)
        assertEquals(id.value,op.entityId);assertEquals(org.value,op.organizationId)
        assertTrue(op.payloadJson.contains("\"country\":\"RO\""))
        assertFalse(op.payloadJson.contains("createdAt")) // Server-authoritative timestamps are not fabricated in the request.
    }
    @Test fun outboxFailureRollsBackBusinessRow()=runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_fixture_outbox BEFORE INSERT ON sync_operations BEGIN SELECT RAISE(ABORT, 'fixture failure'); END")
        assertTrue(repo.saveLocation(row()).isFailure)
        assertNull(db.locationDao().getLocationByLocationId(id.value))
        assertTrue(db.syncOperationDao().getPendingOperationsList().isEmpty())
    }
    @Test fun countryNewValuesFailClosedButHistoricalValueRemainsVerbatim()=runBlocking {
        assertTrue(repo.saveLocation(row("Romania")).isFailure)
        assertTrue(db.syncOperationDao().getPendingOperationsList().isEmpty())
        db.locationDao().insertLocation(LocationEntity(organizationId=1,locationId=id.value,canonicalOrganizationId=org.value,
            name="North",code="N",country="Romania",createdAt=0,createdBy="fixture",updatedAt=0,updatedBy="fixture",entityVersion=1))
        assertTrue(repo.saveLocation(row("Romania").copy(name="Renamed")).isSuccess)
        assertEquals("Romania",db.locationDao().getLocationByLocationId(id.value)!!.country)
        assertTrue(repo.saveLocation(row("ro")).isFailure)
    }
    @Test fun canonicalReadNeverFallsBackToInsufficientLegacyRowsAndCodeUniquenessIsCaseInsensitive()=runBlocking {
        db.locationDao().insertLocation(LocationEntity(organizationId=1,name="Legacy",code="LEG"))
        assertTrue(repo.observeLocations(org).first().isEmpty())
        assertTrue(repo.saveLocation(row()).isSuccess)
        val duplicate=row().copy(locationId=LocationId("10000000-0000-0000-0000-000000000002"))
        assertTrue(repo.saveLocation(duplicate).isFailure)
        assertEquals(1,db.syncOperationDao().getPendingOperationsList().size)
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use{assertEquals(0,it.count)}
    }
    @Test fun localHistoricalReferenceProtectsCodeWithoutRewritingHistory()=runBlocking {
        assertTrue(repo.saveLocation(row()).isSuccess)
        db.storageDao().insertStorageUnit(StorageUnitEntity(id=1,locationId=1,code="U",name="Fixture Unit"))
        db.geometryDao().insertFamily(GeometryFamilyEntity(id=1,name="Fixture"))
        db.geometryDao().insertProfile(GeometryProfileEntity(id=1,name="Fixture",geometryFamilyId=1,profileCode="FIX"))
        db.storageDao().insertStorageSpace(StorageSpaceEntity(id=1,storageUnitId=1,code="S",geometryProfileId=1))
        db.campaignDao().insertCampaign(CampaignEntity(id=1,organizationId=1,name="Fixture",status="Active"))
        db.campaignDao().insertScope(CampaignScopeEntity(campaignId=1,storageSpaceId=1))
        assertTrue(repo.hasHistoricalReferences(org,id).getOrThrow())
        assertTrue(repo.saveLocation(row().copy(code="CHANGED")).isFailure)
        assertEquals("N",db.locationDao().getLocationByLocationId(id.value)!!.code)
        assertEquals(1,db.syncOperationDao().getPendingOperationsList().size)
    }
}
