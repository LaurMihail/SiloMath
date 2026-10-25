package com.silomath.core.database.transaction

import androidx.room.withTransaction
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.entity.SyncOperationDependencyRoomEntity
import com.silomath.core.database.entity.SyncOperationEntity

/**
 * Frozen V6 local-first commit boundary.
 *
 * UI-visible Save success is valid only after the supplied business mutation,
 * its durable SyncOperation and any REQUIRED_BEFORE dependency edges commit in
 * one Room transaction. Network dispatch is intentionally outside this class.
 */
class RoomAtomicMutationExecutor(
    private val database: SiloMathDatabase
) {

    suspend fun <T> execute(
        operation: SyncOperationEntity,
        dependencies: List<SyncOperationDependencyRoomEntity> = emptyList(),
        businessMutation: suspend () -> T
    ): T {
        require(operation.state == "PENDING") {
            "New offline mutation SyncOperation must start in PENDING"
        }
        require(operation.idempotencyKey.isNotBlank()) {
            "SyncOperation idempotencyKey must be stable and non-blank"
        }
        require(operation.syncOperationId.isNotBlank()) {
            "SyncOperation identity must be allocated before local commit"
        }
        require(operation.entityId.isNotBlank() && operation.entityType.isNotBlank()) {
            "SyncOperation target identity must be allocated before local commit"
        }
        require(dependencies.all { it.dependentOperationId == operation.syncOperationId }) {
            "Every dependency committed with an operation must target that operation"
        }
        require(dependencies.all { it.parentOperationId != it.dependentOperationId }) {
            "SyncOperation dependency cannot reference itself"
        }
        require(dependencies.all { it.dependencyType == REQUIRED_BEFORE }) {
            "V6 dependency type is frozen to REQUIRED_BEFORE"
        }

        return database.withTransaction {
            val result = businessMutation()
            database.syncOperationDao().insertOperation(operation)
            if (dependencies.isNotEmpty()) {
                database.syncDependencyConflictDao().insertDependencies(dependencies)
            }
            result
        }
    }

    companion object {
        const val REQUIRED_BEFORE = "REQUIRED_BEFORE"
    }
}
