package com.silomath.core.sync.offline

import com.silomath.core.data.network.dto.OfflineManifestDto
import com.silomath.core.database.entity.OfflinePackageDownloadEntity
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.*
import kotlin.coroutines.coroutineContext

class VerifiedOfflinePackage internal constructor(
    val manifest: OfflineManifestDto,
    val authorizedUserId: String,
    val tables: Map<String,JsonArray>
)

class OfflinePackageIntegrityVerifier(private val staging: PackageStagingStore) {
    suspend fun verify(record: OfflinePackageDownloadEntity): VerifiedOfflinePackage {
        require(record.state=="VERIFYING") { "Package has not completed transfer" }
        val manifest=Json.decodeFromString<OfflineManifestDto>(record.manifestJson)
        require(manifest.integrityAlgorithm=="SHA-256" && manifest.integrityHash.matches(Regex("[0-9a-f]{64}")))
        require(listOf(manifest.manifestId,manifest.organizationId,manifest.campaignId,manifest.deviceInstallationId,manifest.packageVersion)==
            listOf(record.manifestId,record.organizationId,record.campaignId,record.deviceInstallationId,record.packageVersion))
        val metadata=requireNotNull(manifest.verificationMetadata)
        val expected=metadata.getValue("byteSize").jsonPrimitive.long
        require(expected>0 && record.downloadedBytes==expected)
        val digest=MessageDigest.getInstance("SHA-256")
        val output=ByteArrayOutputStream()
        var size=0L
        staging.open(requireNotNull(record.stagingPrivateRef)).use { input ->
            val buffer=ByteArray(64*1024)
            while(true) {
                coroutineContext.ensureActive()
                val read=input.read(buffer)
                if(read<0) break
                if(read==0) continue
                size+=read
                require(size<=expected) { "Package length exceeds manifest" }
                digest.update(buffer,0,read)
                output.write(buffer,0,read)
            }
        }
        require(size==expected && hex(digest.digest())==manifest.integrityHash) { "Package hash/length mismatch" }
        val root=Json.parseToJsonElement(output.toString(Charsets.UTF_8.name())).jsonObject
        require(root.getValue("formatVersion").jsonPrimitive.int==1 && metadata.getValue("formatVersion").jsonPrimitive.int==1)
        for ((key,value) in mapOf("manifestId" to record.manifestId,"organizationId" to record.organizationId,
            "campaignId" to record.campaignId,"deviceInstallationId" to record.deviceInstallationId,
            "authorizedUserId" to record.authorizedUserId,"packageVersion" to record.packageVersion,"generatedAt" to manifest.generatedAt)) {
            require(root.getValue(key).jsonPrimitive.content==value) { "Package identity mismatch: $key" }
        }
        require(metadata.getValue("authorizedUserId").jsonPrimitive.content==record.authorizedUserId)
        val tables=root.getValue("tables").jsonObject.mapValues { it.value.jsonArray }
        require(tables.keys==requiredTables && manifest.contentManifest.keys==requiredTables) { "Incomplete manifest inventory" }
        for ((table,rows) in tables) {
            coroutineContext.ensureActive()
            val inventory=manifest.contentManifest.getValue(table).jsonObject
            require(inventory.getValue("count").jsonPrimitive.int==rows.size) { "Count mismatch: $table" }
            val hash=hex(MessageDigest.getInstance("SHA-256").digest(rows.toString().toByteArray(Charsets.UTF_8)))
            require(inventory.getValue("sha256").jsonPrimitive.content==hash) { "Collection hash mismatch: $table" }
            rows.forEach { row ->
                val obj=row.jsonObject
                obj["entity_version"]?.let { require(it.jsonPrimitive.long>=1) { "Invalid entity version" } }
                obj["version_number"]?.let { require(it.jsonPrimitive.int>=1) { "Invalid immutable version" } }
            }
        }
        val snapshot=tables.getValue("campaign_snapshots").single().jsonObject
        for(key in listOf("snapshot_schema_version","calculation_engine_version","calculation_schema_versions")) {
            require(snapshot.getValue(key)==metadata.getValue(key)) { "Pinned compatibility metadata mismatch: $key" }
            require(snapshot.getValue(key)!=JsonNull && snapshot.getValue(key).toString()!="[]")
        }
        return VerifiedOfflinePackage(manifest,record.authorizedUserId,tables)
    }

    companion object {
        val requiredTables=setOf(
            "organizations","inventory_campaigns","campaign_scopes","campaign_team_assignments",
            "campaign_snapshots","campaign_snapshot_scope_items","campaign_snapshot_team_items","campaign_snapshot_commodity_items",
            "locations","storage_units","storage_spaces","geometry_profiles","geometry_profile_versions","geometry_families",
            "commodities","commodity_versions","organization_memberships","users","clients","stocktaking_records",
            "stocktaking_revisions","measurements","calculation_results","issues","issue_resolutions","issue_status_events","evidence"
        )
        private fun hex(bytes: ByteArray)=bytes.joinToString("") { "%02x".format(it) }
    }
}
