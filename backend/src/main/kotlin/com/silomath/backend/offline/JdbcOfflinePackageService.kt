package com.silomath.backend.offline

import java.sql.Connection
import java.sql.DriverManager
import java.nio.file.Path
import java.time.Instant
import java.util.Properties
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** One repeatable-read snapshot produces both bytes and the immutable manifest. */
internal class JdbcOfflinePackageService(
    private val databaseUrl: String,
    private val databaseUser: String?,
    private val databasePassword: String?,
    directory: Path
) : OfflineManifestService, OfflinePackageService {
    private val files = OfflinePackageFiles(directory)
    private val manifests = JdbcOfflineManifestService(databaseUrl, databaseUser, databasePassword)

    override suspend fun current(userId: String, organizationId: String, campaignId: String,
        deviceInstallationId: String, packageVersion: String?): OfflineManifestResult = withContext(Dispatchers.IO) {
        // Explicit versions select existing immutable artifacts; no silent replacement.
        if (packageVersion != null) return@withContext manifests.current(
            userId, organizationId, campaignId, deviceInstallationId, packageVersion)
        open().use { connection ->
            connection.autoCommit = false
            connection.transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ
            try {
                if (!authorized(connection, userId, organizationId, campaignId)) {
                    return@withContext OfflineManifestResult.Forbidden
                }
                // An installation is a privacy-safe UUID bound to the authenticated user.
                connection.prepareStatement("""
                    insert into silomath.device_installations(device_installation_id,user_id,created_at,last_authorized_at)
                    values (?::uuid,?::uuid,now(),now()) on conflict (device_installation_id) do nothing
                """.trimIndent()).use { statement ->
                    statement.setString(1, deviceInstallationId); statement.setString(2, userId); statement.executeUpdate()
                }
                connection.prepareStatement("""
                    select 1 from silomath.device_installations
                    where device_installation_id=?::uuid and user_id=?::uuid and revoked_at is null for update
                """.trimIndent()).use { statement ->
                    statement.setString(1, deviceInstallationId); statement.setString(2, userId)
                    statement.executeQuery().use { if (!it.next()) return@withContext OfflineManifestResult.Forbidden }
                }
                val tables = linkedMapOf<String, JsonArray>()
                for ((table, predicate) in selections) {
                    val projection = if (table == "evidence") "to_jsonb(t) - 'local_private_ref'" else "to_jsonb(t)"
                    connection.prepareStatement("""
                        with context as (select ?::uuid as campaign_id, ?::uuid as organization_id, ?::uuid as user_id, ?::uuid as device_id),
                        scope as (select s.storage_space_id from silomath.campaign_scopes s, context c where s.campaign_id=c.campaign_id),
                        frozen_scope as (select s.* from silomath.campaign_snapshot_scope_items s
                          join silomath.campaign_snapshots h on h.campaign_snapshot_id=s.campaign_snapshot_id,
                          context c where h.campaign_id=c.campaign_id)
                        select coalesce(jsonb_agg(payload order by payload::text), '[]'::jsonb)::text
                        from (select $projection as payload from silomath.$table t, context c where $predicate) selected
                    """.trimIndent()).use { statement ->
                        statement.setString(1, campaignId); statement.setString(2, organizationId); statement.setString(3, userId); statement.setString(4, deviceInstallationId)
                        statement.executeQuery().use { rows ->
                            check(rows.next())
                            tables[table] = Json.parseToJsonElement(rows.getString(1)).jsonArray
                        }
                    }
                }
                completeReferencedContext(connection,tables,organizationId,campaignId)
                val id = UUID.randomUUID().toString()
                val artifact = OfflinePackageEncoder.encode(OfflinePackageEnvelope(
                    manifestId=id, organizationId=organizationId, campaignId=campaignId,
                    deviceInstallationId=deviceInstallationId, authorizedUserId=userId,
                    packageVersion=UUID.randomUUID().toString(), generatedAt=Instant.now().toString(), tables=tables
                ))
                // File is durable before its manifest can become discoverable. Rollback leaves only an unreferenced artifact.
                files.write(id, artifact.bytes)
                connection.prepareStatement("""
                    update silomath.offline_working_set_manifests set is_current=false
                    where campaign_id=?::uuid and device_installation_id=?::uuid and is_current
                """.trimIndent()).use { statement ->
                    statement.setString(1,campaignId);statement.setString(2,deviceInstallationId);statement.executeUpdate()
                }
                val m=artifact.manifest
                connection.prepareStatement("""
                    insert into silomath.offline_working_set_manifests
                    (manifest_id,campaign_id,organization_id,device_installation_id,package_version,content_manifest,
                     integrity_hash,integrity_algorithm,verification_metadata,generated_at,is_current)
                    values (?::uuid,?::uuid,?::uuid,?::uuid,?,?::jsonb,?,?,?::jsonb,?::timestamptz,true)
                """.trimIndent()).use { statement ->
                    listOf(m.manifestId,m.campaignId,m.organizationId,m.deviceInstallationId,m.packageVersion,
                        m.contentManifest.toString(),m.integrityHash,m.integrityAlgorithm,m.verificationMetadata.toString(),m.generatedAt)
                        .forEachIndexed { i,v -> statement.setString(i+1,v) }
                    statement.executeUpdate()
                }
                connection.commit()
                OfflineManifestResult.Found(m)
            } finally {
                if (!connection.autoCommit) connection.rollback()
            }
        }
    }

    override suspend fun download(userId: String, organizationId: String, campaignId: String,
        deviceInstallationId: String, packageVersion: String, manifestId: String): OfflinePackageResult = withContext(Dispatchers.IO) {
        when(val result=manifests.current(userId,organizationId,campaignId,deviceInstallationId,packageVersion)) {
            OfflineManifestResult.Forbidden -> OfflinePackageResult.Forbidden
            OfflineManifestResult.NotFound -> OfflinePackageResult.NotFound
            is OfflineManifestResult.Found -> {
                if (result.manifest.manifestId != manifestId) return@withContext OfflinePackageResult.NotFound
                val bytes=files.read(manifestId)
                check(OfflinePackageEncoder.sha256(bytes)==result.manifest.integrityHash) { "Package storage integrity failure" }
                OfflinePackageResult.Found(MaterializedOfflinePackage(result.manifest,bytes))
            }
        }
    }

    private fun open(): Connection = DriverManager.getConnection(databaseUrl, Properties().apply {
        databaseUser?.let { setProperty("user",it) }; databasePassword?.let { setProperty("password",it) }
    })

    private fun authorized(connection: Connection,user: String,org: String,campaign: String): Boolean =
        connection.prepareStatement("""
            select 1 from silomath.inventory_campaigns c
            join silomath.organizations o on o.organization_id=c.organization_id
            join silomath.organization_memberships m on m.organization_id=c.organization_id
            join silomath.users u on u.user_id=m.user_id
            where c.campaign_id=?::uuid and c.organization_id=?::uuid and m.user_id=?::uuid
              and o.lifecycle_status='ACTIVE' and m.lifecycle_status='ACTIVE' and u.lifecycle_status='ACTIVE'
              and (m.role='ADMIN' or exists(select 1 from silomath.campaign_team_assignments t
                  where t.campaign_id=c.campaign_id and t.user_id=m.user_id and t.status='ACTIVE'))
        """.trimIndent()).use { statement ->
            statement.setString(1,campaign);statement.setString(2,org);statement.setString(3,user)
            statement.executeQuery().use { it.next() }
        }

    private fun completeReferencedContext(connection: Connection,tables: MutableMap<String,JsonArray>,organizationId: String,campaignId: String) {
        val rows=tables.mapValues { (table,items) ->
            items.associateBy { it.jsonObject.getValue(primaryKeys.getValue(table)).jsonPrimitive.content }.toMutableMap()
        }.toMutableMap()
        fun checkScope(row: JsonObject) {
            row["organization_id"]?.takeIf { it!=JsonNull }?.let { check(it.jsonPrimitive.content==organizationId) { "Cross-tenant package reference" } }
            row["campaign_id"]?.takeIf { it!=JsonNull }?.let { check(it.jsonPrimitive.content==campaignId) { "Cross-campaign package reference" } }
        }
        rows.values.forEach { items -> items.values.forEach { checkScope(it.jsonObject) } }
        var changed: Boolean
        do {
            changed=false
            val requested=mutableMapOf<String,MutableSet<String>>()
            for((child,column,parent) in referenceEdges) {
                rows.getValue(child).values.forEach { value ->
                    value.jsonObject[column]?.takeIf { it!=JsonNull }?.let {
                        val id=it.jsonPrimitive.content
                        if(id !in rows.getValue(parent)) requested.getOrPut(parent) { mutableSetOf() }.add(id)
                    }
                }
            }
            rows.values.forEach { items -> items.values.forEach { value ->
                value.jsonObject.filterKeys { it.endsWith("_by") }.values.filter { it!=JsonNull }.forEach {
                    val id=it.jsonPrimitive.content
                    if(id !in rows.getValue("users")) requested.getOrPut("users") { mutableSetOf() }.add(id)
                }
            }}
            for((table,ids) in requested) {
                // Bounded parameter batches; relation/table/PK names come only from the closed inventory below.
                for(batch in ids.toList().chunked(200)) {
                    val placeholders=batch.joinToString(",") { "?::uuid" }
                    val pk=primaryKeys.getValue(table)
                    val projection=if(table=="evidence") "to_jsonb(t)-'local_private_ref'" else "to_jsonb(t)"
                    connection.prepareStatement("SELECT ($projection)::text FROM silomath.$table t WHERE $pk IN ($placeholders)").use { statement ->
                        batch.forEachIndexed { i,id -> statement.setString(i+1,id) }
                        statement.executeQuery().use { result -> while(result.next()) {
                            val value=Json.parseToJsonElement(result.getString(1)).jsonObject
                            checkScope(value)
                            rows.getValue(table)[value.getValue(pk).jsonPrimitive.content]=value
                            changed=true
                        }}
                    }
                    check(batch.all { it in rows.getValue(table) }) { "Missing authoritative package reference" }
                }
            }
        } while(changed)
        rows.forEach { (table,items) -> tables[table]=JsonArray(items.toSortedMap().values.toList()) }
    }

    companion object {
        private val primaryKeys=mapOf(
            "calculation_results" to "calculation_result_id",
            "campaign_scopes" to "campaign_scope_id",
            "campaign_snapshot_commodity_items" to "snapshot_commodity_item_id",
            "campaign_snapshot_scope_items" to "snapshot_scope_item_id",
            "campaign_snapshot_team_items" to "snapshot_team_item_id",
            "campaign_snapshots" to "campaign_snapshot_id",
            "campaign_team_assignments" to "assignment_id",
            "clients" to "client_id",
            "commodities" to "commodity_id",
            "commodity_versions" to "commodity_version_id",
            "device_installations" to "device_installation_id",
            "evidence" to "evidence_id",
            "geometry_families" to "geometry_family_id",
            "geometry_profile_versions" to "geometry_profile_version_id",
            "geometry_profiles" to "geometry_profile_id",
            "inventory_campaigns" to "campaign_id",
            "issue_resolutions" to "issue_resolution_id",
            "issue_status_events" to "issue_status_event_id",
            "issues" to "issue_id",
            "locations" to "location_id",
            "measurements" to "measurement_id",
            "organization_memberships" to "membership_id",
            "organizations" to "organization_id",
            "stocktaking_records" to "stocktaking_record_id",
            "stocktaking_revisions" to "stocktaking_revision_id",
            "storage_spaces" to "storage_space_id",
            "storage_units" to "storage_unit_id",
            "users" to "user_id"
        )
        private val referenceEdges=listOf(
            Triple("calculation_results","stocktaking_revision_id","stocktaking_revisions"),
            Triple("calculation_results","geometry_family_id","geometry_families"),
            Triple("calculation_results","geometry_profile_version_id","geometry_profile_versions"),
            Triple("campaign_scopes","campaign_id","inventory_campaigns"),
            Triple("campaign_scopes","storage_space_id","storage_spaces"),
            Triple("campaign_snapshot_commodity_items","campaign_snapshot_id","campaign_snapshots"),
            Triple("campaign_snapshot_commodity_items","commodity_id","commodities"),
            Triple("campaign_snapshot_commodity_items","commodity_version_id","commodity_versions"),
            Triple("campaign_snapshot_scope_items","campaign_snapshot_id","campaign_snapshots"),
            Triple("campaign_snapshot_scope_items","storage_space_id","storage_spaces"),
            Triple("campaign_snapshot_scope_items","location_id","locations"),
            Triple("campaign_snapshot_scope_items","storage_unit_id","storage_units"),
            Triple("campaign_snapshot_scope_items","geometry_family_id","geometry_families"),
            Triple("campaign_snapshot_scope_items","geometry_profile_id","geometry_profiles"),
            Triple("campaign_snapshot_scope_items","geometry_profile_version_id","geometry_profile_versions"),
            Triple("campaign_snapshot_team_items","campaign_snapshot_id","campaign_snapshots"),
            Triple("campaign_snapshot_team_items","user_id","users"),
            Triple("campaign_snapshot_team_items","organization_membership_id","organization_memberships"),
            Triple("campaign_snapshots","campaign_id","inventory_campaigns"),
            Triple("campaign_snapshots","organization_id","organizations"),
            Triple("campaign_team_assignments","campaign_id","inventory_campaigns"),
            Triple("campaign_team_assignments","user_id","users"),
            Triple("clients","organization_id","organizations"),
            Triple("commodities","current_version_id","commodity_versions"),
            Triple("commodity_versions","commodity_id","commodities"),
            Triple("device_installations","user_id","users"),
            Triple("evidence","organization_id","organizations"),
            Triple("evidence","campaign_id","inventory_campaigns"),
            Triple("evidence","stocktaking_record_id","stocktaking_records"),
            Triple("evidence","issue_id","issues"),
            Triple("evidence","stocktaking_revision_id","stocktaking_revisions"),
            Triple("geometry_profile_versions","geometry_profile_id","geometry_profiles"),
            Triple("geometry_profile_versions","geometry_family_id","geometry_families"),
            Triple("geometry_profiles","organization_id","organizations"),
            Triple("geometry_profiles","geometry_family_id","geometry_families"),
            Triple("geometry_profiles","current_version_id","geometry_profile_versions"),
            Triple("inventory_campaigns","organization_id","organizations"),
            Triple("inventory_campaigns","campaign_snapshot_id","campaign_snapshots"),
            Triple("issue_resolutions","issue_id","issues"),
            Triple("issue_status_events","issue_id","issues"),
            Triple("issues","organization_id","organizations"),
            Triple("issues","campaign_id","inventory_campaigns"),
            Triple("issues","stocktaking_record_id","stocktaking_records"),
            Triple("locations","organization_id","organizations"),
            Triple("measurements","stocktaking_revision_id","stocktaking_revisions"),
            Triple("measurements","geometry_profile_version_id","geometry_profile_versions"),
            Triple("organization_memberships","organization_id","organizations"),
            Triple("organization_memberships","user_id","users"),
            Triple("stocktaking_records","organization_id","organizations"),
            Triple("stocktaking_records","campaign_id","inventory_campaigns"),
            Triple("stocktaking_records","storage_space_id","storage_spaces"),
            Triple("stocktaking_records","current_revision_id","stocktaking_revisions"),
            Triple("stocktaking_revisions","stocktaking_record_id","stocktaking_records"),
            Triple("stocktaking_revisions","based_on_revision_id","stocktaking_revisions"),
            Triple("stocktaking_revisions","commodity_version_id","commodity_versions"),
            Triple("stocktaking_revisions","client_id","clients"),
            Triple("stocktaking_revisions","calculation_result_id","calculation_results"),
            Triple("storage_spaces","organization_id","organizations"),
            Triple("storage_spaces","storage_unit_id","storage_units"),
            Triple("storage_spaces","geometry_profile_id","geometry_profiles"),
            Triple("storage_units","organization_id","organizations"),
            Triple("storage_units","location_id","locations")
        )

        // Closed SQL inventory. Request values are bound parameters, never SQL fragments.
        private val selections = linkedMapOf(
            "device_installations" to "t.device_installation_id=c.device_id and t.user_id=c.user_id and t.revoked_at is null",
            "organizations" to "t.organization_id=c.organization_id",
            "inventory_campaigns" to "t.campaign_id=c.campaign_id and t.organization_id=c.organization_id",
            "campaign_scopes" to "t.campaign_id=c.campaign_id",
            "campaign_team_assignments" to "t.campaign_id=c.campaign_id",
            "campaign_snapshots" to "t.campaign_id=c.campaign_id and t.organization_id=c.organization_id",
            "campaign_snapshot_scope_items" to "t.campaign_snapshot_id in (select h.campaign_snapshot_id from silomath.campaign_snapshots h where h.campaign_id=c.campaign_id)",
            "campaign_snapshot_team_items" to "t.campaign_snapshot_id in (select h.campaign_snapshot_id from silomath.campaign_snapshots h where h.campaign_id=c.campaign_id)",
            "campaign_snapshot_commodity_items" to "t.campaign_snapshot_id in (select h.campaign_snapshot_id from silomath.campaign_snapshots h where h.campaign_id=c.campaign_id)",
            "locations" to "t.organization_id=c.organization_id and t.location_id in (select location_id from frozen_scope)",
            "storage_units" to "t.organization_id=c.organization_id and t.storage_unit_id in (select storage_unit_id from frozen_scope)",
            "storage_spaces" to "t.organization_id=c.organization_id and t.storage_space_id in (select storage_space_id from scope)",
            "geometry_profiles" to "t.organization_id=c.organization_id and t.geometry_profile_id in (select geometry_profile_id from frozen_scope)",
            "geometry_profile_versions" to "t.geometry_profile_version_id in (select geometry_profile_version_id from frozen_scope)",
            "geometry_families" to "t.geometry_family_id in (select geometry_family_id from frozen_scope)",
            "commodity_versions" to "t.commodity_version_id in (select i.commodity_version_id from silomath.campaign_snapshot_commodity_items i join silomath.campaign_snapshots h on h.campaign_snapshot_id=i.campaign_snapshot_id where h.campaign_id=c.campaign_id)",
            "commodities" to "t.commodity_id in (select i.commodity_id from silomath.campaign_snapshot_commodity_items i join silomath.campaign_snapshots h on h.campaign_snapshot_id=i.campaign_snapshot_id where h.campaign_id=c.campaign_id)",
            "organization_memberships" to "t.organization_id=c.organization_id and (t.user_id=c.user_id or t.user_id in (select user_id from silomath.campaign_team_assignments a where a.campaign_id=c.campaign_id))",
            "users" to "t.user_id=c.user_id or t.user_id in (select user_id from silomath.campaign_team_assignments a where a.campaign_id=c.campaign_id)",
            "clients" to "t.organization_id=c.organization_id",
            "stocktaking_records" to "t.campaign_id=c.campaign_id and t.organization_id=c.organization_id and t.storage_space_id in (select storage_space_id from scope)",
            "stocktaking_revisions" to "t.stocktaking_record_id in (select r.stocktaking_record_id from silomath.stocktaking_records r where r.campaign_id=c.campaign_id)",
            "measurements" to "t.stocktaking_revision_id in (select v.stocktaking_revision_id from silomath.stocktaking_revisions v join silomath.stocktaking_records r on r.stocktaking_record_id=v.stocktaking_record_id where r.campaign_id=c.campaign_id)",
            "calculation_results" to "t.stocktaking_revision_id in (select v.stocktaking_revision_id from silomath.stocktaking_revisions v join silomath.stocktaking_records r on r.stocktaking_record_id=v.stocktaking_record_id where r.campaign_id=c.campaign_id)",
            "issues" to "t.campaign_id=c.campaign_id and t.organization_id=c.organization_id",
            "issue_resolutions" to "t.issue_id in (select issue_id from silomath.issues i where i.campaign_id=c.campaign_id)",
            "issue_status_events" to "t.issue_id in (select issue_id from silomath.issues i where i.campaign_id=c.campaign_id)",
            "evidence" to "t.campaign_id=c.campaign_id and t.organization_id=c.organization_id"
        )
    }
}
