package com.silomath.core.data.repository

import com.silomath.core.database.dao.LocationDao
import com.silomath.core.database.dao.StorageDao
import com.silomath.core.database.entity.StorageUnitEntity
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.domain.model.StorageUnit
import com.silomath.core.domain.repository.StorageUnitRepository
import com.silomath.core.model.LocationId
import com.silomath.core.model.StorageUnitId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineStorageUnitRepository @Inject constructor(
    private val storageDao: StorageDao,
    private val locationDao: LocationDao
) : StorageUnitRepository {

    override fun observeStorageUnits(
        locationId: LocationId
    ): Flow<List<StorageUnit>> = flow {
        val location =
            locationDao.getLocationByLocationId(locationId.value)

        if (location == null) {
            emit(emptyList())
            return@flow
        }

        emitAll(
            storageDao
                .getStorageUnitsByLocation(location.id)
                .map { entities ->
                    entities.mapNotNull { entity ->
                        entity.toCanonicalStorageUnitOrNull(
                            expectedLocationId = locationId
                        )
                    }
                }
        )
    }

    override suspend fun saveStorageUnit(
        storageUnit: StorageUnit
    ): Result<Unit> = runCatching {
        val location =
            locationDao.getLocationByLocationId(
                storageUnit.locationId.value
            )
                ?: error(
                    "Canonical Location not found for StorageUnit: " +
                        storageUnit.locationId.value
                )

        val canonicalOrganizationId = location.canonicalOrganizationId
            ?: error("Canonical Organization missing on parent Location: " + storageUnit.locationId.value)

        val existing =
            storageDao.getStorageUnitByStorageUnitId(
                storageUnit.storageUnitId.value
            )

        val entity = StorageUnitEntity(
            id = existing?.id ?: 0L,
            locationId = location.id,
            remoteId = existing?.remoteId,
            code = storageUnit.code,
            name = storageUnit.name,
            type = existing?.type,
            lifecycleStatus = storageUnit.lifecycleStatus.name,
            remoteRevision = existing?.remoteRevision,
            storageUnitId = storageUnit.storageUnitId.value,
            canonicalLocationId = storageUnit.locationId.value,
            canonicalOrganizationId = canonicalOrganizationId,
            description = storageUnit.description,
            createdAt = storageUnit.createdAt.toEpochMilli(),
            createdBy = storageUnit.createdBy,
            updatedAt = storageUnit.updatedAt.toEpochMilli(),
            updatedBy = storageUnit.updatedBy,
            entityVersion = storageUnit.entityVersion
        )

        if (existing == null) {
            storageDao.insertStorageUnit(entity)
        } else {
            storageDao.updateStorageUnit(entity)
        }
    }

    private fun StorageUnitEntity.toCanonicalStorageUnitOrNull(
        expectedLocationId: LocationId
    ): StorageUnit? {
        val canonicalStorageUnitId = storageUnitId ?: return null
        val canonicalLocationId = canonicalLocationId ?: return null
        val canonicalName = name ?: return null
        val canonicalCreatedAt = createdAt ?: return null
        val canonicalCreatedBy = createdBy ?: return null
        val canonicalUpdatedAt = updatedAt ?: return null
        val canonicalUpdatedBy = updatedBy ?: return null
        val canonicalEntityVersion = entityVersion ?: return null

        if (canonicalLocationId != expectedLocationId.value) {
            return null
        }

        val status = when (lifecycleStatus) {
            MasterStatus.ACTIVE.name -> MasterStatus.ACTIVE
            MasterStatus.INACTIVE.name -> MasterStatus.INACTIVE
            else -> return null
        }

        return runCatching {
            StorageUnit(
                storageUnitId = StorageUnitId(canonicalStorageUnitId),
                locationId = LocationId(canonicalLocationId),
                name = canonicalName,
                code = code,
                description = description,
                lifecycleStatus = status,
                createdAt = Instant.ofEpochMilli(canonicalCreatedAt),
                createdBy = canonicalCreatedBy,
                updatedAt = Instant.ofEpochMilli(canonicalUpdatedAt),
                updatedBy = canonicalUpdatedBy,
                entityVersion = canonicalEntityVersion
            )
        }.getOrNull()
    }
}
