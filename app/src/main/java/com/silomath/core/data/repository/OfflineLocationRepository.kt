package com.silomath.core.data.repository

import com.silomath.core.database.dao.LocationDao
import com.silomath.core.database.dao.OrganizationDao
import com.silomath.core.database.entity.LocationEntity
import com.silomath.core.domain.model.Location
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.domain.repository.LocationRepository
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineLocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val organizationDao: OrganizationDao
) : LocationRepository {

    override fun observeLocations(
        organizationId: OrganizationId
    ): Flow<List<Location>> = flow {
        val organization =
            organizationDao.getOrganizationByOrganizationId(
                organizationId.value
            )

        if (organization == null) {
            emit(emptyList())
            return@flow
        }

        emitAll(
            locationDao
                .getLocationsByOrganization(organization.id)
                .map { entities ->
                    entities.mapNotNull { entity ->
                        entity.toCanonicalLocationOrNull(
                            expectedOrganizationId = organizationId
                        )
                    }
                }
        )
    }

    override suspend fun saveLocation(
        location: Location
    ): Result<Unit> = runCatching {
        val organization =
            organizationDao.getOrganizationByOrganizationId(
                location.organizationId.value
            )
                ?: error(
                    "Canonical Organization not found for Location: " +
                        location.organizationId.value
                )

        val existing =
            locationDao.getLocationByLocationId(
                location.locationId.value
            )

        val entity = LocationEntity(
            id = existing?.id ?: 0L,
            organizationId = organization.id,
            remoteId = existing?.remoteId,
            code = location.code,
            name = location.name,
            lifecycleStatus = location.lifecycleStatus.name,
            remoteRevision = existing?.remoteRevision,
            locationId = location.locationId.value,
            canonicalOrganizationId = location.organizationId.value,
            address = location.address,
            city = location.city,
            country = location.country,
            notes = location.notes,
            createdAt = location.createdAt.toEpochMilli(),
            createdBy = location.createdBy,
            updatedAt = location.updatedAt.toEpochMilli(),
            updatedBy = location.updatedBy,
            entityVersion = location.entityVersion
        )

        if (existing == null) {
            locationDao.insertLocation(entity)
        } else {
            locationDao.updateLocation(entity)
        }
    }

    private fun LocationEntity.toCanonicalLocationOrNull(
        expectedOrganizationId: OrganizationId
    ): Location? {
        val canonicalLocationId = locationId ?: return null
        val canonicalOrganizationId =
            canonicalOrganizationId ?: return null
        val canonicalCreatedAt = createdAt ?: return null
        val canonicalCreatedBy = createdBy ?: return null
        val canonicalUpdatedAt = updatedAt ?: return null
        val canonicalUpdatedBy = updatedBy ?: return null
        val canonicalEntityVersion = entityVersion ?: return null

        if (canonicalOrganizationId != expectedOrganizationId.value) {
            return null
        }

        val status = when (lifecycleStatus) {
            MasterStatus.ACTIVE.name -> MasterStatus.ACTIVE
            MasterStatus.INACTIVE.name -> MasterStatus.INACTIVE
            else -> return null
        }

        return runCatching {
            Location(
                locationId = LocationId(canonicalLocationId),
                organizationId =
                    OrganizationId(canonicalOrganizationId),
                name = name,
                code = code,
                address = address,
                city = city,
                country = country,
                notes = notes,
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
