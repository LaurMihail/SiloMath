package com.silomath.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.migration.CampaignCanonicalBackfill17To18
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CanonicalRoomMigration15To19Test {

    companion object {
        private const val TARGET_VERSION = 19

        private const val DB_15 = "canonical_migration_15_19_test.db"
        private const val DB_16 = "canonical_migration_16_19_test.db"
        private const val DB_17 = "canonical_migration_17_19_test.db"
        private const val DB_18 = "canonical_migration_18_19_test.db"

        private const val ORGANIZATION_ID = 1L
        private const val CAMPAIGN_ID = 100L
        private const val STORAGE_SPACE_ID = 200L
        private const val GEOMETRY_VERSION_ID = 300L
        private const val SCOPE_ID = 400L
        private const val ASSIGNMENT_ID = 500L

        private const val USER_ID = "user-001"

        private const val ORGANIZATION_UUID =
            "11111111-1111-4111-8111-111111111111"

        private const val MEMBERSHIP_UUID =
            "22222222-2222-4222-8222-222222222222"

        private const val CAMPAIGN_UUID =
            "33333333-3333-4333-8333-333333333333"

        private const val STORAGE_SPACE_UUID =
            "44444444-4444-4444-8444-444444444444"

        private const val SCOPE_UUID =
            "55555555-5555-4555-8555-555555555555"

        private const val ASSIGNMENT_UUID =
            "66666666-6666-4666-8666-666666666666"

        private const val LEGACY_REMOTE_REVISION = 7L
    }

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        SiloMathDatabase::class.java,
        listOf(
            CampaignCanonicalBackfill17To18()
        ),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun cleanup() {
        val context = instrumentation.targetContext

        listOf(
            DB_15,
            DB_16,
            DB_17,
            DB_18
        ).forEach { dbName ->
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migration15To19_preservesLegacyCampaignAndBackfillsCanonicalData() {
        var db = helper.createDatabase(DB_15, 15)

        insertLegacyFixture(db)

        db.close()

        db = helper.runMigrationsAndValidate(
            DB_15,
            TARGET_VERSION,
            true
        )

        assertLegacyBusinessDataPreserved(db)
        assertCanonicalBackfill(db)
        assertVersion19LegacyBlockersAreNullable(db)

        db.close()
    }

    @Test
    fun migration16To19_preservesCampaignAndBackfillsScopeAndAssignment() {
        var db = helper.createDatabase(DB_16, 16)

        insertLegacyFixture(db)

        db.close()

        db = helper.runMigrationsAndValidate(
            DB_16,
            TARGET_VERSION,
            true
        )

        assertLegacyBusinessDataPreserved(db)
        assertCanonicalBackfill(db)
        assertVersion19LegacyBlockersAreNullable(db)

        db.close()
    }

    @Test
    fun migration17To19_backfillsCanonicalCampaignScopeAndAssignmentData() {
        var db = helper.createDatabase(DB_17, 17)

        insertLegacyFixture(db)

        assertCanonicalFieldsAreNullBeforeBackfill(db)

        db.close()

        db = helper.runMigrationsAndValidate(
            DB_17,
            TARGET_VERSION,
            true
        )

        assertLegacyBusinessDataPreserved(db)
        assertCanonicalBackfill(db)
        assertVersion19LegacyBlockersAreNullable(db)

        db.close()
    }

    @Test
    fun migration18To19_preservesCanonicalDataAndRelaxesLegacyBlockers() {
        var db = helper.createDatabase(DB_18, 18)

        insertLegacyFixture(db)
        populateVersion18CanonicalFixture(db)

        db.close()

        db = helper.runMigrationsAndValidate(
            DB_18,
            TARGET_VERSION,
            true
        )

        assertLegacyBusinessDataPreserved(db)
        assertCanonicalBackfill(db)
        assertVersion19LegacyBlockersAreNullable(db)

        db.close()
    }

    private fun insertLegacyFixture(
        db: SupportSQLiteDatabase
    ) {
        /*
         * Geometry/storage rows are deliberately inserted with FK enforcement
         * temporarily disabled because this migration test is scoped to the
         * Campaign canonicalization contract.
         *
         * The referenced GeometryVersion and StorageSpace rows themselves exist,
         * so CampaignScope relationships remain materially represented for the
         * migration being tested.
         */
        db.execSQL("PRAGMA foreign_keys = OFF")

        try {
            db.execSQL(
                """
                INSERT INTO organizations (
                    id,
                    organization_id,
                    name,
                    lifecycle_status
                )
                VALUES (
                    $ORGANIZATION_ID,
                    '$ORGANIZATION_UUID',
                    'Migration Test Organization',
                    'ACTIVE'
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO organization_memberships (
                    legacy_organization_id,
                    organization_id,
                    user_id,
                    legacy_role,
                    role,
                    membership_id,
                    lifecycle_status,
                    entity_version
                )
                VALUES (
                    $ORGANIZATION_ID,
                    '$ORGANIZATION_UUID',
                    '$USER_ID',
                    'Admin',
                    'ADMIN',
                    '$MEMBERSHIP_UUID',
                    'ACTIVE',
                    1
                )
                """.trimIndent()
            )

            /*
             * geometry_versions is required by the legacy CampaignScope FK.
             * Its own historical parent chain is outside this migration's
             * semantic responsibility.
             */
            db.execSQL(
                """
                INSERT INTO geometry_versions (
                    id,
                    geometry_profile_id,
                    version_number,
                    fixed_parameters,
                    required_dimensions_schema,
                    measurement_schema,
                    supported_units,
                    calculation_method,
                    validation_constraints
                )
                VALUES (
                    $GEOMETRY_VERSION_ID,
                    999,
                    1,
                    '{}',
                    '{}',
                    '{}',
                    '{}',
                    'migration-test',
                    '{}'
                )
                """.trimIndent()
            )

            /*
             * storage_spaces is required because 17 -> 18 obtains the
             * canonical StorageSpace UUID from its remoteId.
             */
            db.execSQL(
                """
                INSERT INTO storage_spaces (
                    id,
                    code,
                    storage_unit_id,
                    geometry_profile_id,
                    remoteId,
                    geometry_version_id,
                    configured_dimensions,
                    lifecycleStatus
                )
                VALUES (
                    $STORAGE_SPACE_ID,
                    'SILO-001',
                    999,
                    999,
                    '$STORAGE_SPACE_UUID',
                    $GEOMETRY_VERSION_ID,
                    '{}',
                    'ACTIVE'
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO campaigns (
                    id,
                    organization_id,
                    name,
                    status,
                    remoteId,
                    remote_revision
                )
                VALUES (
                    $CAMPAIGN_ID,
                    $ORGANIZATION_ID,
                    'Migration Test Campaign',
                    'Active',
                    '$CAMPAIGN_UUID',
                    $LEGACY_REMOTE_REVISION
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO campaign_scope (
                    id,
                    campaign_id,
                    storage_space_id,
                    geometry_version_id,
                    remoteId
                )
                VALUES (
                    $SCOPE_ID,
                    $CAMPAIGN_ID,
                    $STORAGE_SPACE_ID,
                    $GEOMETRY_VERSION_ID,
                    '$SCOPE_UUID'
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO campaign_assignments (
                    id,
                    campaign_id,
                    user_id,
                    role_at_assignment,
                    remoteId
                )
                VALUES (
                    $ASSIGNMENT_ID,
                    $CAMPAIGN_ID,
                    '$USER_ID',
                    'Admin',
                    '$ASSIGNMENT_UUID'
                )
                """.trimIndent()
            )
        } finally {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    private fun populateVersion18CanonicalFixture(
        db: SupportSQLiteDatabase
    ) {
        db.execSQL(
            """
            UPDATE campaigns
            SET
                campaign_id = '$CAMPAIGN_UUID',
                canonical_organization_id = '$ORGANIZATION_UUID',
                canonical_status = 'ACTIVE',
                entity_version = $LEGACY_REMOTE_REVISION
            WHERE id = $CAMPAIGN_ID
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaign_scope
            SET
                campaign_scope_id = '$SCOPE_UUID',
                canonical_campaign_id = '$CAMPAIGN_UUID',
                canonical_storage_space_id = '$STORAGE_SPACE_UUID'
            WHERE id = $SCOPE_ID
            """.trimIndent()
        )

        db.execSQL(
            """
            UPDATE campaign_assignments
            SET
                assignment_id = '$ASSIGNMENT_UUID',
                canonical_campaign_id = '$CAMPAIGN_UUID',
                organization_membership_id = '$MEMBERSHIP_UUID',
                assignment_status = 'ACTIVE'
            WHERE id = $ASSIGNMENT_ID
            """.trimIndent()
        )
    }

    private fun assertCanonicalFieldsAreNullBeforeBackfill(
        db: SupportSQLiteDatabase
    ) {
        db.query(
            """
            SELECT
                campaign_id,
                canonical_organization_id,
                canonical_status,
                entity_version
            FROM campaigns
            WHERE id = $CAMPAIGN_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow("campaign_id")
                )
            )

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "canonical_organization_id"
                    )
                )
            )

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow("canonical_status")
                )
            )

            assertTrue(
                cursor.isNull(
                    cursor.getColumnIndexOrThrow("entity_version")
                )
            )
        }

        db.query(
            """
            SELECT
                campaign_scope_id,
                canonical_campaign_id,
                canonical_storage_space_id,
                snapshot_hierarchy_ref
            FROM campaign_scope
            WHERE id = $SCOPE_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "campaign_scope_id"
                    )
                )
            )

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "canonical_campaign_id"
                    )
                )
            )

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "canonical_storage_space_id"
                    )
                )
            )

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "snapshot_hierarchy_ref"
                    )
                )
            )
        }

        db.query(
            """
            SELECT
                assignment_id,
                canonical_campaign_id,
                organization_membership_id,
                assignment_status,
                assigned_at,
                assigned_by,
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            FROM campaign_assignments
            WHERE id = $ASSIGNMENT_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            listOf(
                "assignment_id",
                "canonical_campaign_id",
                "organization_membership_id",
                "assignment_status",
                "assigned_at",
                "assigned_by",
                "created_at",
                "created_by",
                "updated_at",
                "updated_by",
                "entity_version"
            ).forEach { column ->
                assertTrue(
                    "Expected $column to be NULL before canonical backfill.",
                    cursor.isNull(
                        cursor.getColumnIndexOrThrow(column)
                    )
                )
            }
        }
    }

    private fun assertLegacyBusinessDataPreserved(
        db: SupportSQLiteDatabase
    ) {
        db.query(
            """
            SELECT
                organization_id,
                name,
                status,
                remoteId,
                remote_revision
            FROM campaigns
            WHERE id = $CAMPAIGN_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            assertEquals(
                ORGANIZATION_ID,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "organization_id"
                    )
                )
            )

            assertEquals(
                "Migration Test Campaign",
                cursor.getString(
                    cursor.getColumnIndexOrThrow("name")
                )
            )

            assertEquals(
                "Active",
                cursor.getString(
                    cursor.getColumnIndexOrThrow("status")
                )
            )

            assertEquals(
                CAMPAIGN_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow("remoteId")
                )
            )

            assertEquals(
                LEGACY_REMOTE_REVISION,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "remote_revision"
                    )
                )
            )
        }

        db.query(
            """
            SELECT
                campaign_id,
                storage_space_id,
                geometry_version_id,
                remoteId
            FROM campaign_scope
            WHERE id = $SCOPE_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            assertEquals(
                CAMPAIGN_ID,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow("campaign_id")
                )
            )

            assertEquals(
                STORAGE_SPACE_ID,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "storage_space_id"
                    )
                )
            )

            assertEquals(
                GEOMETRY_VERSION_ID,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "geometry_version_id"
                    )
                )
            )

            assertEquals(
                SCOPE_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow("remoteId")
                )
            )
        }

        db.query(
            """
            SELECT
                campaign_id,
                user_id,
                role_at_assignment,
                remoteId
            FROM campaign_assignments
            WHERE id = $ASSIGNMENT_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            assertEquals(
                CAMPAIGN_ID,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow("campaign_id")
                )
            )

            assertEquals(
                USER_ID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow("user_id")
                )
            )

            assertEquals(
                "Admin",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "role_at_assignment"
                    )
                )
            )

            assertEquals(
                ASSIGNMENT_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow("remoteId")
                )
            )
        }
    }

    private fun assertCanonicalBackfill(
        db: SupportSQLiteDatabase
    ) {
        db.query(
            """
            SELECT
                campaign_id,
                canonical_organization_id,
                canonical_status,
                entity_version
            FROM campaigns
            WHERE id = $CAMPAIGN_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            assertEquals(
                CAMPAIGN_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow("campaign_id")
                )
            )

            assertEquals(
                ORGANIZATION_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "canonical_organization_id"
                    )
                )
            )

            assertEquals(
                "ACTIVE",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "canonical_status"
                    )
                )
            )

            assertEquals(
                LEGACY_REMOTE_REVISION,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "entity_version"
                    )
                )
            )
        }

        db.query(
            """
            SELECT
                campaign_scope_id,
                canonical_campaign_id,
                canonical_storage_space_id,
                snapshot_hierarchy_ref
            FROM campaign_scope
            WHERE id = $SCOPE_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            assertEquals(
                SCOPE_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "campaign_scope_id"
                    )
                )
            )

            assertEquals(
                CAMPAIGN_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "canonical_campaign_id"
                    )
                )
            )

            assertEquals(
                STORAGE_SPACE_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "canonical_storage_space_id"
                    )
                )
            )

            /*
             * Deliberately not synthesized by the migration.
             */
            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "snapshot_hierarchy_ref"
                    )
                )
            )
        }

        db.query(
            """
            SELECT
                assignment_id,
                canonical_campaign_id,
                organization_membership_id,
                assignment_status,
                assigned_at,
                assigned_by,
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            FROM campaign_assignments
            WHERE id = $ASSIGNMENT_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())

            assertEquals(
                ASSIGNMENT_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "assignment_id"
                    )
                )
            )

            assertEquals(
                CAMPAIGN_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "canonical_campaign_id"
                    )
                )
            )

            assertEquals(
                MEMBERSHIP_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "organization_membership_id"
                    )
                )
            )

            assertEquals(
                "ACTIVE",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "assignment_status"
                    )
                )
            )

            /*
             * These values have no authoritative legacy source and the
             * production migration intentionally does not invent them.
             */
            listOf(
                "assigned_at",
                "assigned_by",
                "created_at",
                "created_by",
                "updated_at",
                "updated_by",
                "entity_version"
            ).forEach { column ->
                assertTrue(
                    "Expected $column to remain NULL.",
                    cursor.isNull(
                        cursor.getColumnIndexOrThrow(column)
                    )
                )
            }
        }
    }

    private fun assertVersion19LegacyBlockersAreNullable(
        db: SupportSQLiteDatabase
    ) {
        assertColumnNullable(
            db = db,
            table = "campaign_scope",
            column = "geometry_version_id"
        )

        assertColumnNullable(
            db = db,
            table = "campaign_assignments",
            column = "role_at_assignment"
        )
    }

    private fun assertColumnNullable(
        db: SupportSQLiteDatabase,
        table: String,
        column: String
    ) {
        var found = false

        db.query(
            "PRAGMA table_info(`$table`)"
        ).use { cursor ->
            val nameIndex =
                cursor.getColumnIndexOrThrow("name")

            val notNullIndex =
                cursor.getColumnIndexOrThrow("notnull")

            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    found = true

                    assertEquals(
                        "$table.$column must be nullable in schema 19.",
                        0,
                        cursor.getInt(notNullIndex)
                    )

                    break
                }
            }
        }

        assertTrue(
            "Column $table.$column was not found.",
            found
        )
    }
}
