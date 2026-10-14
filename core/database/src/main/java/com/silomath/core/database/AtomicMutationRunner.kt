package com.silomath.core.database

import androidx.room.withTransaction
import com.silomath.core.database.entity.SyncOperationEntity

class AtomicMutationRunner(
    private val database: SiloMathDatabase
) {
    suspend fun <T> executeAtomicMutation(
        syncOperation: SyncOperationEntity,
        mutationBlock: suspend () -> T
    ): Result<T> {
        return try {
            database.withTransaction {
                val result = mutationBlock()
                database.syncOperationDao().insertOperation(syncOperation)
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
