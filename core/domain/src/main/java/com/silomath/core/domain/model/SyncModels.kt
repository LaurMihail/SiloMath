package com.silomath.core.domain.model

import com.silomath.core.model.OrganizationId
import com.silomath.core.model.SyncConflictId
import com.silomath.core.model.SyncDependencyId
import com.silomath.core.model.SyncOperationId
import com.silomath.core.model.UserId
import java.time.Instant

enum class SyncOperationState {
    PENDING,
    IN_FLIGHT,
    ACKNOWLEDGED,
    RETRYABLE_ERROR,
    CONFLICT,
    PERMANENT_ERROR,
}

enum class SyncDependencyType {
    REQUIRED_BEFORE,
}

enum class SyncConflictClass {
    VERSION_CONFLICT,
    STATE_CONFLICT,
    ACCESS_CONFLICT,
    DUPLICATE_BUSINESS_KEY,
    IMMUTABILITY_CONFLICT,
    DEPENDENCY_CONFLICT,
    IDEMPOTENCY_CONFLICT,
    SAFE_DUPLICATE,
}

enum class SyncConflictStatus {
    OPEN,
    RESOLVED,
}

enum class SyncConflictResolutionType {
    RELOAD_AND_REENTER,
    DISCARD_LOCAL_ATTEMPT,
    NO_ACTION_REQUIRED,
}

data class SyncOperation(
    val syncOperationId: SyncOperationId,
    val idempotencyKey: String,
    val organizationId: OrganizationId,
    val campaignId: String? = null,
    val entityType: String,
    val entityId: String,
    val operationType: String,
    val payload: String,
    val baseEntityVersion: Long? = null,
    val eventAt: Instant,
    val state: SyncOperationState = SyncOperationState.PENDING,
    val attemptCount: Int = 0,
    val nextAttemptAt: Instant? = null,
    val lastAttemptAt: Instant? = null,
    val lastErrorCode: String? = null,
    val correlationId: String? = null,
    val acknowledgedAt: Instant? = null,
    val authoritativeEntityVersion: Long? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        require(entityType.isNotBlank()) { "entityType must not be blank" }
        require(entityId.isNotBlank()) { "entityId must not be blank" }
        require(operationType.isNotBlank()) { "operationType must not be blank" }
        require(payload.isNotBlank()) { "payload must contain canonical mutation intent" }
        require(baseEntityVersion == null || baseEntityVersion >= 1L) { "baseEntityVersion must be >= 1 when present" }
        require(attemptCount >= 0) { "attemptCount must be non-negative" }
        require(!updatedAt.isBefore(createdAt)) { "updatedAt must not precede createdAt" }
        require(lastAttemptAt == null || attemptCount > 0) { "lastAttemptAt requires at least one attempt" }
        require(nextAttemptAt == null || state == SyncOperationState.RETRYABLE_ERROR) {
            "nextAttemptAt is valid only for RETRYABLE_ERROR"
        }
        require((state == SyncOperationState.ACKNOWLEDGED) == (acknowledgedAt != null)) {
            "ACKNOWLEDGED state requires acknowledgedAt and other states must not carry it"
        }
        require(authoritativeEntityVersion == null || state == SyncOperationState.ACKNOWLEDGED) {
            "authoritativeEntityVersion is recorded only on ACKNOWLEDGED reconciliation"
        }
    }

    val isSuccessfulTerminal: Boolean
        get() = state == SyncOperationState.ACKNOWLEDGED

    val isUnresolved: Boolean
        get() = state != SyncOperationState.ACKNOWLEDGED
}

data class SyncOperationDependency(
    val dependencyId: SyncDependencyId,
    val parentOperationId: SyncOperationId,
    val dependentOperationId: SyncOperationId,
    val dependencyType: SyncDependencyType = SyncDependencyType.REQUIRED_BEFORE,
    val createdAt: Instant,
) {
    init {
        require(parentOperationId != dependentOperationId) {
            "parent and dependent SyncOperations must differ"
        }
    }

    fun dependentIsEligible(parentState: SyncOperationState): Boolean =
        parentState == SyncOperationState.ACKNOWLEDGED
}

data class SyncConflict(
    val syncConflictId: SyncConflictId,
    val organizationId: OrganizationId,
    val syncOperationId: SyncOperationId,
    val entityType: String,
    val entityId: String,
    val conflictClass: SyncConflictClass,
    val localAttempt: String,
    val serverAuthoritative: String,
    val baseEntityVersion: Long?,
    val serverEntityVersion: Long?,
    val detectedAt: Instant,
    val status: SyncConflictStatus = SyncConflictStatus.OPEN,
    val resolutionType: SyncConflictResolutionType? = null,
    val resolvedAt: Instant? = null,
    val resolvedBy: UserId? = null,
) {
    init {
        require(entityType.isNotBlank()) { "entityType must not be blank" }
        require(entityId.isNotBlank()) { "entityId must not be blank" }
        require(localAttempt.isNotBlank()) { "local attempted state must be preserved" }
        require(serverAuthoritative.isNotBlank()) { "server authoritative state must be preserved" }
        require(baseEntityVersion == null || baseEntityVersion >= 1L) { "baseEntityVersion must be >= 1 when present" }
        require(serverEntityVersion == null || serverEntityVersion >= 1L) { "serverEntityVersion must be >= 1 when present" }

        when (status) {
            SyncConflictStatus.OPEN -> {
                require(resolutionType == null && resolvedAt == null && resolvedBy == null) {
                    "OPEN conflict cannot contain resolution metadata"
                }
            }
            SyncConflictStatus.RESOLVED -> {
                require(resolutionType != null && resolvedAt != null && resolvedBy != null) {
                    "RESOLVED conflict requires resolution type, time and actor"
                }
                require(!resolvedAt.isBefore(detectedAt)) { "resolvedAt must not precede detectedAt" }
            }
        }
    }
}
