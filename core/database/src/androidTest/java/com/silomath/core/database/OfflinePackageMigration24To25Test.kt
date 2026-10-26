package com.silomath.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflinePackageMigration24To25Test {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    @get:Rule val helper=MigrationTestHelper(instrumentation,SiloMathDatabase::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())

    @Test fun addsStagingWithoutChangingRetainedBusinessData() {
        val name="offline_package_24_25_test.db"
        helper.createDatabase(name,24).use { db ->
            db.execSQL("INSERT INTO organizations(id,name) VALUES(9401,'Retained offline organization')")
        }
        val room=Room.databaseBuilder(instrumentation.targetContext,SiloMathDatabase::class.java,name).build()
        try {
            val db=room.openHelper.writableDatabase
            db.query("SELECT name FROM organizations WHERE id=9401").use { rows ->
                rows.moveToFirst(); assertEquals("Retained offline organization",rows.getString(0))
            }
            db.query("SELECT count(*) FROM offline_package_downloads").use { rows ->
                rows.moveToFirst();assertEquals(0,rows.getInt(0))
            }
        } finally {
            room.close()
        }
    }
}
