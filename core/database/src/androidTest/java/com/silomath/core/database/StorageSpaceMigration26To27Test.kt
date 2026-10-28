package com.silomath.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageSpaceMigration26To27Test {
    companion object {private const val DB="storage_space_26_27.db"}
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    @get:Rule val helper=MigrationTestHelper(instrumentation,SiloMathDatabase::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
    @After fun cleanup(){instrumentation.targetContext.deleteDatabase(DB)}
    @Test fun migrationAddsCanonicalUnitCodeUniquenessWithoutDroppingLegacy() {
        helper.createDatabase(DB,26).close()
        val room=Room.databaseBuilder(instrumentation.targetContext,SiloMathDatabase::class.java,DB).build()
        val db=room.openHelper.writableDatabase
        db.query("PRAGMA index_list('v6_storage_spaces')").use {cursor->
            var found=false
            while(cursor.moveToNext())if(cursor.getString(cursor.getColumnIndexOrThrow("name")).contains("storage_unit_id_code"))found=true
            assertTrue(found)
        }
        db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='storage_spaces'").use{assertTrue(it.moveToFirst())}
        room.close()
    }
}
