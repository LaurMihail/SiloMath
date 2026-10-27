package com.silomath.core.sync.offline

import com.silomath.core.data.network.dto.OfflineManifestDto
import com.silomath.core.database.offline.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class OfflineImportPlannerTest {
    private val ids=CanonicalOfflineSchema.tables.keys.sorted().mapIndexed { index,table ->
        table to "00000000-0000-0000-0000-"+"%012d".format(index+1)
    }.toMap()
    private val user get()=ids.getValue("users")
    private val org get()=ids.getValue("organizations")
    private val campaign get()=ids.getValue("inventory_campaigns")
    private val device get()=ids.getValue("device_installations")
    private fun fixture(edit: (MutableMap<String,MutableMap<String,JsonElement>>)->Unit={}): VerifiedOfflinePackage {
        val source=CanonicalOfflineSchema.tables.mapValues { (table,definition) ->
            definition.columns.filter { it.required }.associate { column ->
                val value: JsonElement=when(column.kind) {
                    CanonicalColumnKind.UUID -> JsonPrimitive(if(column.name==definition.primaryKey) ids.getValue(table) else user)
                    CanonicalColumnKind.INTEGER -> JsonPrimitive(1)
                    CanonicalColumnKind.BOOLEAN -> JsonPrimitive(false)
                    CanonicalColumnKind.DECIMAL -> JsonPrimitive("1.000001")
                    CanonicalColumnKind.INSTANT -> JsonPrimitive("2026-10-27T00:00:00Z")
                    CanonicalColumnKind.DATE -> JsonPrimitive("2026-10-27")
                    CanonicalColumnKind.JSON -> if(column.name=="calculation_schema_versions") buildJsonArray { add("LEVEL_BASED_V1") } else buildJsonObject {}
                    CanonicalColumnKind.TEXT -> JsonPrimitive(when(column.name) {
                        "lifecycle_status" -> "ACTIVE"
                        "role","role_at_assignment" -> "STANDARD"
                        "status" -> if(table=="issues") "OPEN" else "ACTIVE"
                        "revision_state" -> "WORKING"
                        "inventory_type" -> "CONTROL"
                        "evidence_type" -> "NOTE"
                        "sync_state" -> "SYNCED"
                        "geometry_family_code","family_code" -> "CYL_FLAT_V1"
                        "formula_version","geometry_formula_version" -> "formula-v1"
                        "snapshot_schema_version","schema_version","input_schema_version" -> "schema-v1"
                        "calculation_engine_version","engine_version" -> "engine-v1"
                        else -> "Test"
                    })
                }
                column.name to value
            }.toMutableMap()
        }.toMutableMap()
        CanonicalOfflineRelations.all.forEach { relation ->
            val parent=CanonicalOfflineSchema.tables.getValue(relation.parent)
            relation.childColumns.zip(relation.parentColumns).forEach { (child,parentColumn) ->
                source.getValue(relation.child)[child]=if(parentColumn==parent.primaryKey) JsonPrimitive(ids.getValue(relation.parent)) else JsonPrimitive(org)
            }
        }
        source.getValue("stocktaking_revisions")["based_on_revision_id"]=JsonNull
        source.getValue("evidence")["issue_id"]=JsonNull
        source.getValue("inventory_campaigns")["code"]=JsonPrimitive("INV-2026-0001")
        source.getValue("locations")["latitude_deg"]=JsonPrimitive("45.123456")
        source.getValue("locations")["longitude_deg"]=JsonPrimitive("26.123456")
        edit(source)
        val manifest=OfflineManifestDto("00000000-0000-0000-0000-000000009999",org,campaign,device,"version-1",
            buildJsonObject {},"a".repeat(64),"SHA-256",buildJsonObject {},"2026-10-27T00:00:00Z")
        return VerifiedOfflinePackage(manifest,user,source.mapValues { JsonArray(listOf(JsonObject(it.value))) })
    }

    @Test fun completeStagingKeepsExactDecimalsAndServerMetadata() {
        val staged=OfflineImportPlanner.prepare(fixture())
        val location=staged.plan.rows.getValue("locations").single()
        assertEquals("45.123456",location["latitude_deg"])
        assertEquals("26.123456",location["longitude_deg"])
        assertEquals(1793059200000L,location["created_at"])
        assertEquals(1L,location["entity_version"])
        assertEquals(user,location["created_by"])
        assertFalse(staged.plan.rows.containsKey("offline_working_set_manifests"))
    }
    @Test fun crossTenantParentIsRejectedBeforeAnyImport() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineImportPlanner.prepare(fixture { it.getValue("locations")["organization_id"]=JsonPrimitive("00000000-0000-0000-0000-000000000999") })
        }
    }
    @Test fun missingPinnedVersionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineImportPlanner.prepare(fixture { it.getValue("measurements")["geometry_profile_version_id"]=JsonPrimitive("00000000-0000-0000-0000-000000000999") })
        }
    }
    @Test fun officialValuesAreNotRoundedDuringImport() {
        assertThrows(ArithmeticException::class.java) {
            OfflineImportPlanner.prepare(fixture { it.getValue("measurements")["normalized_ullage_m"]=JsonPrimitive("1.0000001") })
        }
    }
    @Test fun disabledUserCannotSupplyOfflineAuthority() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineImportPlanner.prepare(fixture { it.getValue("users")["lifecycle_status"]=JsonPrimitive("DISABLED") })
        }
    }
    @Test fun revokedInstallationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineImportPlanner.prepare(fixture { it.getValue("device_installations")["revoked_at"]=JsonPrimitive("2026-10-27T00:00:00Z") })
        }
    }
    @Test fun incompleteCommodityCompatibilityCannotBecomeCanonicalAuthority() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineImportPlanner.prepare(fixture { it.getValue("campaign_snapshot_commodity_items")["commodity_version_id"]=JsonNull })
        }
    }
    @Test fun missingFrozenFieldIsNotFilledFromLegacyOrCurrentMasterData() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineImportPlanner.prepare(fixture { it.getValue("campaign_snapshot_scope_items").remove("geometry_dimensions") })
        }
    }
}
