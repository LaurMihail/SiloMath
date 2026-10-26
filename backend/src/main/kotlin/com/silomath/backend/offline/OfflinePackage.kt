package com.silomath.backend.offline

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/** Versioned transport artifact, not an alternative authoritative business store. */
@Serializable
data class OfflinePackageEnvelope(
    val formatVersion: Int = 1,
    val manifestId: String,
    val organizationId: String,
    val campaignId: String,
    val deviceInstallationId: String,
    val authorizedUserId: String,
    val packageVersion: String,
    val generatedAt: String,
    val tables: Map<String, JsonArray>
)

data class MaterializedOfflinePackage(val manifest: OfflineManifestResponse, val bytes: ByteArray)

internal object OfflinePackageEncoder {
    private val json = Json { encodeDefaults = true }
    fun encode(envelope: OfflinePackageEnvelope): MaterializedOfflinePackage {
        require(envelope.tables.keys == requiredTables) { "Incomplete package table inventory" }
        require(envelope.tables.getValue("inventory_campaigns").size == 1)
        require(envelope.tables.getValue("campaign_snapshots").size == 1)
        require(envelope.tables.getValue("campaign_snapshot_scope_items").isNotEmpty())
        require(envelope.tables.getValue("campaign_snapshot_commodity_items").isNotEmpty())
        require(envelope.tables.getValue("campaign_snapshot_team_items").isNotEmpty())
        validateCoverage(envelope)
        val bytes = json.encodeToString(envelope).toByteArray(Charsets.UTF_8)
        val inventory = buildJsonObject {
            envelope.tables.toSortedMap().forEach { (name, rows) ->
                put(name, buildJsonObject {
                    put("count", rows.size)
                    put("sha256", sha256(rows.toString().toByteArray(Charsets.UTF_8)))
                })
            }
        }
        val snapshot = envelope.tables.getValue("campaign_snapshots").single().jsonObject
        val compatibility = buildJsonObject {
            put("formatVersion", envelope.formatVersion)
            put("authorizedUserId", envelope.authorizedUserId)
            for (key in listOf("snapshot_schema_version", "calculation_engine_version", "calculation_schema_versions")) {
                val value = snapshot[key]
                require(value != null && value != JsonNull) { "Missing pinned compatibility: $key" }
                require(value.toString() != "\"\"" && value.toString() != "[]") { "Empty pinned compatibility: $key" }
                put(key, value)
            }
            put("byteSize", bytes.size)
        }
        return MaterializedOfflinePackage(
            OfflineManifestResponse(
                envelope.manifestId, envelope.organizationId, envelope.campaignId,
                envelope.deviceInstallationId, envelope.packageVersion, inventory,
                sha256(bytes), "SHA-256", compatibility, envelope.generatedAt
            ), bytes
        )
    }

    private fun validateCoverage(envelope: OfflinePackageEnvelope) {
        val tables=envelope.tables
        fun ids(table: String,key: String)=tables.getValue(table).map {
            it.jsonObject.getValue(key).jsonPrimitive.content
        }.toSet()
        fun covers(table: String,key: String,required: Set<String>) {
            require(ids(table,key).containsAll(required)) { "Incomplete $table coverage" }
        }
        val campaign=tables.getValue("inventory_campaigns").single().jsonObject
        require(campaign.getValue("campaign_id").jsonPrimitive.content==envelope.campaignId)
        require(campaign.getValue("organization_id").jsonPrimitive.content==envelope.organizationId)
        val snapshot=tables.getValue("campaign_snapshots").single().jsonObject
        require(snapshot.getValue("campaign_id").jsonPrimitive.content==envelope.campaignId)
        val scope=ids("campaign_scopes","storage_space_id")
        require(scope.isNotEmpty() && scope==ids("campaign_snapshot_scope_items","storage_space_id"))
        covers("organizations","organization_id",setOf(envelope.organizationId))
        covers("storage_spaces","storage_space_id",scope)
        val frozen=tables.getValue("campaign_snapshot_scope_items")
        for ((table,key) in listOf("locations" to "location_id", "storage_units" to "storage_unit_id",
            "geometry_profiles" to "geometry_profile_id", "geometry_profile_versions" to "geometry_profile_version_id",
            "geometry_families" to "geometry_family_id")) {
            covers(table,key,frozen.map { it.jsonObject.getValue(key).jsonPrimitive.content }.toSet())
        }
        val commodity=tables.getValue("campaign_snapshot_commodity_items")
        covers("commodities","commodity_id",commodity.map { it.jsonObject.getValue("commodity_id").jsonPrimitive.content }.toSet())
        covers("commodity_versions","commodity_version_id",commodity.map { it.jsonObject.getValue("commodity_version_id").jsonPrimitive.content }.toSet())
        covers("users","user_id",setOf(envelope.authorizedUserId))
        require(tables.getValue("organization_memberships").any {
            val m=it.jsonObject
            m.getValue("user_id").jsonPrimitive.content==envelope.authorizedUserId &&
                m.getValue("organization_id").jsonPrimitive.content==envelope.organizationId &&
                m.getValue("lifecycle_status").jsonPrimitive.content=="ACTIVE"
        }) { "Missing active offline membership context" }
    }

    val requiredTables = setOf(
        "organizations", "inventory_campaigns", "campaign_scopes", "campaign_team_assignments",
        "campaign_snapshots", "campaign_snapshot_scope_items", "campaign_snapshot_team_items",
        "campaign_snapshot_commodity_items", "locations", "storage_units", "storage_spaces",
        "geometry_profiles", "geometry_profile_versions", "geometry_families", "commodities",
        "commodity_versions", "organization_memberships", "users", "clients", "stocktaking_records",
        "stocktaking_revisions", "measurements", "calculation_results", "issues", "issue_resolutions",
        "issue_status_events", "evidence"
    )

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

/** Private immutable artifact storage; only server-generated UUIDs select paths. */
internal class OfflinePackageFiles(private val directory: Path) {
    fun write(manifestId: String, bytes: ByteArray) {
        Files.createDirectories(directory)
        FileChannel.open(path(manifestId), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) channel.write(buffer)
            channel.force(true)
        }
    }
    fun read(manifestId: String): ByteArray = Files.readAllBytes(path(manifestId))
    private fun path(id: String): Path = directory.resolve(UUID.fromString(id).toString() + ".package")
}

sealed interface OfflinePackageResult {
    data class Found(val artifact: MaterializedOfflinePackage) : OfflinePackageResult
    data object Forbidden : OfflinePackageResult
    data object NotFound : OfflinePackageResult
}

fun interface OfflinePackageService {
    suspend fun download(userId: String, organizationId: String, campaignId: String,
        deviceInstallationId: String, packageVersion: String, manifestId: String): OfflinePackageResult
}
