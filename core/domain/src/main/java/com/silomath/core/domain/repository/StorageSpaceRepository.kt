package com.silomath.core.domain.repository

import com.silomath.core.domain.model.StorageSpace
import com.silomath.core.model.DeviceInstallationId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.StorageUnitId
import kotlinx.coroutines.flow.Flow

interface StorageSpaceRepository {
    fun observeStorageSpaces(storageUnitId: StorageUnitId): Flow<List<StorageSpace>>
    suspend fun getStorageSpace(storageSpaceId: StorageSpaceId): Result<StorageSpace>
    suspend fun hasHistoricalReferences(storageSpaceId: StorageSpaceId): Result<Boolean>
    suspend fun refreshStorageSpaces(storageUnitId: StorageUnitId): Result<Unit>
    suspend fun saveStorageSpace(storageSpace: StorageSpace): Result<Unit>
    suspend fun synchronizeStorageSpaces(deviceInstallationId: DeviceInstallationId, clientVersion: String): Result<Unit>
}
