package com.silomath.core.sync.offline

import com.silomath.core.database.offline.CanonicalColumnKind
import com.silomath.core.database.offline.CanonicalOfflinePlan
import com.silomath.core.database.offline.CanonicalOfflineSchema
import java.time.Instant
import kotlinx.serialization.json.*

class StagedOfflineImport internal constructor(val verified: VerifiedOfflinePackage,val plan: CanonicalOfflinePlan)

/** Preparing a graph never changes canonical rows or the current manifest. */
object OfflineImportPlanner {
    fun prepare(verified: VerifiedOfflinePackage): StagedOfflineImport {
        require(verified.tables.keys==CanonicalOfflineSchema.tables.keys) { "Canonical package schema is incomplete" }
        val input=verified.tables.mapValues { (name,rows) ->
            val columns=CanonicalOfflineSchema.tables.getValue(name).columns.associateBy { it.name }
            rows.map { element ->
                element.jsonObject.mapValues { (key,value) ->
                    val column=columns[key] ?: error("Unsupported package field: $name.$key")
                    if(value==JsonNull) null else when(column.kind) {
                        CanonicalColumnKind.INTEGER -> value.jsonPrimitive.long
                        CanonicalColumnKind.INSTANT -> Instant.parse(value.jsonPrimitive.content).toEpochMilli()
                        CanonicalColumnKind.BOOLEAN -> value.jsonPrimitive.boolean
                        CanonicalColumnKind.JSON -> canonicalJson(value).toString()
                        else -> value.jsonPrimitive.content
                    }
                }
            }
        }
        val plan=CanonicalOfflinePlan.validate(input)
        val org=verified.manifest.organizationId;val campaign=verified.manifest.campaignId
        plan.rows.forEach { (_,rows) -> rows.forEach { row ->
            row["organization_id"]?.let { require(it==org) { "Package crosses organization boundary" } }
            row["campaign_id"]?.let { require(it==campaign) { "Package crosses campaign boundary" } }
        }}
        require(plan.rows.getValue("organizations").single()["organization_id"]==org)
        val header=plan.rows.getValue("inventory_campaigns").single()
        val snapshot=plan.rows.getValue("campaign_snapshots").single()
        require(header["campaign_id"]==campaign && header["campaign_snapshot_id"]==snapshot["campaign_snapshot_id"])
        val scope=plan.rows.getValue("campaign_scopes").map { it["storage_space_id"] }.toSet()
        val frozen=plan.rows.getValue("campaign_snapshot_scope_items").map { it["storage_space_id"] }.toSet()
        require(scope.isNotEmpty() && scope==frozen) { "Campaign Scope differs from frozen snapshot" }
        require(plan.rows.getValue("campaign_snapshot_team_items").isNotEmpty())
        require(plan.rows.getValue("campaign_snapshot_commodity_items").isNotEmpty())
        val user=plan.rows.getValue("users").single { it["user_id"]==verified.authorizedUserId }
        require(user["lifecycle_status"]=="ACTIVE")
        require(plan.rows.getValue("organization_memberships").any {
            it["user_id"]==verified.authorizedUserId && it["organization_id"]==org && it["lifecycle_status"]=="ACTIVE"
        })
        val device=plan.rows.getValue("device_installations").single()
        require(device["device_installation_id"]==verified.manifest.deviceInstallationId &&
            device["user_id"]==verified.authorizedUserId && device["revoked_at"]==null)
        return StagedOfflineImport(verified,plan)
    }

    private fun canonicalJson(value: JsonElement): JsonElement=when(value) {
        is JsonObject -> JsonObject(value.toSortedMap().mapValues { canonicalJson(it.value) })
        is JsonArray -> JsonArray(value.map(::canonicalJson))
        else -> value
    }
}
