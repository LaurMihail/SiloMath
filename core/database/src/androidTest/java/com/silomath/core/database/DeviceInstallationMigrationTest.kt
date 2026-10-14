package com.silomath.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.di.MIGRATION_11_12
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceInstallationMigrationTest {

    private val testDb = "migration_11_12_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SiloMathDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration11To12_preservesInstallationUuidAndCreatesCanonicalTable() {
        var db = helper.createDatabase(testDb, 11)
        db.execSQL("INSERT INTO device_installations (installation_uuid, created_at) VALUES ('123e4567-e89b-12d3-a456-426614174000', 123456789)")
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 12, true, MIGRATION_11_12)

        // Verify exactly one row remains
        val countCursor = db.query("SELECT COUNT(*) FROM device_installations")
        assertTrue(countCursor.moveToFirst())
        assertEquals(1, countCursor.getInt(0))
        countCursor.close()

        // Verify ID, created_at, nullability of user_id, last_authorized_at, revoked_at, updated_at, entity_version
        val cursor = db.query("SELECT device_installation_id, user_id, created_at, last_authorized_at, revoked_at, updated_at, entity_version FROM device_installations")
        assertTrue(cursor.moveToFirst())
        assertEquals("123e4567-e89b-12d3-a456-426614174000", cursor.getString(0))
        assertNull(cursor.getString(1)) // user_id is null
        assertEquals(123456789L, cursor.getLong(2)) // created_at
        assertTrue(cursor.isNull(3)) // last_authorized_at
        assertTrue(cursor.isNull(4)) // revoked_at
        assertEquals(123456789L, cursor.getLong(5)) // updated_at equals created_at
        assertEquals(1L, cursor.getLong(6)) // entity_version
        cursor.close()

        // Verify foreign key constraint definition references users with RESTRICT
        val pragmaCursor = db.query("PRAGMA foreign_key_list(device_installations)")
        var hasUserFk = false
        var isRestrict = false
        var isNoActionUpdate = false
        while (pragmaCursor.moveToNext()) {
            val parentTable = pragmaCursor.getString(2)
            val fromColumn = pragmaCursor.getString(3)
            val toColumn = pragmaCursor.getString(4)
            val onUpdate = pragmaCursor.getString(5)
            val onDelete = pragmaCursor.getString(6)
            if (parentTable == "users" && fromColumn == "user_id" && toColumn == "user_id") {
                hasUserFk = true
                if (onDelete.equals("RESTRICT", ignoreCase = true)) {
                    isRestrict = true
                }
                if (onUpdate.equals("NO ACTION", ignoreCase = true)) {
                    isNoActionUpdate = true
                }
            }
        }
        pragmaCursor.close()
        assertTrue(hasUserFk)
        assertTrue(isRestrict)
        assertTrue(isNoActionUpdate)

        db.close()
    }
}
