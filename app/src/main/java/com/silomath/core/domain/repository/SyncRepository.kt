package com.silomath.core.domain.repository

import com.silomath.core.database.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun getPendingOperations(): Flow<List<SyncOutboxEntity>>
    suspend fun enqueueOperation(operation: SyncOutboxEntity)
    suspend fun markOperationCompleted(operation: SyncOutboxEntity)
}
