package com.silomath.core.data.repository

import com.silomath.core.database.AtomicMutationRunner
import com.silomath.core.database.dao.LocationDao
import com.silomath.core.database.dao.OrganizationDao
import com.silomath.core.database.entity.LocationEntity
import com.silomath.core.database.entity.SyncOperationEntity
import com.silomath.core.domain.model.Location
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.domain.repository.LocationRepository
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.SyncOperationState
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OfflineLocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val organizationDao: OrganizationDao,
    private val atomicMutationRunner: AtomicMutationRunner
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
    ): Result<Unit> {
        val organization =
            organizationDao.getOrganizationByOrganizationId(
                location.organizationId.value
            )
                ?: return Result.failure(
                    IllegalStateException(
                        "Canonical Organization not found for Location: " +
                            location.organizationId.value
                    )
                )

        val existing =
            locationDao.getLocationByLocationId(
                location.locationId.value
            )

        if (
            existing?.entityVersion != null &&
            existing.entityVersion != location.entityVersion
        ) {
            return Result.failure(
                IllegalStateException(
                    "Location entityVersion is stale for local mutation."
                )
            )
        }

        val entity =
            location.toEntity(
                localOrganizationId = organization.id,
                existing = existing
            )

        val operationId =
            UUID.randomUUID()
                .toString()
                .lowercase()

        val idempotencyKey =
            UUID.randomUUID()
                .toString()
                .lowercase()

        val syncOperation =
            SyncOperationEntity(
                syncOperationId = operationId,
                idempotencyKey = idempotencyKey,
                organizationId =
                    location.organizationId.value,
                entityType = ENTITY_TYPE_LOCATION,
                entityId = location.locationId.value,
                operationType =
                    if (existing == null) {
                        OPERATION_CREATE
                    } else {
                        OPERATION_PATCH
                    },
                baseEntityVersion =
                    existing?.entityVersion,
                eventAt =
                    location.updatedAt.toEpochMilli(),
                payloadJson =
                    location.toMutationPayloadJson(),
                state = SyncOperationState.PENDING
            )

        return atomicMutationRunner
            .executeAtomicMutation(
                syncOperation = syncOperation
            ) {
                if (existing == null) {
                    locationDao.insertLocation(entity)
                } else {
                    locationDao.updateLocation(entity)
                }
                Unit
            }
    }

    private fun Location.toEntity(
        localOrganizationId: Long,
        existing: LocationEntity?
    ): LocationEntity =
        LocationEntity(
            id = existing?.id ?: 0L,
            organizationId = localOrganizationId,
            remoteId = existing?.remoteId,
            code = code,
            name = name,
            lifecycleStatus = lifecycleStatus.name,
            remoteRevision = existing?.remoteRevision,
            locationId = locationId.value,
            canonicalOrganizationId =
                organizationId.value,
            address = address,
            city = city,
            country = country,
            notes = notes,
            latitudeDeg =
                latitude?.canonicalCoordinate(),
            longitudeDeg =
                longitude?.canonicalCoordinate(),
            createdAt = createdAt.toEpochMilli(),
            createdBy = createdBy,
            updatedAt = updatedAt.toEpochMilli(),
            updatedBy = updatedBy,
            entityVersion = entityVersion
        )

    private fun Location.toMutationPayloadJson(): String =
        buildJsonObject {
            put("name", JsonPrimitive(name))
            put("code", JsonPrimitive(code))
            putNullable("address", address)
            putNullable("city", city)
            putNullable("country", country)
            putNullable("notes", notes)
            put(
                "lifecycleStatus",
                JsonPrimitive(lifecycleStatus.name)
            )
            putCoordinate("latitude", latitude)
            putCoordinate("longitude", longitude)
        }.toString()

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(
        key: String,
        value: String?
    ) {
        put(
            key,
            value?.let(::JsonPrimitive)
                ?: JsonNull
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putCoordinate(
        key: String,
        value: BigDecimal?
    ) {
        put(
            key,
            value
                ?.canonicalCoordinate()
                ?.let(::JsonPrimitive)
                ?: JsonNull
        )
    }

    private fun BigDecimal.canonicalCoordinate(): BigDecimal =
        setScale(
            COORDINATE_SCALE,
            java.math.RoundingMode.HALF_UP
        )

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
                entityVersion = canonicalEntityVersion,
                latitude = latitudeDeg,
                longitude = longitudeDeg
            )
        }.getOrNull()
    }

    private companion object {
        const val ENTITY_TYPE_LOCATION = "LOCATION"
        const val OPERATION_CREATE = "CREATE"
        const val OPERATION_PATCH = "PATCH"
        const val COORDINATE_SCALE = 6
    }
}
