package com.silomath.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.common.UuidUtils
import com.silomath.core.database.di.MIGRATION_9_10
import com.silomath.core.database.entity.LocationEntity
import com.silomath.core.database.entity.OrganizationEntity
import com.silomath.core.database.entity.SyncOperationEntity
import com.silomath.core.model.SyncOperationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class Wp01TransactionAndMigrationTest {

    private val testDb = "migration_9_10_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SiloMathDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private lateinit var database: SiloMathDatabase
    private lateinit var runner: AtomicMutationRunner

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            SiloMathDatabase::class.java
        ).allowMainThreadQueries().build()

        runner = AtomicMutationRunner(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun transaction_businessMutationAndSyncOperation_persistTogetherAtomically() = runTest {
        val orgId = database.organizationDao().insertOrganization(OrganizationEntity(name = "Org1"))
        val syncOp = SyncOperationEntity(
            syncOperationId = UuidUtils.generateUuid(),
            idempotencyKey = UuidUtils.generateUuid(),
            organizationId = orgId.toString(),
            entityType = "location",
            entityId = "loc-1",
            operationType = "INSERT",
            payloadJson = "{}",
            state = SyncOperationState.PENDING
        )

        val result = runner.executeAtomicMutation(syncOp) {
            database.locationDao().insertLocation(
                LocationEntity(organizationId = orgId, code = "LOC1", name = "Location 1")
            )
        }

        assertTrue(result.isSuccess)

        val locs = database.locationDao().getLocationsByOrganizationSync(orgId)
        assertEquals(1, locs.size)

        val persistedOp = database.syncOperationDao().getOperationById(syncOp.syncOperationId)
        assertNotNull(persistedOp)
        assertEquals(SyncOperationState.PENDING, persistedOp?.state)
    }

    @Test
    fun transaction_forcedOutboxFailure_rollsBackBusinessMutation() = runTest {
        val orgId = database.organizationDao().insertOrganization(OrganizationEntity(name = "Org2"))
        val duplicateId = "duplicate_id_123"
        val originalSyncOp = SyncOperationEntity(
            syncOperationId = duplicateId,
            idempotencyKey = "key1",
            organizationId = "org1",
            entityType = "location",
            entityId = "loc-2",
            operationType = "INSERT",
            payloadJson = "{\"test\":1}",
            state = SyncOperationState.PENDING
        )

        // Pre-insert original sync operation
        database.syncOperationDao().insertOperation(originalSyncOp)

        // Attempt execution with duplicate syncOperationId to trigger ABORT conflict failure
        val duplicateSyncOp = SyncOperationEntity(
            syncOperationId = duplicateId,
            idempotencyKey = "key2",
            organizationId = "org1",
            entityType = "location",
            entityId = "loc-2",
            operationType = "INSERT",
            payloadJson = "{\"test\":2}",
            state = SyncOperationState.PENDING
        )

        val result = runner.executeAtomicMutation(duplicateSyncOp) {
            database.locationDao().insertLocation(
                LocationEntity(organizationId = orgId, code = "LOC2", name = "Location 2")
            )
        }

        // 1. executeAtomicMutation returns failure
        assertTrue(result.isFailure)

        // 2. Location business row absent after rollback
        val locs = database.locationDao().getLocationsByOrganizationSync(orgId)
        assertEquals(0, locs.size)

        // 3. Original SyncOperation still exists and was NOT replaced
        val persistedOp = database.syncOperationDao().getOperationById(duplicateId)
        assertNotNull(persistedOp)
        assertEquals("key1", persistedOp?.idempotencyKey)
        assertEquals("{\"test\":1}", persistedOp?.payloadJson)
        assertEquals(SyncOperationState.PENDING, persistedOp?.state)
    }

    @Test
    fun migration9To10_migratesNonDestructively() {
        // Create database v9
        var db = helper.createDatabase(testDb, 9)
        db.execSQL("INSERT INTO organizations (id, name, lifecycleStatus) VALUES (1, 'Test Org', 'ACTIVE')")
        db.close()

        // Run migration to v10 with validation
        db = helper.runMigrationsAndValidate(testDb, 10, true, MIGRATION_9_10)

        // Verify existing organization data preserved
        val cursor = db.query("SELECT id, name FROM organizations WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(1L, cursor.getLong(0))
        assertEquals("Test Org", cursor.getString(1))
        cursor.close()

        // Verify sync_operations and device_installations tables created
        val syncCursor = db.query("SELECT COUNT(*) FROM sync_operations")
        assertTrue(syncCursor.moveToFirst())
        assertEquals(0, syncCursor.getInt(0))
        syncCursor.close()

        val deviceCursor = db.query("SELECT COUNT(*) FROM device_installations")
        assertTrue(deviceCursor.moveToFirst())
        assertEquals(0, deviceCursor.getInt(0))
        deviceCursor.close()

        db.close()
    }
}
