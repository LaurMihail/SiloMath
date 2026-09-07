package com.silomath.core.domain.repository

import com.silomath.core.database.entity.InventorySnapshotEntity
import com.silomath.core.database.entity.InventorySnapshotItemEntity
import kotlinx.coroutines.flow.Flow

interface InventorySnapshotRepository {
    fun getSnapshots(campaignId: Long): Flow<List<InventorySnapshotEntity>>
    suspend fun createSnapshot(campaignId: Long, metadata: String, snapshotItems: List<InventorySnapshotItemEntity>)
}
