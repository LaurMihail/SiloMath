package com.silomath.core.database.offline

import androidx.sqlite.db.SupportSQLiteDatabase

/** Constraints that Room annotations cannot express; no legacy table is touched. */
object CanonicalOfflineGuards {
    fun install(db: SupportSQLiteDatabase) {
        for (table in CanonicalOfflineSchema.tables.values) {
            val checks=table.columns.mapNotNull { column ->
                val value="NEW.`${column.name}`"
                when(column.kind) {
                    CanonicalColumnKind.UUID -> "$value IS NOT NULL AND (length($value) != 36 OR $value != lower($value) OR $value NOT LIKE '________-____-____-____-____________' OR $value GLOB '*[^0-9a-f-]*')"
                    CanonicalColumnKind.DECIMAL -> "$value IS NOT NULL AND (typeof($value) != 'text' OR $value GLOB '*[^0-9.-]*' OR instr($value,'.') < 2 OR length($value)-instr($value,'.') != 6 OR length($value)-length(replace($value,'.','')) != 1 OR instr(substr($value,2),'-') > 0 OR length($value)-length(replace($value,'-','')) > 1)"
                    CanonicalColumnKind.BOOLEAN -> "$value IS NOT NULL AND $value NOT IN (0,1)"
                    CanonicalColumnKind.INTEGER -> if(column.name in setOf("entity_version","version_number","revision_number","sequence_number")) "$value IS NOT NULL AND $value < 1" else null
                    else -> null
                }
            }
            for(action in listOf("INSERT","UPDATE")) {
                if(checks.isNotEmpty()) db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS guard_${table.localName}_${action.lowercase()}
                    BEFORE $action ON ${table.localName}
                    WHEN ${checks.joinToString(" OR ") { "($it)" }}
                    BEGIN SELECT RAISE(ABORT,'V6_CANONICAL_VALUE_INVALID'); END
                """.trimIndent())
            }
            if("entity_version" in table.columns.map { it.name }) db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS version_${table.localName}
                BEFORE UPDATE ON ${table.localName} WHEN NEW.entity_version < OLD.entity_version
                BEGIN SELECT RAISE(ABORT,'V6_ENTITY_VERSION_REGRESSION'); END
            """.trimIndent())
            if(table.sourceName in immutableTables) {
                for(action in listOf("UPDATE","DELETE")) db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS immutable_${table.localName}_${action.lowercase()}
                    BEFORE $action ON ${table.localName}
                    BEGIN SELECT RAISE(ABORT,'V6_IMMUTABLE_CONTEXT'); END
                """.trimIndent())
            }
        }
        for(action in listOf("INSERT","UPDATE")) db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS active_team_${action.lowercase()}
            BEFORE $action ON v6_campaign_team_assignments
            WHEN NEW.status='ACTIVE' AND EXISTS (
                SELECT 1 FROM v6_campaign_team_assignments t
                WHERE t.campaign_id=NEW.campaign_id AND t.user_id=NEW.user_id
                  AND t.status='ACTIVE' AND t.assignment_id != NEW.assignment_id)
            BEGIN SELECT RAISE(ABORT,'V6_DUPLICATE_ACTIVE_TEAM_ASSIGNMENT'); END
        """.trimIndent())
        for(action in listOf("UPDATE","DELETE")) {
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS validated_revision_${action.lowercase()}
                BEFORE $action ON v6_stocktaking_revisions WHEN OLD.revision_state='VALIDATED'
                BEGIN SELECT RAISE(ABORT,'V6_VALIDATED_REVISION_IMMUTABLE'); END
            """.trimIndent())
            for(table in listOf("measurements","calculation_results","evidence")) db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS validated_${table}_${action.lowercase()}
                BEFORE $action ON v6_$table
                WHEN EXISTS (SELECT 1 FROM v6_stocktaking_revisions r
                    WHERE r.stocktaking_revision_id=OLD.stocktaking_revision_id AND r.revision_state='VALIDATED')
                BEGIN SELECT RAISE(ABORT,'V6_VALIDATED_CONTENT_IMMUTABLE'); END
            """.trimIndent())
        }
    }

    val immutableTables=setOf("geometry_families","geometry_profile_versions","commodity_versions",
        "campaign_snapshots","campaign_snapshot_scope_items","campaign_snapshot_team_items",
        "campaign_snapshot_commodity_items","issue_resolutions","issue_status_events")
}
