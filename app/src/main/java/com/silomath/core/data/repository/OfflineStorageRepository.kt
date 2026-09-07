package com.silomath.core.data.repository

import com.silomath.core.database.dao.StorageDao
import com.silomath.core.database.entity.StorageSpaceEntity
import com.silomath.core.database.entity.StorageUnitEntity
import com.silomath.core.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineStorageRepository @Inject constructor(
    private val storageDao: StorageDao
) : StorageRepository {
    override fun getStorageUnits(locationId: Long): Flow<List<StorageUnitEntity>> = 
        storageDao.getStorageUnitsByLocation(locationId)

    override fun getStorageSpaces(storageUnitId: Long): Flow<List<StorageSpaceEntity>> = 
        storageDao.getStorageSpacesByUnit(storageUnitId)

    override suspend fun saveStorageUnit(unit: StorageUnitEntity) {
        storageDao.insertStorageUnit(unit)
    }

    override suspend fun saveStorageSpace(space: StorageSpaceEntity) {
        storageDao.insertStorageSpace(space)
    }
}
