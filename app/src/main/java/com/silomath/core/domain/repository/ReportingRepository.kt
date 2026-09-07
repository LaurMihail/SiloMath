package com.silomath.core.domain.repository

import com.silomath.core.database.entity.InventorySnapshotItemEntity
import kotlinx.coroutines.flow.Flow

interface ReportingRepository {
    fun getInventoryReports(campaignId: Long): Flow<List<InventorySnapshotItemEntity>>
}
