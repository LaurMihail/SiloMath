package com.silomath.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.migration.MasterDataCanonicalBackfill20To21
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CanonicalRoomMigration20To21Test {

    companion object {
        private const val SOURCE_VERSION = 20
        private const val TARGET_VERSION = 21
        private const val DB_NAME = "canonical_migration_20_21_test.db"

        private const val ORG_UUID = "11111111-1111-4111-8111-111111111111"
        private const val LOCATION_UUID = "22222222-2222-4222-8222-222222222222"
        private const val UNIT_UUID_UPPER = "33333333-3333-4333-8333-ABCDEFABCDEF"
        private const val UNIT_UUID = "33333333-3333-4333-8333-abcdefabcdef"
        private const val SPACE_UUID = "44444444-4444-4444-8444-444444444444"
        private const val PROFILE_UUID = "55555555-5555-4555-8555-555555555555"
        private const val CLIENT_UUID = "66666666-6666-4666-8666-666666666666"
        private const val INVALID_UUID = "not-a-uuid"
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        SiloMathDatabase::class.java,
        listOf(MasterDataCanonicalBackfill20To21()),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun cleanup() {
        instrumentation.targetContext.deleteDatabase(DB_NAME)
    }

    @Test
    fun migration20To21_backfillsCanonicalMasterDataWithoutFabrication() {
        var db = helper.createDatabase(DB_NAME, SOURCE_VERSION)

        insertVersion20Fixture(db)
        db.close()

        db = helper.runMigrationsAndValidate(
            DB_NAME,
            TARGET_VERSION,
            true
        )

        assertCanonicalBackfill(db)
        assertInvalidRemoteIdNotBackfilled(db)
        assertNewlyRelaxedColumnsNullable(db)

        db.close()
    }

    private fun insertVersion20Fixture(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO organizations (id, organization_id, name, code, lifecycle_status, entity_version) VALUES (1, '$ORG_UUID', 'Org', 'ORG', 'ACTIVE', 1)"
        )

        db.execSQL(
            "INSERT INTO locations (id, organization_id, remoteId, code, name, lifecycleStatus, location_id, canonical_organization_id, entity_version) VALUES (10, 1, '$LOCATION_UUID', 'LOC1', 'Location', 'ACTIVE', '$LOCATION_UUID', '$ORG_UUID', 2)"
        )

        db.execSQL(
            "INSERT INTO storage_units (id, location_id, remoteId, code, name, type, lifecycleStatus, remoteRevision) VALUES (20, 10, '$UNIT_UUID_UPPER', 'UNIT1', 'Unit 1', 'SILO', 'ACTIVE', 7)"
        )

        db.execSQL(
            "INSERT INTO storage_units (id, location_id, remoteId, code, name, type, lifecycleStatus, remoteRevision) VALUES (21, 10, '$INVALID_UUID', 'UNIT2', 'Unit 2', 'SILO', 'ACTIVE', 0)"
        )

        db.execSQL(
            "INSERT INTO geometry_families (id, remoteId, name, lifecycleStatus, remoteRevision) VALUES (30, NULL, 'Family', 'ACTIVE', NULL)"
        )

        db.execSQL(
            "INSERT INTO geometry_profiles (id, geometry_family_id, remoteId, profile_code, name, lifecycleStatus, remoteRevision) VALUES (40, 30, '$PROFILE_UUID', 'PROFILE1', 'Profile', 'ACTIVE', 3)"
        )

        db.execSQL(
            "INSERT INTO geometry_versions (id, geometry_profile_id, remoteId, version_number, fixed_parameters, required_dimensions_schema, measurement_schema, supported_units, calculation_method, validation_constraints, capacity_rules) VALUES (50, 40, NULL, 1, '{}', '{}', '{}', '{}', 'METHOD', '{}', NULL)"
        )

        db.execSQL(
            "INSERT INTO storage_spaces (id, code, storage_unit_id, geometry_profile_id, remoteId, geometry_version_id, configured_dimensions, lifecycleStatus, remoteRevision) VALUES (60, 'SPACE1', 20, 40, '$SPACE_UUID', 50, '{}', 'ACTIVE', 9)"
        )

        db.execSQL(
            "INSERT INTO clients (id, organization_id, remoteId, name, lifecycle_status, remote_revision) VALUES (70, 1, '$CLIENT_UUID', 'Client', 'ACTIVE', 4)"
        )
    }

    private fun assertCanonicalBackfill(db: SupportSQLiteDatabase) {
        db.query("SELECT storage_unit_id, canonical_location_id, canonical_organization_id, entity_version FROM storage_units WHERE id = 20").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(UNIT_UUID, cursor.getString(cursor.getColumnIndexOrThrow("storage_unit_id")))
            assertEquals(LOCATION_UUID, cursor.getString(cursor.getColumnIndexOrThrow("canonical_location_id")))
            assertEquals(ORG_UUID, cursor.getString(cursor.getColumnIndexOrThrow("canonical_organization_id")))
            assertEquals(7L, cursor.getLong(cursor.getColumnIndexOrThrow("entity_version")))
        }

        db.query("SELECT geometry_profile_id FROM geometry_profiles WHERE id = 40").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(PROFILE_UUID, cursor.getString(cursor.getColumnIndexOrThrow("geometry_profile_id")))
        }

        db.query("SELECT storage_space_id, canonical_storage_unit_id, canonical_organization_id, canonical_geometry_profile_id, entity_version FROM storage_spaces WHERE id = 60").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(SPACE_UUID, cursor.getString(cursor.getColumnIndexOrThrow("storage_space_id")))
            assertEquals(UNIT_UUID, cursor.getString(cursor.getColumnIndexOrThrow("canonical_storage_unit_id")))
            assertEquals(ORG_UUID, cursor.getString(cursor.getColumnIndexOrThrow("canonical_organization_id")))
            assertEquals(PROFILE_UUID, cursor.getString(cursor.getColumnIndexOrThrow("canonical_geometry_profile_id")))
            assertEquals(9L, cursor.getLong(cursor.getColumnIndexOrThrow("entity_version")))
        }

        db.query("SELECT client_id, canonical_organization_id, entity_version FROM clients WHERE id = 70").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(CLIENT_UUID, cursor.getString(cursor.getColumnIndexOrThrow("client_id")))
            assertEquals(ORG_UUID, cursor.getString(cursor.getColumnIndexOrThrow("canonical_organization_id")))
            assertEquals(4L, cursor.getLong(cursor.getColumnIndexOrThrow("entity_version")))
        }
    }

    private fun assertInvalidRemoteIdNotBackfilled(db: SupportSQLiteDatabase) {
        db.query("SELECT storage_unit_id, entity_version FROM storage_units WHERE id = 21").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("storage_unit_id")))
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("entity_version")))
        }
    }

    private fun assertNewlyRelaxedColumnsNullable(db: SupportSQLiteDatabase) {
        assertColumnNullable(db, "storage_units", "type")
        assertColumnNullable(db, "storage_spaces", "geometry_version_id")
        assertColumnNullable(db, "storage_spaces", "configured_dimensions")
    }

    private fun assertColumnNullable(
        db: SupportSQLiteDatabase,
        table: String,
        column: String
    ) {
        var found = false

        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")

            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    found = true
                    assertEquals(
                        "$table.$column must be nullable in schema 21.",
                        0,
                        cursor.getInt(notNullIndex)
                    )
                    break
                }
            }
        }

        assertTrue("Column $table.$column was not found.", found)
    }
}
