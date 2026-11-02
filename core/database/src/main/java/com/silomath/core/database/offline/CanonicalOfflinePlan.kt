package com.silomath.core.database.offline

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

/** Validated explicit row values in memory; never a JSON business-row cache or a READY assertion. */
class CanonicalOfflinePlan private constructor(val rows: Map<String,List<Map<String,Any?>>>) {
    companion object {
        fun validate(input: Map<String,List<Map<String,Any?>>>): CanonicalOfflinePlan {
            require(input.keys==CanonicalOfflineSchema.tables.keys) { "Incomplete canonical table inventory" }
            val normalized=input.mapValues { (name,rows) ->
                val table=CanonicalOfflineSchema.tables.getValue(name)
                val fields=table.columns.associateBy { it.name }
                val result=rows.map { row ->
                    require(fields.keys.containsAll(row.keys)) { "Unsupported canonical field: $name" }
                    fields.mapValues { (key,column) ->
                        val value=row[key]
                        if(value==null) {
                            require(!column.required) { "Required canonical field: $name.$key" }
                            null
                        } else when(column.kind) {
                            CanonicalColumnKind.UUID -> {
                                require(value is String && UUID.fromString(value).toString()==value) { "Noncanonical UUID: $name.$key" };value
                            }
                            CanonicalColumnKind.DECIMAL -> {
                                require(value is String) { "Decimal must not pass through floating point" }
                                val decimal=BigDecimal(value).setScale(6,RoundingMode.UNNECESSARY)
                                require(decimal.precision()<=20) { "Canonical decimal precision exceeded" }
                                decimal.toPlainString()
                            }
                            CanonicalColumnKind.INTEGER,CanonicalColumnKind.INSTANT -> {
                                require(value is Long) { "Canonical integer/instant required: $name.$key" }
                                if(key in setOf("entity_version","version_number","revision_number","sequence_number")) require(value>=1)
                                value
                            }
                            CanonicalColumnKind.BOOLEAN -> { require(value is Boolean);value }
                            CanonicalColumnKind.DATE -> { require(value is String && LocalDate.parse(value).toString()==value);value }
                            CanonicalColumnKind.TEXT,CanonicalColumnKind.JSON -> { require(value is String);value }
                        }
                    }
                }
                require(result.map { it[table.primaryKey] }.distinct().size==result.size) { "Duplicate canonical identity: $name" }
                result
            }
            for(relation in CanonicalOfflineRelations.all) {
                val parents=normalized.getValue(relation.parent).map { row -> relation.parentColumns.map { row[it] } }.toSet()
                normalized.getValue(relation.child).forEach { row ->
                    val child=relation.childColumns.map { row[it] }
                    if(child.none { it==null }) require(child in parents) { "Missing/cross-tenant canonical parent: ${relation.child} -> ${relation.parent}" }
                }
            }
            val userIds=normalized.getValue("users").map { it["user_id"] }.toSet()
            normalized.forEach { (table,rows) -> rows.forEach { row ->
                row.filterKeys { it.endsWith("_by") }.values.filterNotNull().forEach {
                    require(it in userIds) { "Missing historical actor identity: $table" }
                }
            }}
            validateStates(normalized)
            return CanonicalOfflinePlan(normalized)
        }

        private fun validateStates(tables: Map<String,List<Map<String,Any?>>>) {
            tables.forEach { (table,rows) -> rows.forEach { row ->
                row["lifecycle_status"]?.let {
                    val allowed=if(table in setOf("users","organization_memberships")) setOf("ACTIVE","DISABLED") else setOf("ACTIVE","INACTIVE")
                    require(it in allowed) { "Unsupported lifecycle: $table" }
                }
                row["role"]?.let { require(it in setOf("ADMIN","STANDARD")) }
                row["status"]?.let {
                    val allowed=when(table) {
                        "inventory_campaigns" -> setOf("ACTIVE","COMPLETED")
                        "campaign_team_assignments" -> setOf("ACTIVE","REMOVED")
                        "issues" -> setOf("OPEN","RESOLVED")
                        else -> error("Unexpected status column")
                    }
                    require(it in allowed) { "Unsupported canonical state: $table" }
                }
                row["revision_state"]?.let { require(it in setOf("WORKING","VALIDATED")) }
                row["ownership"]?.let { require(it in setOf("OWNED","THIRD_PARTY")) }
                row["inventory_type"]?.let { require(it in setOf("ANNUAL","MONTHLY","CONTROL")) }
                row["evidence_type"]?.let { require(it in setOf("PHOTO","NOTE")) }
                row["sync_state"]?.let { require(it in setOf("LOCAL_PENDING","UPLOADING","SYNCED","SYNC_ERROR")) }
            }}
        }
    }
}
