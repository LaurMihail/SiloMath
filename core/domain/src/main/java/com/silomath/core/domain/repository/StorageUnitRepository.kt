package com.silomath.core.domain.repository

import com.silomath.core.domain.model.StorageUnit
import com.silomath.core.model.LocationId
import kotlinx.coroutines.flow.Flow

interface StorageUnitRepository {
    fun observeStorageUnits(
        locationId: LocationId
    ): Flow<List<StorageUnit>>

    suspend fun hasHistoricalReferences(locationId: LocationId, storageUnitId: com.silomath.core.model.StorageUnitId): Result<Boolean>

    suspend fun refreshStorageUnits(locationId: LocationId): Result<Unit>

    /** Pushes this entity owner's durable outbox; never part of local Save. */
    suspend fun synchronizeStorageUnits(deviceInstallationId: com.silomath.core.model.DeviceInstallationId, clientVersion: String): Result<Unit>

    suspend fun saveStorageUnit(
        storageUnit: StorageUnit
    ): Result<Unit>
}
