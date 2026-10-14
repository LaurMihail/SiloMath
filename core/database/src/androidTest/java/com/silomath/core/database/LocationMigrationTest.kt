package com.silomath.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.common.UuidUtils
import com.silomath.core.database.di.MIGRATION_14_15
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationMigrationTest {

    private val testDb = "migration_14_15_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SiloMathDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration14To15_migratesLocationsCorrectly() {
        val validOrgUuid = UuidUtils.generateUuid()
        val validLocUuid = UuidUtils.generateUuid()
        val invalidUuid = "not-a-valid-uuid-36-chars-long-here-xyz"

        var db = helper.createDatabase(testDb, 14)
        db.execSQL("INSERT INTO organizations (id, name, organization_id, lifecycle_status) VALUES (1, 'Org 1', '$validOrgUuid', 'ACTIVE')")
        db.execSQL("INSERT INTO organizations (id, name, organization_id, lifecycle_status) VALUES (2, 'Org 2', NULL, 'ACTIVE')")

        db.execSQL("INSERT INTO locations (id, organization_id, remoteId, code, name, lifecycleStatus) VALUES (100, 1, '$validLocUuid', 'LOC-1', 'Location 1', 'ACTIVE')")
        db.execSQL("INSERT INTO locations (id, organization_id, remoteId, code, name, lifecycleStatus) VALUES (200, 2, '$invalidUuid', 'LOC-2', 'Location 2', 'ACTIVE')")

        db.execSQL("INSERT INTO storage_units (id, location_id, code, name, type, lifecycleStatus) VALUES (1000, 100, 'UNIT-1', 'Unit 1', 'SILO', 'ACTIVE')")
        db.close()

        // Run migration 14 -> 15 and validate schema version 15
        db = helper.runMigrationsAndValidate(testDb, 15, true, MIGRATION_14_15)

        // Verify total locations count = 2
        val countCursor = db.query("SELECT COUNT(*) FROM locations")
        assertTrue(countCursor.moveToFirst())
        assertEquals(2, countCursor.getInt(0))
        countCursor.close()

        // Verify PRAGMA foreign_key_list(storage_units) references locations(id)
        val pragmaCursor = db.query("PRAGMA foreign_key_list(storage_units)")
        var hasLocationFk = false
        while (pragmaCursor.moveToNext()) {
            val parentTable = pragmaCursor.getString(2)
            val fromCol = pragmaCursor.getString(3)
            val toCol = pragmaCursor.getString(4)
            if (parentTable == "locations" && fromCol == "location_id" && toCol == "id") {
                hasLocationFk = true
            }
        }
        pragmaCursor.close()
        assertTrue(hasLocationFk)

        // Verify existing storage_unit row remains preserved
        val suPreserved = db.query("SELECT COUNT(*) FROM storage_units WHERE id = 1000")
        assertTrue(suPreserved.moveToFirst())
        assertEquals(1, suPreserved.getInt(0))
        suPreserved.close()

        val l1 = db.query("SELECT location_id, canonical_organization_id, address, city, country, notes, created_at, created_by, updated_at, updated_by, entity_version FROM locations WHERE id = 100")
        assertTrue(l1.moveToFirst())
        assertEquals(validLocUuid, l1.getString(0))
        assertEquals(validOrgUuid, l1.getString(1))
        assertNull(l1.getString(2))
        assertNull(l1.getString(3))
        assertNull(l1.getString(4))
        assertNull(l1.getString(5))
        assertTrue(l1.isNull(6))
        assertTrue(l1.isNull(7))
        assertTrue(l1.isNull(8))
        assertTrue(l1.isNull(9))
        assertTrue(l1.isNull(10))
        l1.close()

        val l2 = db.query("SELECT location_id, canonical_organization_id FROM locations WHERE id = 200")
        assertTrue(l2.moveToFirst())
        assertNull(l2.getString(0))
        assertNull(l2.getString(1))
        l2.close()

        val suCursor = db.query("SELECT name FROM storage_units WHERE location_id = 100")
        assertTrue(suCursor.moveToFirst())
        assertEquals("Unit 1", suCursor.getString(0))
        suCursor.close()

        db.close()
    }

    @Test
    fun productionDatabase_enforcesForeignKeysByDefault() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val roomDb = androidx.room.Room.databaseBuilder(
            context,
            SiloMathDatabase::class.java,
            "prod_fk_test.db"
        ).addMigrations(MIGRATION_14_15).build()

        val cursor = roomDb.openHelper.writableDatabase.query("PRAGMA foreign_keys")
        assertTrue(cursor.moveToFirst())
        val fkEnabled = cursor.getInt(0)
        cursor.close()
        roomDb.close()

        assertEquals(1, fkEnabled)
    }
}
