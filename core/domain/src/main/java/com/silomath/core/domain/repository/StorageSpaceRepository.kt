package com.silomath.core.domain.repository

import com.silomath.core.domain.model.StorageSpace
import com.silomath.core.model.StorageUnitId
import kotlinx.coroutines.flow.Flow

interface StorageSpaceRepository {
    fun observeStorageSpaces(
        storageUnitId: StorageUnitId
    ): Flow<List<StorageSpace>>

    suspend fun saveStorageSpace(
        storageSpace: StorageSpace
    ): Result<Unit>
}
