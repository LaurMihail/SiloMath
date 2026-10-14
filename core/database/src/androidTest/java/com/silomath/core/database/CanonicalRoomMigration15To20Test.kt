package com.silomath.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.migration.CampaignCanonicalBackfill17To18
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CanonicalRoomMigration15To20Test {

    companion object {
        private const val TARGET_VERSION = 20

        private const val DB_15 =
            "canonical_migration_15_20_test.db"

        private const val DB_16 =
            "canonical_migration_16_20_test.db"

        private const val DB_17 =
            "canonical_migration_17_20_test.db"

        private const val DB_18 =
            "canonical_migration_18_20_test.db"

        private const val LEGACY_ORGANIZATION_ID = 1L

        private const val ORGANIZATION_UUID =
            "11111111-1111-4111-8111-111111111111"

        private const val USER_UUID =
            "22222222-2222-4222-8222-222222222222"

        private const val MEMBERSHIP_UUID =
            "33333333-3333-4333-8333-333333333333"

        private const val ACTOR_UUID =
            "44444444-4444-4444-8444-444444444444"

        private const val CREATED_AT =
            1_700_000_000_000L

        private const val UPDATED_AT =
            1_700_000_100_000L

        private const val ENTITY_VERSION =
            7L
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
        val context =
            instrumentation.targetContext

        listOf(
            DB_15,
            DB_16,
            DB_17,
            DB_18
        ).forEach { databaseName ->
            context.deleteDatabase(
                databaseName
            )
        }
    }

    @Test
    fun migration15To20_preservesOrganizationMembershipData() {
        migrateSupportedSourceTo20(
            databaseName =
                DB_15,
            sourceVersion =
                15
        )
    }

    @Test
    fun migration16To20_preservesOrganizationMembershipData() {
        migrateSupportedSourceTo20(
            databaseName =
                DB_16,
            sourceVersion =
                16
        )
    }

    @Test
    fun migration17To20_preservesOrganizationMembershipData() {
        migrateSupportedSourceTo20(
            databaseName =
                DB_17,
            sourceVersion =
                17
        )
    }

    @Test
    fun migration18To20_preservesOrganizationMembershipData() {
        migrateSupportedSourceTo20(
            databaseName =
                DB_18,
            sourceVersion =
                18
        )
    }

    private fun migrateSupportedSourceTo20(
        databaseName: String,
        sourceVersion: Int
    ) {
        var db =
            helper.createDatabase(
                databaseName,
                sourceVersion
            )

        insertSupportedSourceFixture(
            db
        )

        db.close()

        db =
            helper.runMigrationsAndValidate(
                databaseName,
                TARGET_VERSION,
                true
            )

        assertOrganizationPreserved(
            db
        )

        assertMembershipPreserved(
            db
        )

        assertDisableColumnsAddedWithoutFabricatedData(
            db
        )

        db.close()
    }

    private fun insertSupportedSourceFixture(
        db: SupportSQLiteDatabase
    ) {
        db.execSQL(
            """
            INSERT INTO organizations (
                id,
                organization_id,
                name,
                code,
                lifecycle_status,
                entity_version
            )
            VALUES (
                $LEGACY_ORGANIZATION_ID,
                '$ORGANIZATION_UUID',
                'Migration Test Organization',
                'MIGRATION_TEST',
                'ACTIVE',
                1
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
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            )
            VALUES (
                $LEGACY_ORGANIZATION_ID,
                '$ORGANIZATION_UUID',
                '$USER_UUID',
                'Admin',
                'ADMIN',
                '$MEMBERSHIP_UUID',
                'ACTIVE',
                $CREATED_AT,
                '$ACTOR_UUID',
                $UPDATED_AT,
                '$ACTOR_UUID',
                $ENTITY_VERSION
            )
            """.trimIndent()
        )
    }

    private fun assertOrganizationPreserved(
        db: SupportSQLiteDatabase
    ) {
        db.query(
            """
            SELECT
                organization_id,
                name,
                code,
                lifecycle_status,
                entity_version
            FROM organizations
            WHERE id = $LEGACY_ORGANIZATION_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(
                cursor.moveToFirst()
            )

            assertEquals(
                ORGANIZATION_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "organization_id"
                    )
                )
            )

            assertEquals(
                "Migration Test Organization",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "name"
                    )
                )
            )

            assertEquals(
                "MIGRATION_TEST",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "code"
                    )
                )
            )

            assertEquals(
                "ACTIVE",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "lifecycle_status"
                    )
                )
            )

            assertEquals(
                1L,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "entity_version"
                    )
                )
            )
        }
    }

    private fun assertMembershipPreserved(
        db: SupportSQLiteDatabase
    ) {
        db.query(
            """
            SELECT
                legacy_organization_id,
                organization_id,
                user_id,
                legacy_role,
                role,
                membership_id,
                lifecycle_status,
                created_at,
                created_by,
                updated_at,
                updated_by,
                entity_version
            FROM organization_memberships
            WHERE membership_id = '$MEMBERSHIP_UUID'
            """.trimIndent()
        ).use { cursor ->
            assertTrue(
                cursor.moveToFirst()
            )

            assertEquals(
                LEGACY_ORGANIZATION_ID,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "legacy_organization_id"
                    )
                )
            )

            assertEquals(
                ORGANIZATION_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "organization_id"
                    )
                )
            )

            assertEquals(
                USER_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "user_id"
                    )
                )
            )

            assertEquals(
                "Admin",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "legacy_role"
                    )
                )
            )

            assertEquals(
                "ADMIN",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "role"
                    )
                )
            )

            assertEquals(
                MEMBERSHIP_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "membership_id"
                    )
                )
            )

            assertEquals(
                "ACTIVE",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "lifecycle_status"
                    )
                )
            )

            assertEquals(
                CREATED_AT,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "created_at"
                    )
                )
            )

            assertEquals(
                ACTOR_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "created_by"
                    )
                )
            )

            assertEquals(
                UPDATED_AT,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "updated_at"
                    )
                )
            )

            assertEquals(
                ACTOR_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "updated_by"
                    )
                )
            )

            assertEquals(
                ENTITY_VERSION,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "entity_version"
                    )
                )
            )
        }
    }

    private fun assertDisableColumnsAddedWithoutFabricatedData(
        db: SupportSQLiteDatabase
    ) {
        assertNullableColumn(
            db =
                db,
            column =
                "disabled_at"
        )

        assertNullableColumn(
            db =
                db,
            column =
                "disabled_by"
        )

        db.query(
            """
            SELECT
                disabled_at,
                disabled_by
            FROM organization_memberships
            WHERE membership_id = '$MEMBERSHIP_UUID'
            """.trimIndent()
        ).use { cursor ->
            assertTrue(
                cursor.moveToFirst()
            )

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "disabled_at"
                    )
                )
            )

            assertNull(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "disabled_by"
                    )
                )
            )
        }
    }

    private fun assertNullableColumn(
        db: SupportSQLiteDatabase,
        column: String
    ) {
        var found = false

        db.query(
            "PRAGMA table_info(`organization_memberships`)"
        ).use { cursor ->
            val nameIndex =
                cursor.getColumnIndexOrThrow(
                    "name"
                )

            val notNullIndex =
                cursor.getColumnIndexOrThrow(
                    "notnull"
                )

            while (cursor.moveToNext()) {
                if (
                    cursor.getString(
                        nameIndex
                    ) == column
                ) {
                    found = true

                    assertEquals(
                        "organization_memberships.$column " +
                            "must be nullable in schema 20.",
                        0,
                        cursor.getInt(
                            notNullIndex
                        )
                    )

                    break
                }
            }
        }

        assertTrue(
            "Column organization_memberships.$column " +
                "was not found.",
            found
        )
    }
}