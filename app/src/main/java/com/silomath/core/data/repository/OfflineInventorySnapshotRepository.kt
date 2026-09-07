package com.silomath.core.data.repository

import com.silomath.core.database.dao.InventoryDao
import com.silomath.core.database.entity.InventorySnapshotEntity
import com.silomath.core.database.entity.InventorySnapshotItemEntity
import com.silomath.core.domain.repository.InventorySnapshotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineInventorySnapshotRepository @Inject constructor(
    private val inventoryDao: InventoryDao
) : InventorySnapshotRepository {
    override fun getSnapshots(campaignId: Long): Flow<List<InventorySnapshotEntity>> = 
        inventoryDao.getSnapshotsByCampaign(campaignId)

    override suspend fun createSnapshot(campaignId: Long, metadata: String, snapshotItems: List<InventorySnapshotItemEntity>) {
        // Enforce immutability: Check if snapshot already exists for this campaign
        val existing = inventoryDao.getSnapshotsByCampaignSync(campaignId)
        if (existing.isNotEmpty()) {
            throw IllegalStateException("Inventory snapshot already exists for campaign $campaignId and cannot be modified.")
        }
        
        val snapshotId = inventoryDao.insertSnapshot(
            InventorySnapshotEntity(campaignId = campaignId, metadataJson = metadata)
        )
        val itemsWithId = snapshotItems.map { it.copy(snapshotId = snapshotId) }
        inventoryDao.insertSnapshotItems(itemsWithId)
    }
}
