package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.StorageSpaceEntity
import com.silomath.core.database.entity.StorageUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageDao {
    @Query("SELECT * FROM storage_units WHERE location_id = :locationId")
    fun getStorageUnitsByLocation(locationId: Long): Flow<List<StorageUnitEntity>>

    @Query("SELECT * FROM storage_units WHERE location_id = :locationId")
    suspend fun getStorageUnitsByLocationSync(locationId: Long): List<StorageUnitEntity>

    @Query("SELECT * FROM storage_spaces WHERE storage_unit_id = :storageUnitId")
    fun getStorageSpacesByUnit(storageUnitId: Long): Flow<List<StorageSpaceEntity>>

    @Query("SELECT * FROM storage_spaces WHERE storage_unit_id = :storageUnitId")
    suspend fun getStorageSpacesByUnitSync(storageUnitId: Long): List<StorageSpaceEntity>

    @Query("SELECT * FROM storage_units WHERE storage_unit_id = :storageUnitId LIMIT 1")
    suspend fun getStorageUnitByStorageUnitId(storageUnitId: String): StorageUnitEntity?

    @Query("SELECT * FROM storage_units WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getStorageUnitByRemoteId(remoteId: String): StorageUnitEntity?

    @Query("SELECT * FROM storage_spaces WHERE storage_space_id = :storageSpaceId LIMIT 1")
    suspend fun getStorageSpaceByStorageSpaceId(storageSpaceId: String): StorageSpaceEntity?

    @Query("SELECT * FROM storage_spaces WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getStorageSpaceByRemoteId(remoteId: String): StorageSpaceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStorageUnit(unit: StorageUnitEntity): Long

    @Update
    suspend fun updateStorageUnit(unit: StorageUnitEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStorageSpace(space: StorageSpaceEntity): Long

    @Update
    suspend fun updateStorageSpace(space: StorageSpaceEntity)
}
