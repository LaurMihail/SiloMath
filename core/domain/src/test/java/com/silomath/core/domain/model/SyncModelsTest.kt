package com.silomath.core.domain.model

import com.silomath.core.model.OrganizationId
import com.silomath.core.model.SyncConflictId
import com.silomath.core.model.SyncDependencyId
import com.silomath.core.model.SyncOperationId
import com.silomath.core.model.UserId
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncModelsTest {
    private val t0 = Instant.parse("2026-10-18T10:00:00Z")
    private val t1 = Instant.parse("2026-10-18T10:01:00Z")

    @Test
    fun dependencyRequiresAcknowledgedParent() {
        val dependency = SyncOperationDependency(
            dependencyId = SyncDependencyId("00000000-0000-4000-8000-000000000001"),
            parentOperationId = SyncOperationId("00000000-0000-4000-8000-000000000002"),
            dependentOperationId = SyncOperationId("00000000-0000-4000-8000-000000000003"),
            createdAt = t0,
        )
        assertFalse(dependency.dependentIsEligible(SyncOperationState.PENDING))
        assertTrue(dependency.dependentIsEligible(SyncOperationState.ACKNOWLEDGED))
    }

    @Test(expected = IllegalArgumentException::class)
    fun dependencyRejectsSelfReference() {
        val id = SyncOperationId("00000000-0000-4000-8000-000000000002")
        SyncOperationDependency(
            dependencyId = SyncDependencyId("00000000-0000-4000-8000-000000000001"),
            parentOperationId = id,
            dependentOperationId = id,
            createdAt = t0,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun acknowledgedOperationRequiresAcknowledgementMetadata() {
        operation(state = SyncOperationState.ACKNOWLEDGED)
    }

    @Test
    fun acknowledgedOperationIsOnlySuccessfulTerminalState() {
        val op = operation(
            state = SyncOperationState.ACKNOWLEDGED,
            acknowledgedAt = t1,
            authoritativeEntityVersion = 2,
        )
        assertTrue(op.isSuccessfulTerminal)
        assertFalse(op.isUnresolved)
    }

    @Test(expected = IllegalArgumentException::class)
    fun openConflictRejectsResolutionMetadata() {
        conflict(
            status = SyncConflictStatus.OPEN,
            resolutionType = SyncConflictResolutionType.DISCARD_LOCAL_ATTEMPT,
            resolvedAt = t1,
            resolvedBy = userId(),
        )
    }

    @Test
    fun resolvedConflictRequiresOneOfFrozenResolutionTypes() {
        val conflict = conflict(
            status = SyncConflictStatus.RESOLVED,
            resolutionType = SyncConflictResolutionType.NO_ACTION_REQUIRED,
            resolvedAt = t1,
            resolvedBy = userId(),
        )
        assertTrue(conflict.status == SyncConflictStatus.RESOLVED)
    }

    private fun operation(
        state: SyncOperationState,
        acknowledgedAt: Instant? = null,
        authoritativeEntityVersion: Long? = null,
    ) = SyncOperation(
        syncOperationId = SyncOperationId("00000000-0000-4000-8000-000000000010"),
        idempotencyKey = "00000000-0000-4000-8000-000000000011",
        organizationId = OrganizationId("00000000-0000-4000-8000-000000000012"),
        entityType = "STOCKTAKING_REVISION",
        entityId = "00000000-0000-4000-8000-000000000013",
        operationType = "ACTION",
        payload = "{\"action\":\"VALIDATE\"}",
        baseEntityVersion = 1,
        eventAt = t0,
        state = state,
        acknowledgedAt = acknowledgedAt,
        authoritativeEntityVersion = authoritativeEntityVersion,
        createdAt = t0,
        updatedAt = t1,
    )

    private fun conflict(
        status: SyncConflictStatus,
        resolutionType: SyncConflictResolutionType? = null,
        resolvedAt: Instant? = null,
        resolvedBy: UserId? = null,
    ) = SyncConflict(
        syncConflictId = SyncConflictId("00000000-0000-4000-8000-000000000020"),
        organizationId = OrganizationId("00000000-0000-4000-8000-000000000021"),
        syncOperationId = SyncOperationId("00000000-0000-4000-8000-000000000022"),
        entityType = "STOCKTAKING_REVISION",
        entityId = "00000000-0000-4000-8000-000000000023",
        conflictClass = SyncConflictClass.VERSION_CONFLICT,
        localAttempt = "{\"value\":1}",
        serverAuthoritative = "{\"value\":2}",
        baseEntityVersion = 1,
        serverEntityVersion = 2,
        detectedAt = t0,
        status = status,
        resolutionType = resolutionType,
        resolvedAt = resolvedAt,
        resolvedBy = resolvedBy,
    )

    private fun userId() = UserId("00000000-0000-4000-8000-000000000024")
}
