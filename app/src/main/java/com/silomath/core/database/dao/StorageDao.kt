package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.StorageSpaceEntity
import com.silomath.core.database.entity.StorageUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageDao {
    @Query("SELECT * FROM storage_units WHERE location_id = :locationId")
    fun getStorageUnitsByLocation(locationId: Long): Flow<List<StorageUnitEntity>>

    @Query("SELECT * FROM storage_spaces WHERE storage_unit_id = :storageUnitId")
    fun getStorageSpacesByUnit(storageUnitId: Long): Flow<List<StorageSpaceEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStorageUnit(unit: StorageUnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStorageSpace(space: StorageSpaceEntity): Long
}
