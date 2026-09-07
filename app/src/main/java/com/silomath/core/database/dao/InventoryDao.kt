package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.InventorySnapshotEntity
import com.silomath.core.database.entity.InventorySnapshotItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_snapshots WHERE campaign_id = :campaignId")
    fun getSnapshotsByCampaign(campaignId: Long): Flow<List<InventorySnapshotEntity>>

    @Query("SELECT * FROM inventory_snapshots WHERE campaign_id = :campaignId")
    suspend fun getSnapshotsByCampaignSync(campaignId: Long): List<InventorySnapshotEntity>

    @Query("SELECT * FROM inventory_snapshot_items WHERE snapshot_id = :snapshotId")
    fun getSnapshotItems(snapshotId: Long): Flow<List<InventorySnapshotItemEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: InventorySnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshotItems(items: List<InventorySnapshotItemEntity>)
}
