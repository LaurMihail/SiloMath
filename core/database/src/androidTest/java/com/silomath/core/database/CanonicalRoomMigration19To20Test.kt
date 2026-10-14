package com.silomath.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CanonicalRoomMigration19To20Test {

    companion object {
        private const val SOURCE_VERSION = 19
        private const val TARGET_VERSION = 20

        private const val DB_NAME =
            "canonical_migration_19_20_test.db"

        private const val LEGACY_ORGANIZATION_ID = 1L

        private const val ORGANIZATION_UUID =
            "11111111-1111-4111-8111-111111111111"

        private const val USER_UUID =
            "22222222-2222-4222-8222-222222222222"

        private const val MEMBERSHIP_UUID =
            "33333333-3333-4333-8333-333333333333"

        private const val ACTOR_UUID =
            "44444444-4444-4444-8444-444444444444"

        private const val DISABLER_UUID =
            "55555555-5555-4555-8555-555555555555"

        private const val CREATED_AT =
            1_700_000_000_000L

        private const val UPDATED_AT =
            1_700_000_100_000L

        private const val DISABLED_AT =
            1_700_000_200_000L

        private const val ENTITY_VERSION =
            7L
    }

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        SiloMathDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun cleanup() {
        instrumentation
            .targetContext
            .deleteDatabase(DB_NAME)
    }

    @Test
    fun migration19To20_preservesCanonicalMembershipAndAddsDisableMetadata() {
        var db =
            helper.createDatabase(
                DB_NAME,
                SOURCE_VERSION
            )

        insertVersion19Fixture(
            db
        )

        assertDisableColumnsAbsent(
            db
        )

        db.close()

        db =
            helper.runMigrationsAndValidate(
                DB_NAME,
                TARGET_VERSION,
                true
            )

        assertExistingMembershipPreserved(
            db
        )

        assertDisableColumnsExistAndAreNullable(
            db
        )

        assertNewDisableMetadataStartsNull(
            db
        )

        assertDisableMetadataCanBePersisted(
            db
        )

        db.close()
    }

    private fun insertVersion19Fixture(
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

    private fun assertDisableColumnsAbsent(
        db: SupportSQLiteDatabase
    ) {
        val columnNames =
            mutableSetOf<String>()

        db.query(
            "PRAGMA table_info(`organization_memberships`)"
        ).use { cursor ->
            val nameIndex =
                cursor.getColumnIndexOrThrow(
                    "name"
                )

            while (cursor.moveToNext()) {
                columnNames +=
                    cursor.getString(
                        nameIndex
                    )
            }
        }

        assertFalse(
            "Schema 19 must not already contain disabled_at.",
            columnNames.contains(
                "disabled_at"
            )
        )

        assertFalse(
            "Schema 19 must not already contain disabled_by.",
            columnNames.contains(
                "disabled_by"
            )
        )
    }

    private fun assertExistingMembershipPreserved(
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

    private fun assertDisableColumnsExistAndAreNullable(
        db: SupportSQLiteDatabase
    ) {
        assertColumnNullable(
            db =
                db,
            table =
                "organization_memberships",
            column =
                "disabled_at"
        )

        assertColumnNullable(
            db =
                db,
            table =
                "organization_memberships",
            column =
                "disabled_by"
        )
    }

    private fun assertNewDisableMetadataStartsNull(
        db: SupportSQLiteDatabase
    ) {
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

    private fun assertDisableMetadataCanBePersisted(
        db: SupportSQLiteDatabase
    ) {
        db.execSQL(
            """
            UPDATE organization_memberships
            SET
                lifecycle_status = 'DISABLED',
                disabled_at = $DISABLED_AT,
                disabled_by = '$DISABLER_UUID',
                entity_version = ${ENTITY_VERSION + 1}
            WHERE membership_id = '$MEMBERSHIP_UUID'
            """.trimIndent()
        )

        db.query(
            """
            SELECT
                lifecycle_status,
                disabled_at,
                disabled_by,
                entity_version
            FROM organization_memberships
            WHERE membership_id = '$MEMBERSHIP_UUID'
            """.trimIndent()
        ).use { cursor ->
            assertTrue(
                cursor.moveToFirst()
            )

            assertEquals(
                "DISABLED",
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "lifecycle_status"
                    )
                )
            )

            assertEquals(
                DISABLED_AT,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "disabled_at"
                    )
                )
            )

            assertEquals(
                DISABLER_UUID,
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "disabled_by"
                    )
                )
            )

            assertEquals(
                ENTITY_VERSION + 1,
                cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        "entity_version"
                    )
                )
            )
        }
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
                        "$table.$column must be nullable in schema 20.",
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
            "Column $table.$column was not found.",
            found
        )
    }
}