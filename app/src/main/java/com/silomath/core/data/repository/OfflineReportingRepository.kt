package com.silomath.core.data.repository

import com.silomath.core.database.dao.InventoryDao
import com.silomath.core.database.entity.InventorySnapshotItemEntity
import com.silomath.core.domain.repository.ReportingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineReportingRepository @Inject constructor(
    private val inventoryDao: InventoryDao
) : ReportingRepository {
    override fun getInventoryReports(campaignId: Long): Flow<List<InventorySnapshotItemEntity>> {
        return inventoryDao.getSnapshotsByCampaign(campaignId).map { snapshots ->
            snapshots.lastOrNull()?.id ?: -1L
        }.flatMapLatest { lastSnapshotId ->
            inventoryDao.getSnapshotItems(lastSnapshotId)
        }
    }
}
