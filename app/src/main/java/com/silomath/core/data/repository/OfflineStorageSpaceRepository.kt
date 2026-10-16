package com.silomath.core.data.repository

import com.silomath.core.database.dao.GeometryDao
import com.silomath.core.database.dao.StorageDao
import com.silomath.core.database.entity.StorageSpaceEntity
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.domain.model.StorageSpace
import com.silomath.core.domain.repository.StorageSpaceRepository
import com.silomath.core.model.GeometryProfileId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.StorageUnitId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineStorageSpaceRepository @Inject constructor(
    private val storageDao: StorageDao,
    private val geometryDao: GeometryDao
) : StorageSpaceRepository {

    override fun observeStorageSpaces(
        storageUnitId: StorageUnitId
    ): Flow<List<StorageSpace>> = flow {
        val storageUnit =
            storageDao.getStorageUnitByStorageUnitId(
                storageUnitId.value
            )

        if (storageUnit == null) {
            emit(emptyList())
            return@flow
        }

        emitAll(
            storageDao
                .getStorageSpacesByUnit(storageUnit.id)
                .map { entities ->
                    entities.mapNotNull { entity ->
                        entity.toCanonicalStorageSpaceOrNull(
                            expectedStorageUnitId = storageUnitId
                        )
                    }
                }
        )
    }

    override suspend fun saveStorageSpace(
        storageSpace: StorageSpace
    ): Result<Unit> = runCatching {
        val storageUnit =
            storageDao.getStorageUnitByStorageUnitId(
                storageSpace.storageUnitId.value
            )
                ?: error(
                    "Canonical StorageUnit not found for StorageSpace: " +
                        storageSpace.storageUnitId.value
                )

        val geometryProfile =
            geometryDao.getGeometryProfileByGeometryProfileId(
                storageSpace.geometryProfileId.value
            )
                ?: error(
                    "Canonical GeometryProfile not found for StorageSpace: " +
                        storageSpace.geometryProfileId.value
                )

        val canonicalOrganizationId = storageUnit.canonicalOrganizationId
            ?: error("Canonical Organization missing on parent StorageUnit: " + storageSpace.storageUnitId.value)

        val existing =
            storageDao.getStorageSpaceByStorageSpaceId(
                storageSpace.storageSpaceId.value
            )

        val entity = StorageSpaceEntity(
            id = existing?.id ?: 0L,
            code = storageSpace.code,
            storageUnitId = storageUnit.id,
            geometryProfileId = geometryProfile.id,
            remoteId = existing?.remoteId,
            geometryVersionId = existing?.geometryVersionId,
            configuredDimensions = existing?.configuredDimensions,
            lifecycleStatus = storageSpace.lifecycleStatus.name,
            remoteRevision = existing?.remoteRevision,
            storageSpaceId = storageSpace.storageSpaceId.value,
            canonicalStorageUnitId = storageSpace.storageUnitId.value,
            canonicalOrganizationId = canonicalOrganizationId,
            canonicalGeometryProfileId =
                storageSpace.geometryProfileId.value,
            description = storageSpace.description,
            createdAt = storageSpace.createdAt.toEpochMilli(),
            createdBy = storageSpace.createdBy,
            updatedAt = storageSpace.updatedAt.toEpochMilli(),
            updatedBy = storageSpace.updatedBy,
            entityVersion = storageSpace.entityVersion
        )

        if (existing == null) {
            storageDao.insertStorageSpace(entity)
        } else {
            storageDao.updateStorageSpace(entity)
        }
    }

    private fun StorageSpaceEntity.toCanonicalStorageSpaceOrNull(
        expectedStorageUnitId: StorageUnitId
    ): StorageSpace? {
        val canonicalStorageSpaceId = storageSpaceId ?: return null
        val canonicalStorageUnitId = canonicalStorageUnitId ?: return null
        val canonicalGeometryProfileId =
            canonicalGeometryProfileId ?: return null
        val canonicalCreatedAt = createdAt ?: return null
        val canonicalCreatedBy = createdBy ?: return null
        val canonicalUpdatedAt = updatedAt ?: return null
        val canonicalUpdatedBy = updatedBy ?: return null
        val canonicalEntityVersion = entityVersion ?: return null

        if (canonicalStorageUnitId != expectedStorageUnitId.value) {
            return null
        }

        val status = when (lifecycleStatus) {
            MasterStatus.ACTIVE.name -> MasterStatus.ACTIVE
            MasterStatus.INACTIVE.name -> MasterStatus.INACTIVE
            else -> return null
        }

        return runCatching {
            StorageSpace(
                storageSpaceId =
                    StorageSpaceId(canonicalStorageSpaceId),
                storageUnitId =
                    StorageUnitId(canonicalStorageUnitId),
                code = code,
                description = description,
                geometryProfileId =
                    GeometryProfileId(canonicalGeometryProfileId),
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
