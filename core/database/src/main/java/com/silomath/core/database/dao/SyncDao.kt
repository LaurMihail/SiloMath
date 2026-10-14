package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC")
    fun getPendingOperations(): Flow<List<SyncOutboxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOperation(operation: SyncOutboxEntity)

    @Delete
    suspend fun dequeueOperation(operation: SyncOutboxEntity)
}
