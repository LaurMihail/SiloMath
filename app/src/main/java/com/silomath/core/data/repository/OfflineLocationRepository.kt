package com.silomath.core.data.repository

import com.silomath.core.database.AtomicMutationRunner
import com.silomath.core.database.dao.LocationCanonicalDao
import com.silomath.core.database.entity.SyncOperationEntity
import com.silomath.core.database.entity.canonical.V6LocationsRow
import com.silomath.core.domain.model.CountryCatalog
import com.silomath.core.domain.model.Location
import com.silomath.core.domain.model.MasterStatus
import com.silomath.core.domain.repository.LocationRepository
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.SyncOperationState
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OfflineLocationRepository @Inject constructor(
    private val dao: LocationCanonicalDao,
    private val atomicMutationRunner: AtomicMutationRunner
) : LocationRepository {

    override fun observeLocations(
        organizationId: OrganizationId
    ): Flow<List<Location>> =
        dao.observeByOrganization(organizationId.value).map { rows ->
            rows.mapNotNull { it.toDomainOrNull(organizationId) }
        }

    override suspend fun hasHistoricalReferences(
        organizationId: OrganizationId,
        locationId: LocationId
    ): Result<Boolean> = runCatching {
        val row = checkNotNull(dao.get(locationId.value))
        require(row.organizationId == organizationId.value)
        dao.historicallyReferenced(locationId.value)
    }

    override suspend fun saveLocation(location: Location): Result<Unit> {
        checkNotNull(dao.organization(location.organizationId.value)) {
            "Canonical Organization not found for Location: ${location.organizationId.value}"
        }

        val existing = dao.get(location.locationId.value)

        if (existing != null && existing.entityVersion != location.entityVersion) {
            return Result.failure(
                IllegalStateException("Location entityVersion is stale for local mutation.")
            )
        }

        val row = location.toRow()
        val syncOperation =
            SyncOperationEntity(
                syncOperationId = UUID.randomUUID().toString().lowercase(),
                idempotencyKey = UUID.randomUUID().toString().lowercase(),
                organizationId = location.organizationId.value,
                entityType = ENTITY_TYPE_LOCATION,
                entityId = location.locationId.value,
                operationType = if (existing == null) OPERATION_CREATE else OPERATION_PATCH,
                baseEntityVersion = existing?.entityVersion,
                eventAt = location.updatedAt.toEpochMilli(),
                payloadJson = location.toMutationPayloadJson(),
                state = SyncOperationState.PENDING
            )

        return atomicMutationRunner.executeAtomicMutation(syncOperation) {
            val current = dao.get(location.locationId.value)

            require(current == null || current.organizationId == location.organizationId.value)
            require(current?.entityVersion == existing?.entityVersion) {
                "Location changed while saving"
            }
            require(
                !dao.duplicateCode(
                    location.organizationId.value,
                    location.code,
                    location.locationId.value
                )
            ) {
                "Location Code must be unique in Organization"
            }

            if (current == null || current.country != location.country) {
                require(CountryCatalog.isAllowedNewValue(location.country)) {
                    "Country must be a catalogue value"
                }
            }

            if (current != null && current.code != location.code) {
                require(!dao.historicallyReferenced(location.locationId.value)) {
                    "Historically referenced Location Code is stable"
                }
            }

            if (current == null) {
                dao.insert(row)
            } else {
                check(dao.update(row) == 1)
            }
            Unit
        }
    }

    private fun Location.toRow() =
        V6LocationsRow(
            locationId = locationId.value,
            organizationId = organizationId.value,
            name = name,
            code = code,
            address = address,
            city = city,
            country = country,
            notes = notes,
            latitudeDeg = latitude?.canonicalCoordinate()?.toPlainString(),
            longitudeDeg = longitude?.canonicalCoordinate()?.toPlainString(),
            lifecycleStatus = lifecycleStatus.name,
            createdAt = createdAt.toEpochMilli(),
            createdBy = createdBy,
            updatedAt = updatedAt.toEpochMilli(),
            updatedBy = updatedBy,
            entityVersion = entityVersion
        )

    private fun V6LocationsRow.toDomainOrNull(
        expectedOrganizationId: OrganizationId
    ): Location? {
        if (organizationId != expectedOrganizationId.value) return null

        val status =
            when (lifecycleStatus) {
                MasterStatus.ACTIVE.name -> MasterStatus.ACTIVE
                MasterStatus.INACTIVE.name -> MasterStatus.INACTIVE
                else -> return null
            }

        return runCatching {
            Location(
                locationId = LocationId(locationId),
                organizationId = OrganizationId(organizationId),
                name = name,
                code = code,
                address = address,
                city = city,
                country = country,
                notes = notes,
                lifecycleStatus = status,
                createdAt = Instant.ofEpochMilli(createdAt),
                createdBy = createdBy,
                updatedAt = Instant.ofEpochMilli(updatedAt),
                updatedBy = updatedBy,
                entityVersion = entityVersion,
                latitude = latitudeDeg?.let(::BigDecimal),
                longitude = longitudeDeg?.let(::BigDecimal)
            )
        }.getOrNull()
    }

    private fun Location.toMutationPayloadJson(): String =
        buildJsonObject {
            put("name", JsonPrimitive(name))
            put("code", JsonPrimitive(code))
            putNullable("address", address)
            putNullable("city", city)
            putNullable("country", country)
            putNullable("notes", notes)
            put("lifecycleStatus", JsonPrimitive(lifecycleStatus.name))
            putCoordinate("latitude", latitude)
            putCoordinate("longitude", longitude)
        }.toString()

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(
        key: String,
        value: String?
    ) {
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putCoordinate(
        key: String,
        value: BigDecimal?
    ) {
        put(key, value?.canonicalCoordinate()?.toPlainString()?.let(::JsonPrimitive) ?: JsonNull)
    }

    private fun BigDecimal.canonicalCoordinate(): BigDecimal =
        setScale(COORDINATE_SCALE, RoundingMode.HALF_UP)

    private companion object {
        const val ENTITY_TYPE_LOCATION = "LOCATION"
        const val OPERATION_CREATE = "CREATE"
        const val OPERATION_PATCH = "PATCH"
        const val COORDINATE_SCALE = 6
    }
}
