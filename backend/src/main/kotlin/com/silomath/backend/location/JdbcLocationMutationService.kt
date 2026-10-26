package com.silomath.backend.location

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Instant
import java.util.Properties
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface LocationMutationResult {
    data class Success(
        val location: LocationResponse,
        val replayed: Boolean
    ) : LocationMutationResult

    data class EntityVersionConflict(
        val authoritativeEntityVersion: Long
    ) : LocationMutationResult

    data object IdempotencyMismatch :
        LocationMutationResult

    data object NotFound :
        LocationMutationResult
}

interface LocationMutationService {
    fun create(
        organizationId: String,
        actorUserId: String,
        correlationId: String,
        request: LocationCreateRequest
    ): LocationMutationResult

    fun patch(
        organizationId: String,
        locationId: String,
        actorUserId: String,
        correlationId: String,
        baseEntityVersion: Long,
        requestJson: JsonObject
    ): LocationMutationResult
}

internal class JdbcLocationMutationService(
    private val databaseUrl: String,
    private val databaseUser: String?,
    private val databasePassword: String?
) : LocationMutationService {

    override fun create(
        organizationId: String,
        actorUserId: String,
        correlationId: String,
        request: LocationCreateRequest
    ): LocationMutationResult {
        val canonicalRequest =
            LocationContractValidator
                .canonicalizeCreate(
                    request
                )

        val coordinates =
            LocationContractValidator.parseCoordinates(
                latitude =
                    canonicalRequest.latitude,
                longitude =
                    canonicalRequest.longitude
            )

        val fingerprint =
            LocationMutationFingerprint.create(
                organizationId = organizationId,
                request =
                    canonicalRequest
            )

        return inTransaction { connection ->
            connection.lockIdempotencyKey(
                request.idempotencyKey
            )

            val replay =
                connection.findIdempotentReplay(
                    idempotencyKey =
                        canonicalRequest.idempotencyKey,
                    fingerprint =
                        fingerprint
                )

            if (replay != null) {
                replay
            } else {
                val clientOccurredAt =
                    canonicalRequest.occurredAtUtc
                        ?.let(Instant::parse)

                val serverNow =
                    Instant.now()

                val locationId =
                    UUID.randomUUID()
                        .toString()

                val created =
                    connection.insertLocation(
                        locationId = locationId,
                        organizationId = organizationId,
                        actorUserId = actorUserId,
                        now = serverNow,
                        request =
                            canonicalRequest,
                        coordinates = coordinates
                    )

                connection.insertAcknowledgedSyncOperation(
                    syncOperationId =
                        request.clientOperationId,
                    organizationId =
                        organizationId,
                    entityId =
                        locationId,
                    operationType =
                        "CREATE",
                    baseEntityVersion =
                        null,
                    idempotencyKey =
                        canonicalRequest.idempotencyKey,
                    fingerprint =
                        fingerprint,
                    eventAt =
                        clientOccurredAt
                            ?: serverNow,
                    correlationId =
                        correlationId,
                    authoritativeEntityVersion =
                        created.entityVersion
                )

                connection.insertAuditEvent(
                    organizationId =
                        organizationId,
                    actorUserId =
                        actorUserId,
                    action =
                        "LOCATION_CREATED",
                    entityId =
                        locationId,
                    occurredAt =
                        serverNow,
                    before =
                        null,
                    after =
                        created,
                    correlationId =
                        correlationId
                )

                LocationMutationResult.Success(
                    location = created,
                    replayed = false
                )
            }
        }
    }

    override fun patch(
        organizationId: String,
        locationId: String,
        actorUserId: String,
        correlationId: String,
        baseEntityVersion: Long,
        requestJson: JsonObject
    ): LocationMutationResult {
        val intent =
            LocationPatchParser.parse(
                requestJson
            )

        val idempotencyKey =
            requestJson.requiredString(
                "idempotencyKey"
            )

        val clientOperationId =
            requestJson.requiredString(
                "clientOperationId"
            )

        val clientOccurredAt =
            requestJson.optionalString(
                "occurredAtUtc"
            )
                ?.let(Instant::parse)

        val fingerprint =
            LocationMutationFingerprint.patch(
                organizationId =
                    organizationId,
                locationId =
                    locationId,
                baseEntityVersion =
                    baseEntityVersion,
                json =
                    requestJson
            )

        return inTransaction { connection ->
            connection.lockIdempotencyKey(
                idempotencyKey
            )

            val replay =
                connection.findIdempotentReplay(
                    idempotencyKey =
                        idempotencyKey,
                    fingerprint =
                        fingerprint
                )

            if (replay != null) {
                replay
            } else {
                val before =
                    connection.selectLocationForUpdate(
                        organizationId =
                            organizationId,
                        locationId =
                            locationId
                    )

                if (before == null) {
                    LocationMutationResult.NotFound
                } else if (
                    before.entityVersion !=
                    baseEntityVersion
                ) {
                    LocationMutationResult
                        .EntityVersionConflict(
                            authoritativeEntityVersion =
                                before.entityVersion
                        )
                } else {
                    val serverNow =
                        Instant.now()

                    val after =
                        connection.updateLocation(
                            before = before,
                            actorUserId = actorUserId,
                            occurredAt = serverNow,
                            intent = intent
                        )

                    connection.insertAcknowledgedSyncOperation(
                        syncOperationId =
                            clientOperationId,
                        organizationId =
                            organizationId,
                        entityId =
                            locationId,
                        operationType =
                            "PATCH",
                        baseEntityVersion =
                            baseEntityVersion,
                        idempotencyKey =
                            idempotencyKey,
                        fingerprint =
                            fingerprint,
                        eventAt =
                            clientOccurredAt
                                ?: serverNow,
                        correlationId =
                            correlationId,
                        authoritativeEntityVersion =
                            after.entityVersion
                    )

                    connection.insertAuditEvent(
                        organizationId =
                            organizationId,
                        actorUserId =
                            actorUserId,
                        action =
                            "LOCATION_UPDATED",
                        entityId =
                            locationId,
                        occurredAt =
                            serverNow,
                        before =
                            before,
                        after =
                            after,
                        correlationId =
                            correlationId
                    )

                    LocationMutationResult.Success(
                        location = after,
                        replayed = false
                    )
                }
            }
        }
    }

    private fun Connection.findIdempotentReplay(
        idempotencyKey: String,
        fingerprint: String
    ): LocationMutationResult? {
        prepareStatement(
            """
            select
                entity_id,
                payload ->> 'requestFingerprint'
                    as request_fingerprint
            from silomath.sync_operations
            where idempotency_key = ?
            limit 1
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                idempotencyKey
            )

            statement.executeQuery().use {
                rows ->
                if (!rows.next()) {
                    return null
                }

                if (
                    rows.getString(
                        "request_fingerprint"
                    ) != fingerprint
                ) {
                    return LocationMutationResult
                        .IdempotencyMismatch
                }

                val entityId =
                    rows.getString(
                        "entity_id"
                    )

                val location =
                    selectLocationById(
                        entityId
                    )
                        ?: return LocationMutationResult
                            .NotFound

                return LocationMutationResult
                    .Success(
                        location = location,
                        replayed = true
                    )
            }
        }
    }

    private fun Connection.insertLocation(
        locationId: String,
        organizationId: String,
        actorUserId: String,
        now: Instant,
        request: LocationCreateRequest,
        coordinates: CanonicalCoordinates
    ): LocationResponse {
        prepareStatement(
            """
            insert into silomath.locations (
                location_id,
                organization_id,
                name,
                code,
                address,
                city,
                country,
                notes,
                latitude_deg,
                longitude_deg,
                lifecycle_status,
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            )
            values (
                ?::uuid,
                ?::uuid,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?::timestamptz,
                ?::uuid,
                ?::timestamptz,
                ?::uuid,
                1
            )
            returning
                location_id,
                organization_id,
                name,
                code,
                address,
                city,
                country,
                notes,
                latitude_deg,
                longitude_deg,
                lifecycle_status,
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                locationId
            )
            statement.setString(
                2,
                organizationId
            )
            statement.setString(
                3,
                request.name
            )
            statement.setString(
                4,
                request.code
            )
            statement.setString(
                5,
                request.address
            )
            statement.setString(
                6,
                request.city
            )
            statement.setString(
                7,
                request.country
            )
            statement.setString(
                8,
                request.notes
            )
            statement.setBigDecimal(
                9,
                coordinates.latitude
            )
            statement.setBigDecimal(
                10,
                coordinates.longitude
            )
            statement.setString(
                11,
                request.lifecycleStatus
            )
            statement.setString(
                12,
                now.toString()
            )
            statement.setString(
                13,
                actorUserId
            )
            statement.setString(
                14,
                now.toString()
            )
            statement.setString(
                15,
                actorUserId
            )

            statement.executeQuery().use {
                rows ->
                check(rows.next()) {
                    "Location INSERT returned no row."
                }
                return rows.toLocationResponse()
            }
        }
    }

    private fun Connection.updateLocation(
        before: LocationResponse,
        actorUserId: String,
        occurredAt: Instant,
        intent: LocationPatchIntent
    ): LocationResponse {
        val name =
            intent.name.resolveRequired(
                before.name
            )

        val code =
            intent.code.resolveRequired(
                before.code
            )

        val address =
            intent.address.resolveNullable(
                before.address
            )

        val city =
            intent.city.resolveNullable(
                before.city
            )

        val country =
            intent.country.resolveNullable(
                before.country
            )

        val notes =
            intent.notes.resolveNullable(
                before.notes
            )

        val lifecycleStatus =
            intent.lifecycleStatus.resolveRequired(
                before.lifecycleStatus
            )

        val latitude =
            intent.latitude.resolveDecimal(
                before.latitude
                    ?.toBigDecimal()
            )

        val longitude =
            intent.longitude.resolveDecimal(
                before.longitude
                    ?.toBigDecimal()
            )

        prepareStatement(
            """
            update silomath.locations
            set
                name = ?,
                code = ?,
                address = ?,
                city = ?,
                country = ?,
                notes = ?,
                latitude_deg = ?,
                longitude_deg = ?,
                lifecycle_status = ?,
                updated_at = ?::timestamptz,
                updated_by = ?::uuid,
                entity_version =
                    entity_version + 1
            where location_id = ?::uuid
              and organization_id = ?::uuid
            returning
                location_id,
                organization_id,
                name,
                code,
                address,
                city,
                country,
                notes,
                latitude_deg,
                longitude_deg,
                lifecycle_status,
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, name)
            statement.setString(2, code)
            statement.setString(3, address)
            statement.setString(4, city)
            statement.setString(5, country)
            statement.setString(6, notes)
            statement.setBigDecimal(
                7,
                latitude
            )
            statement.setBigDecimal(
                8,
                longitude
            )
            statement.setString(
                9,
                lifecycleStatus
            )
            statement.setString(
                10,
                occurredAt.toString()
            )
            statement.setString(
                11,
                actorUserId
            )
            statement.setString(
                12,
                before.id
            )
            statement.setString(
                13,
                before.organizationId
            )

            statement.executeQuery().use {
                rows ->
                check(rows.next()) {
                    "Location UPDATE returned no row."
                }
                return rows.toLocationResponse()
            }
        }
    }

    private fun Connection.selectLocationForUpdate(
        organizationId: String,
        locationId: String
    ): LocationResponse? {
        prepareStatement(
            """
            select
                location_id,
                organization_id,
                name,
                code,
                address,
                city,
                country,
                notes,
                latitude_deg,
                longitude_deg,
                lifecycle_status,
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            from silomath.locations
            where organization_id = ?::uuid
              and location_id = ?::uuid
            for update
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                organizationId
            )
            statement.setString(
                2,
                locationId
            )

            statement.executeQuery().use {
                rows ->
                return if (rows.next()) {
                    rows.toLocationResponse()
                } else {
                    null
                }
            }
        }
    }

    private fun Connection.selectLocationById(
        locationId: String
    ): LocationResponse? {
        prepareStatement(
            """
            select
                location_id,
                organization_id,
                name,
                code,
                address,
                city,
                country,
                notes,
                latitude_deg,
                longitude_deg,
                lifecycle_status,
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            from silomath.locations
            where location_id = ?::uuid
            limit 1
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                locationId
            )

            statement.executeQuery().use {
                rows ->
                return if (rows.next()) {
                    rows.toLocationResponse()
                } else {
                    null
                }
            }
        }
    }

    private fun Connection.insertAcknowledgedSyncOperation(
        syncOperationId: String,
        organizationId: String,
        entityId: String,
        operationType: String,
        baseEntityVersion: Long?,
        idempotencyKey: String,
        fingerprint: String,
        eventAt: Instant,
        correlationId: String,
        authoritativeEntityVersion: Long
    ) {
        prepareStatement(
            """
            insert into silomath.sync_operations (
                sync_operation_id,
                organization_id,
                campaign_id,
                entity_type,
                entity_id,
                operation_type,
                payload,
                base_entity_version,
                state,
                idempotency_key,
                attempt_count,
                next_attempt_at,
                last_attempt_at,
                last_error_code,
                correlation_id,
                event_at,
                acknowledged_at,
                authoritative_entity_version,
                created_at,
                updated_at
            )
            values (
                ?::uuid,
                ?::uuid,
                null,
                'LOCATION',
                ?::uuid,
                ?,
                jsonb_build_object(
                    'requestFingerprint',
                    ?
                ),
                ?,
                'ACKNOWLEDGED',
                ?,
                0,
                null,
                null,
                null,
                ?,
                ?::timestamptz,
                now(),
                ?,
                now(),
                now()
            )
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                syncOperationId
            )
            statement.setString(
                2,
                organizationId
            )
            statement.setString(
                3,
                entityId
            )
            statement.setString(
                4,
                operationType
            )
            statement.setString(
                5,
                fingerprint
            )

            if (baseEntityVersion == null) {
                statement.setNull(
                    6,
                    java.sql.Types.BIGINT
                )
            } else {
                statement.setLong(
                    6,
                    baseEntityVersion
                )
            }

            statement.setString(
                7,
                idempotencyKey
            )
            statement.setString(
                8,
                correlationId
            )
            statement.setString(
                9,
                eventAt.toString()
            )
            statement.setLong(
                10,
                authoritativeEntityVersion
            )
            statement.executeUpdate()
        }
    }

    private fun Connection.insertAuditEvent(
        organizationId: String,
        actorUserId: String,
        action: String,
        entityId: String,
        occurredAt: Instant,
        before: LocationResponse?,
        after: LocationResponse,
        correlationId: String
    ) {
        prepareStatement(
            """
            insert into silomath.audit_events (
                audit_event_id,
                organization_id,
                campaign_id,
                actor_user_id,
                action,
                entity_type,
                entity_id,
                occurred_at,
                before_data,
                after_data,
                change_metadata,
                correlation_id,
                source,
                system_actor
            )
            values (
                ?::uuid,
                ?::uuid,
                null,
                ?::uuid,
                ?,
                'LOCATION',
                ?::uuid,
                ?::timestamptz,
                ?::jsonb,
                ?::jsonb,
                null,
                ?,
                'API',
                null
            )
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                UUID.randomUUID().toString()
            )
            statement.setString(
                2,
                organizationId
            )
            statement.setString(
                3,
                actorUserId
            )
            statement.setString(
                4,
                action
            )
            statement.setString(
                5,
                entityId
            )
            statement.setString(
                6,
                occurredAt.toString()
            )
            statement.setString(
                7,
                before?.toAuditJson()
            )
            statement.setString(
                8,
                after.toAuditJson()
            )
            statement.setString(
                9,
                correlationId
            )
            statement.executeUpdate()
        }
    }

    private fun Connection.lockIdempotencyKey(
        idempotencyKey: String
    ) {
        prepareStatement(
            """
            select pg_advisory_xact_lock(
                hashtextextended(?, 0)
            )
            """.trimIndent()
        ).use { statement ->
            statement.setString(
                1,
                idempotencyKey
            )
            statement.execute()
        }
    }

    private fun <T> inTransaction(
        block: (Connection) -> T
    ): T =
        openConnection().use { connection ->
            val previousAutoCommit =
                connection.autoCommit

            connection.autoCommit = false

            try {
                val result =
                    block(connection)

                connection.commit()
                result
            } catch (error: Exception) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit =
                    previousAutoCommit
            }
        }

    private fun openConnection(): Connection {
        if (databaseUser.isNullOrBlank()) {
            return DriverManager.getConnection(
                databaseUrl
            )
        }

        val properties =
            Properties().apply {
                setProperty(
                    "user",
                    databaseUser
                )

                if (
                    !databasePassword.isNullOrBlank()
                ) {
                    setProperty(
                        "password",
                        databasePassword
                    )
                }
            }

        return DriverManager.getConnection(
            databaseUrl,
            properties
        )
    }

    private fun ResultSet.toLocationResponse():
        LocationResponse =
        LocationResponse(
            id =
                getString("location_id"),
            organizationId =
                getString("organization_id"),
            name =
                getString("name"),
            code =
                getString("code"),
            address =
                getString("address"),
            city =
                getString("city"),
            country =
                getString("country"),
            notes =
                getString("notes"),
            lifecycleStatus =
                getString("lifecycle_status"),
            createdAt =
                getObject(
                    "created_at",
                    java.time.OffsetDateTime::class.java
                ).toInstant().toString(),
            createdBy =
                getString("created_by"),
            updatedAt =
                getObject(
                    "updated_at",
                    java.time.OffsetDateTime::class.java
                ).toInstant().toString(),
            updatedBy =
                getString("updated_by"),
            entityVersion =
                getLong("entity_version"),
            latitude =
                getBigDecimal("latitude_deg")
                    ?.toPlainString(),
            longitude =
                getBigDecimal("longitude_deg")
                    ?.toPlainString()
        )

    private fun JsonObject.requiredString(
        key: String
    ): String =
        optionalString(key)
            ?: throw IllegalArgumentException(
                "$key is required"
            )

    private fun JsonObject.optionalString(
        key: String
    ): String? {
        val value =
            this[key]
                ?: return null

        val primitive =
            value as? JsonPrimitive
                ?: throw IllegalArgumentException(
                    "$key must be a JSON string"
                )

        require(primitive.isString) {
            "$key must be a JSON string"
        }

        return primitive.content
            .takeIf { it.isNotBlank() }
    }

    private fun <T> PatchValue<T>.resolveRequired(
        current: T
    ): T =
        when (this) {
            PatchValue.Omitted ->
                current

            PatchValue.Clear ->
                error(
                    "Required field cannot be cleared."
                )

            is PatchValue.Set ->
                value
        }

    private fun PatchValue<String>.resolveNullable(
        current: String?
    ): String? =
        when (this) {
            PatchValue.Omitted ->
                current

            PatchValue.Clear ->
                null

            is PatchValue.Set ->
                value
        }

    private fun PatchValue<java.math.BigDecimal>
        .resolveDecimal(
            current: java.math.BigDecimal?
        ): java.math.BigDecimal? =
        when (this) {
            PatchValue.Omitted ->
                current

            PatchValue.Clear ->
                null

            is PatchValue.Set ->
                value
        }

    private fun LocationResponse.toAuditJson():
        String =
        Json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("id", id)
                put(
                    "organizationId",
                    organizationId
                )
                put("name", name)
                put("code", code)
                address?.let {
                    put("address", it)
                }
                city?.let {
                    put("city", it)
                }
                country?.let {
                    put("country", it)
                }
                notes?.let {
                    put("notes", it)
                }
                latitude?.let {
                    put("latitude", it)
                }
                longitude?.let {
                    put("longitude", it)
                }
                put(
                    "lifecycleStatus",
                    lifecycleStatus
                )
                put(
                    "entityVersion",
                    entityVersion
                )
            }
        )
}
