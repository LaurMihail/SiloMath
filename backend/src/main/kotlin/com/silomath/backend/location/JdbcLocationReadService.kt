package com.silomath.backend.location

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.Properties

internal class JdbcLocationReadService(
    private val databaseUrl: String,
    private val databaseUser: String?,
    private val databasePassword: String?
) : LocationReadService {

    override fun list(
        query: LocationListQuery
    ): LocationPage {
        require(query.pageSize in 1..100) {
            "pageSize must be between 1 and 100"
        }

        val normalizedStatus =
            query.status
                ?.trim()
                ?.uppercase()
                ?.also {
                    require(
                        it == "ACTIVE" ||
                            it == "INACTIVE"
                    ) {
                        "status must be ACTIVE or INACTIVE"
                    }
                }

        val normalizedSearch =
            query.search
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        val rows =
            openConnection().use { connection ->
                connection.prepareStatement(
                    buildListSql(
                        hasCursor = query.cursor != null,
                        hasSearch = normalizedSearch != null,
                        hasStatus = normalizedStatus != null
                    )
                ).use { statement ->
                    var index = 1

                    statement.setString(
                        index++,
                        query.organizationId
                    )

                    if (query.cursor != null) {
                        statement.setString(
                            index++,
                            query.cursor
                        )
                    }

                    if (normalizedSearch != null) {
                        val pattern =
                            "%${normalizedSearch}%"

                        statement.setString(
                            index++,
                            pattern
                        )
                        statement.setString(
                            index++,
                            pattern
                        )
                    }

                    if (normalizedStatus != null) {
                        statement.setString(
                            index++,
                            normalizedStatus
                        )
                    }

                    statement.setInt(
                        index,
                        query.pageSize + 1
                    )

                    statement.executeQuery().use {
                        resultSet ->
                        buildList {
                            while (resultSet.next()) {
                                add(
                                    resultSet.toLocationResponse()
                                )
                            }
                        }
                    }
                }
            }

        val hasMore =
            rows.size > query.pageSize

        val items =
            if (hasMore) {
                rows.take(query.pageSize)
            } else {
                rows
            }

        return LocationPage(
            items = items,
            nextCursor =
                if (hasMore) {
                    items.last().id
                } else {
                    null
                },
            hasMore = hasMore
        )
    }

    override fun get(
        organizationId: String,
        locationId: String
    ): LocationResponse? =
        openConnection().use { connection ->
            connection.prepareStatement(
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
                limit 1
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
                    resultSet ->
                    if (!resultSet.next()) {
                        null
                    } else {
                        resultSet.toLocationResponse()
                    }
                }
            }
        }

    private fun buildListSql(
        hasCursor: Boolean,
        hasSearch: Boolean,
        hasStatus: Boolean
    ): String {
        val conditions =
            mutableListOf(
                "organization_id = ?::uuid"
            )

        if (hasCursor) {
            conditions +=
                "location_id > ?::uuid"
        }

        if (hasSearch) {
            conditions +=
                "(name ilike ? or code ilike ?)"
        }

        if (hasStatus) {
            conditions +=
                "lifecycle_status = ?"
        }

        return """
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
            where ${conditions.joinToString(" and ")}
            order by location_id
            limit ?
        """.trimIndent()
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
}
