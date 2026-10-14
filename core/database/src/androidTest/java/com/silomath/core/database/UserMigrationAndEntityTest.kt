package com.silomath.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.di.MIGRATION_10_11
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserMigrationAndEntityTest {

    private val testDb = "migration_10_11_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SiloMathDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration10To11_createsUsersTableNonDestructively() {
        var db = helper.createDatabase(testDb, 10)
        db.execSQL("INSERT INTO organizations (id, name, lifecycle_status) VALUES (1, 'Org 1', 'ACTIVE')")
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 11, true, MIGRATION_10_11)

        val cursor = db.query("SELECT COUNT(*) FROM users")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
        cursor.close()

        val orgCursor = db.query("SELECT name FROM organizations WHERE id = 1")
        assertTrue(orgCursor.moveToFirst())
        assertEquals("Org 1", orgCursor.getString(0))
        orgCursor.close()

        db.close()
    }
}
