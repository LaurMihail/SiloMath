package com.silomath.core.data.repository

import com.silomath.core.database.dao.SyncDao
import com.silomath.core.database.entity.SyncOutboxEntity
import com.silomath.core.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineSyncRepository @Inject constructor(
    private val syncDao: SyncDao
) : SyncRepository {
    override fun getPendingOperations(): Flow<List<SyncOutboxEntity>> = syncDao.getPendingOperations()

    override suspend fun enqueueOperation(operation: SyncOutboxEntity) {
        syncDao.enqueueOperation(operation)
    }

    override suspend fun markOperationCompleted(operation: SyncOutboxEntity) {
        syncDao.dequeueOperation(operation)
    }
}
