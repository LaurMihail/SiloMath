package com.silomath.core.domain.repository

import com.silomath.core.domain.model.StorageUnit
import com.silomath.core.model.LocationId
import kotlinx.coroutines.flow.Flow

interface StorageUnitRepository {
    fun observeStorageUnits(
        locationId: LocationId
    ): Flow<List<StorageUnit>>

    suspend fun saveStorageUnit(
        storageUnit: StorageUnit
    ): Result<Unit>
}
