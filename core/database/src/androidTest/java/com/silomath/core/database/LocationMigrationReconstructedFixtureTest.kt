package com.silomath.core.database

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.di.MIGRATION_14_15
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * reconstructed / non-authoritative historical test fixture.
 * Tests only MIGRATION_14_15 behavior on the structures it uses. This is NOT an
 * authentic Room 14 export, full-schema validation, or a replacement for LocationMigrationTest.
 * The authentic schema 14 remains unavailable and its MigrationTestHelper test remains BLOCKED.
 */
@RunWith(AndroidJUnit4::class)
class LocationMigrationReconstructedFixtureTest {
    @Test fun reconstructedPre15FixturePreservesLocationsChildrenAndCanonicalNulls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // Controlled in-memory fixture; never an installed operational database.
                .callback(object : SupportSQLiteOpenHelper.Callback(14) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE organizations(id INTEGER PRIMARY KEY, organization_id TEXT)")
                        db.execSQL("""CREATE TABLE locations(
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            organization_id INTEGER NOT NULL, remoteId TEXT, code TEXT NOT NULL,
                            name TEXT NOT NULL, lifecycleStatus TEXT NOT NULL, remoteRevision INTEGER,
                            FOREIGN KEY(organization_id) REFERENCES organizations(id) ON DELETE CASCADE)""")
                        db.execSQL("""CREATE TABLE storage_units(
                            id INTEGER PRIMARY KEY, location_id INTEGER NOT NULL, code TEXT,
                            name TEXT, type TEXT, lifecycleStatus TEXT, remoteRevision INTEGER,
                            FOREIGN KEY(location_id) REFERENCES locations(id) ON DELETE CASCADE)""")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        error("This fixture invokes the approved migration explicitly")
                    }
                }).build()
        )
        try {
            val db = helper.writableDatabase
            val org = "11111111-1111-4111-8111-111111111111"
            val location = "22222222-2222-4222-8222-222222222222"
            val malformed = "gggggggg-gggg-gggg-gggg-gggggggggggg" // 36 chars, invalid hex.
            db.execSQL("INSERT INTO organizations VALUES(1, ?), (2, NULL)", arrayOf(org))
            db.execSQL("""INSERT INTO locations VALUES
                (100,1,?,'LOC-1',' Retained name ','ACTIVE',7),
                (200,2,?,'LOC-2','Location 2','INACTIVE',NULL),
                (300,1,NULL,'LOC-3','Location 3','ACTIVE',0),
                (400,2,?,'LOC-4','Location 4','ACTIVE',9)""", arrayOf(location, malformed, location))
            db.execSQL("""INSERT INTO storage_units VALUES
                (1000,100,'UNIT-1',' Retained unit ','SILO','ACTIVE',13),
                (2000,200,'UNIT-2','Unit 2','LEGACY-TYPE','INACTIVE',NULL)""")
            val legacyColumns = "id,organization_id,remoteId,code,name,lifecycleStatus,remoteRevision"
            val locationsBefore = rows(db, "SELECT $legacyColumns FROM locations ORDER BY id")
            val unitsBefore = rows(db, "SELECT * FROM storage_units ORDER BY id")
            val organizationsBefore = rows(db, "SELECT * FROM organizations ORDER BY id")
            // Table rebuilds run with FK enforcement disabled, as required for the migration;
            // retain and validate the FK declarations and re-enable enforcement afterwards.
            db.setForeignKeyConstraintsEnabled(false)
            db.beginTransaction()
            try {
                MIGRATION_14_15.migrate(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            assertEquals(locationsBefore, rows(db, "SELECT $legacyColumns FROM locations ORDER BY id"))
            assertEquals(unitsBefore, rows(db, "SELECT * FROM storage_units ORDER BY id"))
            assertEquals(organizationsBefore, rows(db, "SELECT * FROM organizations ORDER BY id"))
            assertEquals(listOf(
                listOf(100L, location, org), listOf(200L, null, null),
                listOf(300L, null, org), listOf(400L, location, null)
            ), rows(db, "SELECT id,location_id,canonical_organization_id FROM locations ORDER BY id"))
            db.query("SELECT address,city,country,notes,created_at,created_by,updated_at,updated_by,entity_version FROM locations").use { cursor ->
                var count = 0
                while (cursor.moveToNext()) {
                    count++
                    for (column in 0 until cursor.columnCount) assertTrue(cursor.isNull(column))
                }
                assertEquals(4, count)
            }
            db.query("PRAGMA foreign_key_list(storage_units)").use { cursor ->
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(2) == "locations" && cursor.getString(3) == "location_id" && cursor.getString(4) == "id") found = true
                }
                assertTrue("Preserve storage_units(location_id) -> locations(id)", found)
            }
            db.setForeignKeyConstraintsEnabled(true)
            db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
                db.execSQL("INSERT INTO storage_units(id,location_id) VALUES(3000,9999)")
            }
            assertEquals(unitsBefore, rows(db, "SELECT * FROM storage_units ORDER BY id"))
        } finally { helper.close() }
    }

    private fun rows(db: SupportSQLiteDatabase, sql: String): List<List<Any?>> = db.query(sql).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add((0 until cursor.columnCount).map { column ->
                when (cursor.getType(column)) {
                    Cursor.FIELD_TYPE_NULL -> null
                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(column)
                    else -> cursor.getString(column)
                }
            })
        }
    }
}
