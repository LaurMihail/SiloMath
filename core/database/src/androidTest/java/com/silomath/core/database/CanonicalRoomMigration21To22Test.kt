package com.silomath.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CanonicalRoomMigration21To22Test {

    companion object {
        private const val SOURCE_VERSION = 21
        private const val DB_NAME = "canonical_migration_21_22_test.db"

        private val REQUIRED_V6_TABLES = listOf(
            "commodity_versions",
            "campaign_snapshots",
            "campaign_snapshot_scope_items",
            "campaign_snapshot_team_items",
            "campaign_snapshot_commodity_items",
            "offline_working_set_manifests",
            "stocktaking_revisions",
            "issue_resolutions",
            "issue_status_events",
            "evidence",
            "final_inventory_snapshots",
            "final_inventory_snapshot_entries",
            "sync_operation_dependencies",
            "sync_conflicts",
            "audit_events",
            "notifications",
            "user_preferences",
            "organization_configurations"
        )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        SiloMathDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun cleanup() {
        instrumentation.targetContext.deleteDatabase(DB_NAME)
    }

    @Test
    fun migration21To22_addsCanonicalV6RoomBaselineWithoutDroppingLegacyTables() {
        var source = helper.createDatabase(DB_NAME, SOURCE_VERSION)
        source.execSQL(
            "INSERT INTO organizations (id, name) VALUES (9001, 'Retained legacy organization')"
        )
        source.close()

        val room = Room.databaseBuilder(
            instrumentation.targetContext,
            SiloMathDatabase::class.java,
            DB_NAME
        ).build()

        val db = room.openHelper.writableDatabase

        REQUIRED_V6_TABLES.forEach { table ->
            assertTrue("Schema 22 must contain $table", tableExists(db, table))
        }

        // EXPAND is non-destructive: transitional legacy tables remain until
        // their explicit CONTRACT migration.
        assertTrue(tableExists(db, "organizations"))
        db.query("SELECT name FROM organizations WHERE id = 9001").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Retained legacy organization", cursor.getString(0))
        }

        room.close()
    }

    private fun tableExists(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        table: String
    ): Boolean = db.query(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
        arrayOf(table)
    ).use { cursor -> cursor.moveToFirst() }
}
