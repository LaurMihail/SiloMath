package com.silomath.backend.offline

import java.sql.DriverManager
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable
data class OfflineManifestResponse(
    val manifestId: String,
    val organizationId: String,
    val campaignId: String,
    val deviceInstallationId: String,
    val packageVersion: String,
    val contentManifest: JsonObject,
    val integrityHash: String,
    val integrityAlgorithm: String,
    val verificationMetadata: JsonObject?,
    val generatedAt: String
)

sealed interface OfflineManifestResult {
    data class Found(val manifest: OfflineManifestResponse) : OfflineManifestResult
    data object Forbidden : OfflineManifestResult
    data object NotFound : OfflineManifestResult
}

fun interface OfflineManifestService {
    suspend fun current(
        userId: String,
        organizationId: String,
        campaignId: String,
        deviceInstallationId: String,
        packageVersion: String?
    ): OfflineManifestResult
}

/** Reads immutable server manifests; it never asserts device-side READY. */
internal class JdbcOfflineManifestService(
    private val databaseUrl: String,
    private val databaseUser: String?,
    private val databasePassword: String?
) : OfflineManifestService {
    override suspend fun current(
        userId: String,
        organizationId: String,
        campaignId: String,
        deviceInstallationId: String,
        packageVersion: String?
    ): OfflineManifestResult = withContext(Dispatchers.IO) {
        val properties = Properties().apply {
            databaseUser?.let { setProperty("user", it) }
            databasePassword?.let { setProperty("password", it) }
        }
        DriverManager.getConnection(databaseUrl, properties).use { connection ->
            // Recheck current access in the same database snapshot as the manifest.
            connection.autoCommit = false
            connection.transactionIsolation = java.sql.Connection.TRANSACTION_REPEATABLE_READ
            connection.isReadOnly = true
            connection.prepareStatement(
                """
                select 1 from silomath.inventory_campaigns c
                join silomath.organization_memberships m on m.organization_id = c.organization_id
                join silomath.users u on u.user_id = m.user_id
                join silomath.organizations o on o.organization_id = c.organization_id
                where c.campaign_id = ?::uuid and c.organization_id = ?::uuid
                  and m.user_id = ?::uuid and m.lifecycle_status = 'ACTIVE'
                  and u.lifecycle_status = 'ACTIVE' and o.lifecycle_status = 'ACTIVE'
                  and (m.role = 'ADMIN' or exists (
                      select 1 from silomath.campaign_team_assignments t
                      where t.campaign_id = c.campaign_id and t.user_id = m.user_id
                        and t.status = 'ACTIVE'))
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, campaignId)
                statement.setString(2, organizationId)
                statement.setString(3, userId)
                statement.executeQuery().use { rows ->
                    if (!rows.next()) return@withContext OfflineManifestResult.Forbidden
                }
            }
            connection.prepareStatement(
                """
                select * from silomath.offline_working_set_manifests
                where campaign_id = ?::uuid and organization_id = ?::uuid
                  and device_installation_id = ?::uuid and is_current
                  and (?::text is null or package_version = ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, campaignId)
                statement.setString(2, organizationId)
                statement.setString(3, deviceInstallationId)
                statement.setString(4, packageVersion)
                statement.setString(5, packageVersion)
                statement.executeQuery().use { rows ->
                    if (!rows.next()) return@withContext OfflineManifestResult.NotFound
                    val manifest = OfflineManifestResponse(
                        manifestId = rows.getString("manifest_id"),
                        organizationId = rows.getString("organization_id"),
                        campaignId = rows.getString("campaign_id"),
                        deviceInstallationId = rows.getString("device_installation_id"),
                        packageVersion = rows.getString("package_version"),
                        contentManifest = Json.parseToJsonElement(rows.getString("content_manifest")).jsonObject,
                        integrityHash = rows.getString("integrity_hash"),
                        integrityAlgorithm = rows.getString("integrity_algorithm"),
                        verificationMetadata = rows.getString("verification_metadata")?.let {
                            Json.parseToJsonElement(it).jsonObject
                        },
                        generatedAt = rows.getTimestamp("generated_at").toInstant().toString()
                    )
                    connection.commit()
                    OfflineManifestResult.Found(manifest)
                }
            }
        }
    }
}
