package com.silomath.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silomath.core.database.dao.*
import com.silomath.core.database.di.MIGRATION_7_8
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
    private lateinit var commodityDao: CommodityDao
    private lateinit var clientDao: ClientDao

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
        commodityDao = db.commodityDao()
        clientDao = db.clientDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private suspend fun createBaseFixture() {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org"))
        locationDao.insertLocation(LocationEntity(id = 1, organizationId = 1, code = "LOC1", name = "Loc"))
        storageDao.insertStorageUnit(StorageUnitEntity(id = 1, locationId = 1, code = "UNIT1", name = "Unit"))
        geometryDao.insertFamily(GeometryFamilyEntity(id = 1, name = "Cylinder"))
        geometryDao.insertProfile(GeometryProfileEntity(id = 1, geometryFamilyId = 1, profileCode = "PROF1", name = "Standard Silo"))
        geometryDao.insertVersion(GeometryVersionEntity(
            id = 1,
            geometryProfileId = 1,
            versionNumber = 1,
            fixedParameters = "{}",
            requiredDimensionsSchema = "{}",
            measurementSchema = "{}",
            supportedUnits = "{}",
            calculationMethod = "test",
            validationConstraints = "{}"
        ))
        campaignDao.insertCampaign(CampaignEntity(id = 1, organizationId = 1, name = "Camp", status = "Active"))
    }

    @Test
    fun testStorageSpaceUniqueConstraint() = runBlocking {
        createBaseFixture()
        val space = StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}")
        storageDao.insertStorageSpace(space)
        
        try {
            storageDao.insertStorageSpace(space.copy(id = 2)) // Same unit_id + code
            fail("Expected SQLiteConstraintException for duplicate (unit_id, code)")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testStocktakingRecordUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))
        
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
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testCampaignScopeUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))
        
        val scope = CampaignScopeEntity(id = 1, campaignId = 1, storageSpaceId = 1, geometryVersionId = 1)
        campaignDao.insertScope(scope)
        
        try {
            campaignDao.insertScope(scope.copy(id = 2)) // Same campaign_id + storage_space_id
            fail("Expected SQLiteConstraintException for duplicate (campaign_id, storage_space_id)")
        } catch (_: SQLiteConstraintException) {
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
        } catch (_: SQLiteConstraintException) {
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
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testMeasurementLimitRule() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))
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

    @Test
    fun testExistingRowPreservesLocalIdOnUpdate() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        val localId = locationDao.insertLocation(LocationEntity(id = 0, organizationId = 1, remoteId = "loc-1", code = "L1", name = "Old Name"))
        
        val existing = locationDao.getLocationByRemoteId("loc-1")!!
        assertEquals(localId, existing.id)
        
        val updated = existing.copy(name = "New Name", remoteRevision = 2L)
        locationDao.updateLocation(updated)
        
        val reloaded = locationDao.getLocationByRemoteId("loc-1")!!
        assertEquals(localId, reloaded.id)
        assertEquals("New Name", reloaded.name)
        assertEquals(2L, reloaded.remoteRevision)
    }

    @Test
    fun testInactiveLifecycleRetained() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        val localId = locationDao.insertLocation(LocationEntity(id = 0, organizationId = 1, remoteId = "loc-1", code = "L1", name = "Loc", lifecycleStatus = "ACTIVE"))
        
        val existing = locationDao.getLocationByRemoteId("loc-1")!!
        assertEquals("ACTIVE", existing.lifecycleStatus)
        
        locationDao.updateLocation(existing.copy(lifecycleStatus = "INACTIVE", remoteRevision = 2L))
        
        val reloaded = locationDao.getLocationByRemoteId("loc-1")!!
        assertEquals(localId, reloaded.id)
        assertEquals("INACTIVE", reloaded.lifecycleStatus)
    }

    @Test
    fun testParentIntegrityFailureForeignConstraint() = runBlocking {
        try {
            storageDao.insertStorageUnit(StorageUnitEntity(id = 0, locationId = 999L, remoteId = "unit-1", code = "U1"))
            fail("Expected SQLiteConstraintException for foreign key violation")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testRemoteAbsenceDoesNotDeleteLocalRow() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        val localId = locationDao.insertLocation(LocationEntity(id = 0, organizationId = 1, remoteId = "loc-1", code = "L1", name = "Loc"))
        
        val existing = locationDao.getLocationByRemoteId("loc-1")
        assertNotNull(existing)
        assertEquals(localId, existing!!.id)
        
        val reloaded = locationDao.getLocationByRemoteId("loc-1")
        assertNotNull(reloaded)
        assertEquals("Loc", reloaded!!.name)
    }

    @Test
    fun testCommodityExistingRowPreservesLocalIdOnUpdate() = runBlocking {
        val localId = commodityDao.insertCommodity(
            CommodityEntity(id = 0, remoteId = "com-1", code = "WHT", name = "Wheat", densityUnit = "kg/m3", lifecycleStatus = "ACTIVE", remoteRevision = 1L)
        )
        val existing = commodityDao.getCommodityByRemoteId("com-1")!!
        assertEquals(localId, existing.id)

        val updated = existing.copy(name = "Premium Wheat", lifecycleStatus = "ACTIVE", remoteRevision = 2L)
        commodityDao.updateCommodity(updated)

        val reloaded = commodityDao.getCommodityByRemoteId("com-1")!!
        assertEquals(localId, reloaded.id)
        assertEquals("Premium Wheat", reloaded.name)
        assertEquals(2L, reloaded.remoteRevision)
    }

    @Test
    fun testCommodityMissingRowInserts() = runBlocking {
        assertNull(commodityDao.getCommodityByRemoteId("com-new"))
        val localId = commodityDao.insertCommodity(
            CommodityEntity(id = 0, remoteId = "com-new", code = "CRN", name = "Corn", densityUnit = "kg/m3")
        )
        val inserted = commodityDao.getCommodityByRemoteId("com-new")
        assertNotNull(inserted)
        assertEquals(localId, inserted!!.id)
        assertEquals("Corn", inserted.name)
    }

    @Test
    fun testCommodityInactiveLifecycleUpdated() = runBlocking {
        val localId = commodityDao.insertCommodity(
            CommodityEntity(id = 0, remoteId = "com-1", code = "WHT", name = "Wheat", densityUnit = "kg/m3", lifecycleStatus = "ACTIVE")
        )
        val existing = commodityDao.getCommodityByRemoteId("com-1")!!
        commodityDao.updateCommodity(existing.copy(lifecycleStatus = "INACTIVE", remoteRevision = 3L))

        val reloaded = commodityDao.getCommodityByRemoteId("com-1")!!
        assertEquals(localId, reloaded.id)
        assertEquals("INACTIVE", reloaded.lifecycleStatus)
        assertEquals(3L, reloaded.remoteRevision)
    }

    @Test
    fun testCommodityRemoteAbsenceDoesNotDelete() = runBlocking {
        val localId = commodityDao.insertCommodity(
            CommodityEntity(id = 0, remoteId = "com-1", code = "WHT", name = "Wheat", densityUnit = "kg/m3")
        )
        val existing = commodityDao.getCommodityByRemoteId("com-1")
        assertNotNull(existing)

        val reloaded = commodityDao.getCommodityByRemoteId("com-1")
        assertNotNull(reloaded)
        assertEquals(localId, reloaded!!.id)
    }

    @Test
    fun testClientExistingRowPreservesLocalIdOnUpdate() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        val localId = clientDao.insertClient(
            ClientEntity(id = 0, organizationId = 1, remoteId = "cli-1", name = "Client Old", lifecycleStatus = "ACTIVE", remoteRevision = 1L)
        )
        val existing = clientDao.getClientByRemoteId("cli-1")!!
        assertEquals(localId, existing.id)

        val updated = existing.copy(name = "Client New", remoteRevision = 2L)
        clientDao.updateClient(updated)

        val reloaded = clientDao.getClientByRemoteId("cli-1")!!
        assertEquals(localId, reloaded.id)
        assertEquals("Client New", reloaded.name)
        assertEquals(2L, reloaded.remoteRevision)
    }

    @Test
    fun testClientMissingRowInserts() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        assertNull(clientDao.getClientByRemoteId("cli-new"))

        val localId = clientDao.insertClient(
            ClientEntity(id = 0, organizationId = 1, remoteId = "cli-new", name = "Agro Client")
        )
        val inserted = clientDao.getClientByRemoteId("cli-new")
        assertNotNull(inserted)
        assertEquals(localId, inserted!!.id)
        assertEquals("Agro Client", inserted.name)
    }

    @Test
    fun testClientInactiveLifecycleUpdated() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        val localId = clientDao.insertClient(
            ClientEntity(id = 0, organizationId = 1, remoteId = "cli-1", name = "Client", lifecycleStatus = "ACTIVE")
        )
        val existing = clientDao.getClientByRemoteId("cli-1")!!
        clientDao.updateClient(existing.copy(lifecycleStatus = "INACTIVE", remoteRevision = 4L))

        val reloaded = clientDao.getClientByRemoteId("cli-1")!!
        assertEquals(localId, reloaded.id)
        assertEquals("INACTIVE", reloaded.lifecycleStatus)
        assertEquals(4L, reloaded.remoteRevision)
    }

    @Test
    fun testClientRemoteAbsenceDoesNotDelete() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        val localId = clientDao.insertClient(
            ClientEntity(id = 0, organizationId = 1, remoteId = "cli-1", name = "Client")
        )
        val existing = clientDao.getClientByRemoteId("cli-1")
        assertNotNull(existing)

        val reloaded = clientDao.getClientByRemoteId("cli-1")
        assertNotNull(reloaded)
        assertEquals(localId, reloaded!!.id)
    }

    @Test
    fun testClientParentOrganizationConstraintFailure() = runBlocking {
        try {
            clientDao.insertClient(
                ClientEntity(id = 0, organizationId = 9999L, remoteId = "cli-err", name = "Client Orphan")
            )
            fail("Expected SQLiteConstraintException for non-existent organization_id")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testCampaignRemoteIdUniqueConstraint() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        campaignDao.insertCampaign(CampaignEntity(id = 1, organizationId = 1, name = "Camp 1", status = "Active", remoteId = "camp-remote-1"))

        try {
            campaignDao.insertCampaign(CampaignEntity(id = 2, organizationId = 1, name = "Camp 2", status = "Active", remoteId = "camp-remote-1"))
            fail("Expected SQLiteConstraintException for duplicate campaign remoteId")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testCampaignScopeRemoteIdUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 2, code = "C02", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))

        campaignDao.insertScope(CampaignScopeEntity(id = 1, campaignId = 1, storageSpaceId = 1, geometryVersionId = 1, remoteId = "scope-remote-1"))

        try {
            campaignDao.insertScope(CampaignScopeEntity(id = 2, campaignId = 1, storageSpaceId = 2, geometryVersionId = 1, remoteId = "scope-remote-1"))
            fail("Expected SQLiteConstraintException for duplicate campaign_scope remoteId")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testCampaignAssignmentRemoteIdUniqueConstraint() = runBlocking {
        createBaseFixture()
        campaignDao.insertAssignment(CampaignAssignmentEntity(id = 1, campaignId = 1, userId = "user1", roleAtAssignment = "Admin", remoteId = "assign-remote-1"))

        try {
            campaignDao.insertAssignment(CampaignAssignmentEntity(id = 2, campaignId = 1, userId = "user2", roleAtAssignment = "Standard User", remoteId = "assign-remote-1"))
            fail("Expected SQLiteConstraintException for duplicate campaign_assignment remoteId")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testExistingCampaignRowPreservedThroughSchemaContract() = runBlocking {
        organizationDao.insertOrganization(OrganizationEntity(id = 1, name = "Org", organizationId = "123e4567-e89b-12d3-a456-426614174000"))
        val localId = campaignDao.insertCampaign(CampaignEntity(id = 0, organizationId = 1, name = "Camp 1", status = "Active", remoteId = "camp-remote-1", remoteRevision = 1L))

        val existing = campaignDao.getCampaignByRemoteId("camp-remote-1")!!
        assertEquals(localId, existing.id)

        val updated = existing.copy(name = "Updated Camp 1", remoteRevision = 2L)
        campaignDao.updateCampaign(updated)

        val reloaded = campaignDao.getCampaignByRemoteId("camp-remote-1")!!
        assertEquals(localId, reloaded.id)
        assertEquals("Updated Camp 1", reloaded.name)
        assertEquals(2L, reloaded.remoteRevision)
    }

    @Test
    fun testStocktakingRecordRemoteIdUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 2, code = "C02", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))

        stocktakingDao.insertRecord(StocktakingRecordEntity(id = 1, campaignId = 1, storageSpaceId = 1, commodityId = null, density = null, ownership = "Owned", clientId = null, status = "To Do", remoteId = "sr-remote-1"))

        try {
            stocktakingDao.insertRecord(StocktakingRecordEntity(id = 2, campaignId = 1, storageSpaceId = 2, commodityId = null, density = null, ownership = "Owned", clientId = null, status = "To Do", remoteId = "sr-remote-1"))
            fail("Expected SQLiteConstraintException for duplicate stocktaking_record remoteId")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testMeasurementRemoteIdUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))
        val recordId = stocktakingDao.insertRecord(StocktakingRecordEntity(id = 1, campaignId = 1, storageSpaceId = 1, commodityId = null, density = null, ownership = "Owned", clientId = null, status = "To Do"))

        stocktakingDao.insertMeasurement(MeasurementEntity(id = 1, stocktakingRecordId = recordId, value = 10.0, remoteId = "m-remote-1"))

        try {
            stocktakingDao.insertMeasurement(MeasurementEntity(id = 2, stocktakingRecordId = recordId, value = 12.0, remoteId = "m-remote-1"))
            fail("Expected SQLiteConstraintException for duplicate measurement remoteId")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testCalculationResultRemoteIdUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 2, code = "C02", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))

        val rec1 = stocktakingDao.insertRecord(StocktakingRecordEntity(id = 1, campaignId = 1, storageSpaceId = 1, commodityId = null, density = null, ownership = "Owned", clientId = null, status = "Validated"))
        val rec2 = stocktakingDao.insertRecord(StocktakingRecordEntity(id = 2, campaignId = 1, storageSpaceId = 2, commodityId = null, density = null, ownership = "Owned", clientId = null, status = "Validated"))

        stocktakingDao.insertCalculationResult(CalculationResultEntity(id = 1, stocktakingRecordId = rec1, volume = 100.0, mass = 80.0, engineVersion = "v1", metadataJson = "{}", remoteId = "calc-remote-1"))

        try {
            stocktakingDao.insertCalculationResult(CalculationResultEntity(id = 2, stocktakingRecordId = rec2, volume = 200.0, mass = 160.0, engineVersion = "v1", metadataJson = "{}", remoteId = "calc-remote-1"))
            fail("Expected SQLiteConstraintException for duplicate calculation_result remoteId")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testIssueRemoteIdUniqueConstraint() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))
        val rec1 = stocktakingDao.insertRecord(StocktakingRecordEntity(id = 1, campaignId = 1, storageSpaceId = 1, commodityId = null, density = null, ownership = "Owned", clientId = null, status = "Issues"))

        stocktakingDao.insertIssue(IssueEntity(id = 1, stocktakingRecordId = rec1, description = "Issue 1", remoteId = "iss-remote-1"))

        try {
            stocktakingDao.insertIssue(IssueEntity(id = 2, stocktakingRecordId = rec1, description = "Issue 2", remoteId = "iss-remote-1"))
            fail("Expected SQLiteConstraintException for duplicate issue remoteId")
        } catch (_: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun testExistingStocktakingRecordPreservesLocalIdOnUpdate() = runBlocking {
        createBaseFixture()
        storageDao.insertStorageSpace(StorageSpaceEntity(id = 1, code = "C01", storageUnitId = 1, geometryProfileId = 1, geometryVersionId = 1, configuredDimensions = "{}"))

        val localId = stocktakingDao.insertRecord(StocktakingRecordEntity(id = 0, campaignId = 1, storageSpaceId = 1, commodityId = null, density = null, ownership = "Owned", clientId = null, status = "To Do", remoteId = "sr-remote-1", remoteRevision = 1L))

        val existing = stocktakingDao.getRecordByRemoteId("sr-remote-1")!!
        assertEquals(localId, existing.id)

        val updated = existing.copy(status = "Validated", remoteRevision = 2L)
        stocktakingDao.updateRecord(updated)

        val reloaded = stocktakingDao.getRecordByRemoteId("sr-remote-1")!!
        assertEquals(localId, reloaded.id)
        assertEquals("Validated", reloaded.status)
        assertEquals(2L, reloaded.remoteRevision)
    }

    @Test
    fun testMigration7To8LosslessPreservation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("mig_7_8_test.db")
        if (dbFile.exists()) dbFile.delete()

        val sqliteDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqliteDb.execSQL("PRAGMA user_version = 7")
        assertEquals(7, sqliteDb.version)

        sqliteDb.execSQL("CREATE TABLE inventory_snapshots (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, campaign_id INTEGER NOT NULL, captured_at INTEGER NOT NULL, metadata_json TEXT NOT NULL)")
        sqliteDb.execSQL("CREATE TABLE geometry_versions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, geometry_profile_id INTEGER NOT NULL, version_number INTEGER NOT NULL, fixed_parameters TEXT NOT NULL, required_dimensions_schema TEXT NOT NULL, measurement_schema TEXT NOT NULL, supported_units TEXT NOT NULL, calculation_method TEXT NOT NULL, validation_constraints TEXT NOT NULL, capacity_rules TEXT)")
        sqliteDb.execSQL("CREATE TABLE inventory_snapshot_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, snapshot_id INTEGER NOT NULL, storage_space_code TEXT NOT NULL, captured_geometry_version_id INTEGER NOT NULL, commodity_name TEXT, density REAL, ownership TEXT NOT NULL, client_name TEXT, final_volume REAL NOT NULL, final_mass REAL NOT NULL, engine_version TEXT NOT NULL, metadata_json TEXT NOT NULL)")

        sqliteDb.execSQL("INSERT INTO inventory_snapshots (id, campaign_id, captured_at, metadata_json) VALUES (1, 1, 123456789, '{}')")
        sqliteDb.execSQL("INSERT INTO geometry_versions (id, geometry_profile_id, version_number, fixed_parameters, required_dimensions_schema, measurement_schema, supported_units, calculation_method, validation_constraints) VALUES (10, 1, 1, '{}', '{}', '{}', '{}', 'm', '{}')")
        sqliteDb.execSQL("INSERT INTO inventory_snapshot_items (id, snapshot_id, storage_space_code, captured_geometry_version_id, commodity_name, density, ownership, client_name, final_volume, final_mass, engine_version, metadata_json) VALUES (100, 1, 'C01', 10, 'Wheat', 800.0, 'Owned', null, 50.0, 40.0, 'v1', '{}')")
        sqliteDb.close()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        if (oldVersion == 7 && newVersion == 8) {
                            MIGRATION_7_8.migrate(db)
                        }
                    }
                })
                .build()
        )

        val supportDb = openHelper.writableDatabase
        supportDb.query("SELECT * FROM inventory_snapshot_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(100L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
            assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("snapshot_id")))
            assertEquals("C01", cursor.getString(cursor.getColumnIndexOrThrow("storage_space_code")))
            assertEquals(10L, cursor.getLong(cursor.getColumnIndexOrThrow("captured_geometry_version_id")))
            assertEquals("Wheat", cursor.getString(cursor.getColumnIndexOrThrow("commodity_name")))
            assertEquals(800.0, cursor.getDouble(cursor.getColumnIndexOrThrow("density")), 0.001)
            assertEquals("Owned", cursor.getString(cursor.getColumnIndexOrThrow("ownership")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("client_name")))
            assertEquals(50.0, cursor.getDouble(cursor.getColumnIndexOrThrow("final_volume")), 0.001)
            assertEquals(40.0, cursor.getDouble(cursor.getColumnIndexOrThrow("final_mass")), 0.001)
            assertEquals("v1", cursor.getString(cursor.getColumnIndexOrThrow("engine_version")))
            assertEquals("{}", cursor.getString(cursor.getColumnIndexOrThrow("metadata_json")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("remoteId")))
        }

        // Verify Foreign Keys via PRAGMA foreign_key_list
        val fkCursor = supportDb.query("PRAGMA foreign_key_list('inventory_snapshot_items')")
        var foundSnapshotFk = false
        var foundGeometryFk = false
        while (fkCursor.moveToNext()) {
            val fromCol = fkCursor.getString(fkCursor.getColumnIndexOrThrow("from"))
            val table = fkCursor.getString(fkCursor.getColumnIndexOrThrow("table"))
            val toCol = fkCursor.getString(fkCursor.getColumnIndexOrThrow("to"))
            val onDelete = fkCursor.getString(fkCursor.getColumnIndexOrThrow("on_delete"))
            if (fromCol == "snapshot_id" && table == "inventory_snapshots" && toCol == "id" && onDelete == "CASCADE") {
                foundSnapshotFk = true
            }
            if (fromCol == "captured_geometry_version_id" && table == "geometry_versions" && toCol == "id" && onDelete == "RESTRICT") {
                foundGeometryFk = true
            }
        }
        fkCursor.close()
        assertTrue(foundSnapshotFk)
        assertTrue(foundGeometryFk)

        // Verify indices via PRAGMA index_list
        val indexCursor = supportDb.query("PRAGMA index_list('inventory_snapshot_items')")
        var foundSnapshotIdx = false
        var foundGeometryIdx = false
        var foundRemoteIdIdx = false
        var isUniqueRemoteId = false
        while (indexCursor.moveToNext()) {
            val idxName = indexCursor.getString(indexCursor.getColumnIndexOrThrow("name"))
            val unique = indexCursor.getInt(indexCursor.getColumnIndexOrThrow("unique"))
            if (idxName == "index_inventory_snapshot_items_snapshot_id") {
                foundSnapshotIdx = true
            }
            if (idxName == "index_inventory_snapshot_items_captured_geometry_version_id") {
                foundGeometryIdx = true
            }
            if (idxName == "index_inventory_snapshot_items_remoteId") {
                foundRemoteIdIdx = true
                isUniqueRemoteId = (unique == 1)
            }
        }
        indexCursor.close()
        assertTrue(foundSnapshotIdx)
        assertTrue(foundGeometryIdx)
        assertTrue(foundRemoteIdIdx)
        assertTrue(isUniqueRemoteId)

        openHelper.close()
        dbFile.delete()
    }

    @Test
    fun testMigration7To8FailsOnOrphanGeometry() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("mig_7_8_orphan_test.db")
        if (dbFile.exists()) dbFile.delete()

        val sqliteDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqliteDb.execSQL("PRAGMA user_version = 7")
        assertEquals(7, sqliteDb.version)

        sqliteDb.execSQL("CREATE TABLE inventory_snapshots (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, campaign_id INTEGER NOT NULL, captured_at INTEGER NOT NULL, metadata_json TEXT NOT NULL)")
        sqliteDb.execSQL("CREATE TABLE geometry_versions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, geometry_profile_id INTEGER NOT NULL, version_number INTEGER NOT NULL, fixed_parameters TEXT NOT NULL, required_dimensions_schema TEXT NOT NULL, measurement_schema TEXT NOT NULL, supported_units TEXT NOT NULL, calculation_method TEXT NOT NULL, validation_constraints TEXT NOT NULL, capacity_rules TEXT)")
        sqliteDb.execSQL("CREATE TABLE inventory_snapshot_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, snapshot_id INTEGER NOT NULL, storage_space_code TEXT NOT NULL, captured_geometry_version_id INTEGER NOT NULL, commodity_name TEXT, density REAL, ownership TEXT NOT NULL, client_name TEXT, final_volume REAL NOT NULL, final_mass REAL NOT NULL, engine_version TEXT NOT NULL, metadata_json TEXT NOT NULL)")

        sqliteDb.execSQL("INSERT INTO inventory_snapshots (id, campaign_id, captured_at, metadata_json) VALUES (1, 1, 123456789, '{}')")
        sqliteDb.execSQL("INSERT INTO inventory_snapshot_items (id, snapshot_id, storage_space_code, captured_geometry_version_id, commodity_name, density, ownership, client_name, final_volume, final_mass, engine_version, metadata_json) VALUES (100, 1, 'C01', 999, 'Wheat', 800.0, 'Owned', null, 50.0, 40.0, 'v1', '{}')")
        sqliteDb.close()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        if (oldVersion == 7 && newVersion == 8) {
                            MIGRATION_7_8.migrate(db)
                        }
                    }
                })
                .build()
        )

        try {
            openHelper.writableDatabase
            fail("Expected exception for orphan geometry version")
        } catch (e: Exception) {
            val msg = e.message ?: ""
            val causeMsg = e.cause?.message ?: ""
            assertTrue(msg.contains("orphaned captured_geometry_version_id") || msg.contains("Migration 7->8 failed") ||
                       causeMsg.contains("orphaned captured_geometry_version_id") || causeMsg.contains("Migration 7->8 failed"))
        } finally {
            openHelper.close()
            dbFile.delete()
        }
    }
}
