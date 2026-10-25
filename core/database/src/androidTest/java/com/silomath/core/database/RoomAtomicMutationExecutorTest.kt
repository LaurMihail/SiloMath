package com.silomath.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.entity.EvidenceRoomEntity
import com.silomath.core.database.entity.SyncOperationEntity
import com.silomath.core.database.transaction.RoomAtomicMutationExecutor
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAtomicMutationExecutorTest {

    private lateinit var database: SiloMathDatabase
    private lateinit var executor: RoomAtomicMutationExecutor

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SiloMathDatabase::class.java
        ).build()
        executor = RoomAtomicMutationExecutor(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun execute_commitsBusinessMutationAndOutboxTogether() = runTest {
        val evidence = evidence("evidence-success")
        val operation = operation(
            operationId = "operation-success",
            idempotencyKey = "idem-success",
            entityId = evidence.evidenceId
        )

        executor.execute(operation) {
            database.evidenceDao().insert(evidence)
        }

        assertNotNull(database.evidenceDao().getById(evidence.evidenceId))
        assertNotNull(database.syncOperationDao().getOperationById(operation.syncOperationId))
    }

    @Test
    fun execute_rollsBackBusinessMutationWhenOutboxInsertFails() = runTest {
        val existing = operation(
            operationId = "operation-existing",
            idempotencyKey = "idem-duplicate",
            entityId = "existing-entity"
        )
        database.syncOperationDao().insertOperation(existing)

        val evidence = evidence("evidence-rollback")
        val duplicateIdempotencyOperation = operation(
            operationId = "operation-new",
            idempotencyKey = "idem-duplicate",
            entityId = evidence.evidenceId
        )

        var failed = false
        try {
            executor.execute(duplicateIdempotencyOperation) {
                database.evidenceDao().insert(evidence)
            }
        } catch (_: Exception) {
            failed = true
        }

        assertEquals(true, failed)
        assertNull(database.evidenceDao().getById(evidence.evidenceId))
        assertNull(database.syncOperationDao().getOperationById("operation-new"))
        assertNotNull(database.syncOperationDao().getOperationById("operation-existing"))
    }

    private fun evidence(id: String) = EvidenceRoomEntity(
        evidenceId = id,
        organizationId = "00000000-0000-0000-0000-000000000001",
        campaignId = "00000000-0000-0000-0000-000000000002",
        stocktakingRecordId = "00000000-0000-0000-0000-000000000003",
        evidenceType = "NOTE",
        noteText = "Atomic transaction test",
        syncState = "LOCAL_PENDING",
        createdAt = 1_700_000_000_000,
        createdBy = "00000000-0000-0000-0000-000000000004"
    )

    private fun operation(
        operationId: String,
        idempotencyKey: String,
        entityId: String
    ) = SyncOperationEntity(
        syncOperationId = operationId,
        organizationId = "00000000-0000-0000-0000-000000000001",
        campaignId = "00000000-0000-0000-0000-000000000002",
        entityType = "EVIDENCE",
        entityId = entityId,
        operationType = "UPSERT",
        payload = "{}",
        baseEntityVersion = null,
        state = "PENDING",
        idempotencyKey = idempotencyKey,
        attemptCount = 0,
        nextAttemptAt = null,
        lastAttemptAt = null,
        lastErrorCode = null,
        correlationId = null,
        eventAt = 1_700_000_000_000,
        acknowledgedAt = null,
        authoritativeEntityVersion = null,
        createdAt = 1_700_000_000_000,
        updatedAt = 1_700_000_000_000
    )
}
