package com.silomath.core.domain.repository

import com.silomath.core.database.entity.StorageSpaceEntity
import com.silomath.core.database.entity.StorageUnitEntity
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getStorageUnits(locationId: Long): Flow<List<StorageUnitEntity>>
    fun getStorageSpaces(storageUnitId: Long): Flow<List<StorageSpaceEntity>>
    suspend fun saveStorageUnit(unit: StorageUnitEntity)
    suspend fun saveStorageSpace(space: StorageSpaceEntity)
}
