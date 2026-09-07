package com.silomath.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.database.dao.*
import com.silomath.core.database.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.database.sqlite.SQLiteConstraintException

@RunWith(AndroidJUnit4::class)
class SiloMathDbTest {
    private lateinit var db: SiloMathDatabase
    private lateinit var organizationDao: OrganizationDao
    private lateinit var locationDao: LocationDao
    private lateinit var storageDao: StorageDao
    private lateinit var campaignDao: CampaignDao
    private lateinit var inventoryDao: InventoryDao
    private lateinit var geometryDao: GeometryDao
    private lateinit var stocktakingDao: StocktakingDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SiloMathDatabase::class.java).build()
        organizationDao = db.organizationDao()
        locationDao = db.locationDao()
        storageDao = db.storageDao()
        campaignDao = db.campaignDao()
        inventoryDao = db.inventoryDao()
        geometryDao = db.geometryDao()
        stocktakingDao = db.stocktakingDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private suspend fun createBaseFixture() {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org"))
        locationDao.insertLocation(LocationEntity(id = 1, organizationId = 1, name = "Loc"))
        storageDao.insertStorageUnit(StorageUnitEntity(id = 1, locationId = 1, name = "Unit"))
        geometryDao.insertFamily(GeometryFamilyEntity(id = 1, name = "Cylinder"))
        geometryDao.insertProfile(GeometryProfileEntity(id = 1, name = "Standard Silo", geometryFamilyId = 1))
        geometryDao.insertVersion(GeometryVersionEntity(id = 1, geometryProfileId = 1, versionNumber = 1, dimensionsJson = "{}"))
        campaignDao.insertCampaign(CampaignEntity(id = 1, organizationId = 1, name = "Camp", status = "Active"))
    }

    @Test
    fun testStorageSpaceUniqueConstraint() = runBlocking {
        createBaseFixture()
        val space = StorageSpaceEntity(id = 1, storageUnitId = 1, code = "C01", geometryProfileId = 1)
        storageDao.insertStorageSpace(space)
        
        try {
            storageDao.insertStorageSpace(space.copy(id = 2)) // Same unit_id + code
            fail("Expected SQLiteConstraintException for duplicate (unit_id, code)")
        } catch (e: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testStocktakingRecordUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, storageUnitId = 1, code = "C01", geometryProfileId = 1))
        
        val record = StocktakingRecordEntity(
            id = 1, 
            campaignId = 1, 
            storageSpaceId = 1, 
            status = "To Do", 
            ownership = "Owned",
            commodityId = null,
            density = null,
            clientId = null
        )
        stocktakingDao.insertRecord(record)
        
        try {
            stocktakingDao.insertRecord(record.copy(id = 2)) // Same campaign_id + storage_space_id
            fail("Expected SQLiteConstraintException for duplicate (campaign_id, storage_space_id)")
        } catch (e: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testCampaignScopeUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, storageUnitId = 1, code = "C01", geometryProfileId = 1))
        
        val scope = CampaignScopeEntity(id = 1, campaignId = 1, storageSpaceId = 1, geometryVersionId = 1)
        campaignDao.insertScope(scope)
        
        try {
            campaignDao.insertScope(scope.copy(id = 2)) // Same campaign_id + storage_space_id
            fail("Expected SQLiteConstraintException for duplicate (campaign_id, storage_space_id)")
        } catch (e: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testCampaignAssignmentUniqueConstraint() = runBlocking {
        createBaseFixture()
        
        val assignment = CampaignAssignmentEntity(id = 1, campaignId = 1, userId = "user1", roleAtAssignment = "Standard")
        campaignDao.insertAssignment(assignment)
        
        try {
            campaignDao.insertAssignment(assignment.copy(id = 2)) // Same campaign_id + user_id
            fail("Expected SQLiteConstraintException for duplicate (campaign_id, user_id)")
        } catch (e: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testInventorySnapshotUniqueConstraint() = runBlocking {
        createBaseFixture()
        
        val snapshot = InventorySnapshotEntity(id = 1, campaignId = 1, metadataJson = "{}")
        inventoryDao.insertSnapshot(snapshot)
        
        try {
            inventoryDao.insertSnapshot(snapshot.copy(id = 2)) // Same campaign_id
            fail("Expected SQLiteConstraintException for duplicate campaign_id snapshot")
        } catch (e: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testMeasurementLimitRule() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, storageUnitId = 1, code = "C01", geometryProfileId = 1))
        val recordId = stocktakingDao.insertRecord(StocktakingRecordEntity(
            id = 1, campaignId = 1, storageSpaceId = 1, status = "To Do", ownership = "Owned", commodityId = null, density = null, clientId = null
        ))

        // Add 5 measurements
        for (i in 1..5) {
            stocktakingDao.addMeasurementTransactional(recordId, i.toDouble())
        }
        assertEquals(5, stocktakingDao.getMeasurementCount(recordId))

        // 6th measurement should fail
        try {
            stocktakingDao.addMeasurementTransactional(recordId, 6.0)
            fail("Expected IllegalArgumentException for 6th measurement")
        } catch (e: IllegalArgumentException) {
            assertEquals("Maximum 5 measurements per record exceeded", e.message)
        }

        // Verify existing 5 remain
        assertEquals(5, stocktakingDao.getMeasurementCount(recordId))
    }
}
