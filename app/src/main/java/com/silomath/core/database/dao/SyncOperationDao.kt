package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.SyncOperationEntity
import com.silomath.core.model.SyncOperationState
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: SyncOperationEntity)

    @Update
    suspend fun updateOperation(operation: SyncOperationEntity)

    @Query("SELECT * FROM sync_operations WHERE sync_operation_id = :id LIMIT 1")
    suspend fun getOperationById(id: String): SyncOperationEntity?

    @Query("SELECT * FROM sync_operations WHERE state = :state ORDER BY created_at ASC")
    fun getOperationsByState(state: SyncOperationState): Flow<List<SyncOperationEntity>>

    @Query("SELECT * FROM sync_operations WHERE state = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingOperationsList(): List<SyncOperationEntity>

    @Query("DELETE FROM sync_operations WHERE sync_operation_id = :id")
    suspend fun deleteOperationById(id: String)
}
