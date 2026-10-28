package com.silomath.core.database

import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.di.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * reconstructed / non-authoritative historical test fixture.
 * These minimal sources exercise the REAL migrations, not complete historical Room schemas.
 * Authentic fixtures 9-13 are unavailable; original MigrationTestHelper tests remain BLOCKED.
 * Literal expected mappings below describe existing migration SQL, not new business rules.
 */
@RunWith(AndroidJUnit4::class)
class HistoricalMigrationReconstructedBehaviorTest {
    private val uuid = "11111111-1111-4111-8111-111111111111"

    @Test fun reconstructed9To10PreservesExistingRowsAndCreatesEmptySyncAndDeviceTables() = fixture(9,
        "CREATE TABLE organizations(id INTEGER PRIMARY KEY,name TEXT,lifecycleStatus TEXT)"
    ) { db ->
        db.execSQL("INSERT INTO organizations VALUES(7,' Legacy organization ','INACTIVE')")
        val before = rows(db,"SELECT * FROM organizations")
        migrate(db,MIGRATION_9_10)
        assertEquals(before,rows(db,"SELECT * FROM organizations"))
        assertEquals(listOf(listOf(0L)),rows(db,"SELECT count(*) FROM sync_operations"))
        assertEquals(listOf(listOf(0L)),rows(db,"SELECT count(*) FROM device_installations"))
        db.execSQL("""INSERT INTO sync_operations(sync_operation_id,idempotency_key,organization_id,entity_type,entity_id,
            operation_type,base_entity_version,event_at,payload_json,state,attempt_count,created_at,updated_at)
            VALUES('operation','idempotency',?,'LOCATION','entity','UPDATE',1,23,?,'PENDING',0,23,23)""",
            arrayOf(uuid,"{ \"retained\": \"1.000001\" }"))
        assertEquals(listOf(listOf(null,null,null,1L,"{ \"retained\": \"1.000001\" }","PENDING")),
            rows(db,"SELECT campaign_id,retry_metadata,last_error,base_entity_version,payload_json,state FROM sync_operations"))
        db.execSQL("INSERT INTO device_installations VALUES(?,23)",arrayOf(uuid))
        assertEquals(listOf(listOf(uuid,23L)),rows(db,"SELECT * FROM device_installations"))
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("INSERT INTO device_installations VALUES(?,99)",arrayOf(uuid)) }
    }

    @Test fun reconstructed10To11PreservesRowsAndAddsUsersWithDeclaredDefaults() = fixture(10,
        "CREATE TABLE organizations(id INTEGER PRIMARY KEY,name TEXT,lifecycle_status TEXT)"
    ) { db ->
        db.execSQL("INSERT INTO organizations VALUES(7,'Retained','ACTIVE')")
        val before=rows(db,"SELECT * FROM organizations")
        migrate(db,MIGRATION_10_11)
        assertEquals(before,rows(db,"SELECT * FROM organizations"))
        assertEquals(listOf(listOf(0L)),rows(db,"SELECT count(*) FROM users"))
        db.execSQL("""INSERT INTO users(user_id,full_name,email,email_normalized,created_at,updated_at,updated_by)
            VALUES(?,'Test user','user@example.test','user@example.test',23,24,?)""",arrayOf(uuid,uuid))
        assertEquals(listOf(listOf(uuid,"ACTIVE",1L,null,23L,24L,uuid)),
            rows(db,"SELECT user_id,lifecycle_status,entity_version,created_by,created_at,updated_at,updated_by FROM users"))
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL("""INSERT INTO users(user_id,full_name,email,email_normalized,created_at,updated_at,updated_by)
                VALUES('another','Other','user@example.test','user@example.test',23,24,?)""",arrayOf(uuid))
        }
    }

    @Test fun reconstructed11To12PreservesInstallationIdentityAndAddsNullableUserFk() = fixture(11,
        "CREATE TABLE users(user_id TEXT PRIMARY KEY NOT NULL)",
        "CREATE TABLE device_installations(installation_uuid TEXT PRIMARY KEY NOT NULL,created_at INTEGER NOT NULL)"
    ) { db ->
        db.execSQL("INSERT INTO users VALUES(?)",arrayOf(uuid))
        db.execSQL("INSERT INTO device_installations VALUES(?,123456789),('legacy-installation',0)",arrayOf(uuid))
        val before=rows(db,"SELECT installation_uuid,created_at FROM device_installations ORDER BY installation_uuid")
        val usersBefore=rows(db,"SELECT * FROM users")
        migrate(db,MIGRATION_11_12)
        assertEquals(before,rows(db,"SELECT device_installation_id,created_at FROM device_installations ORDER BY device_installation_id"))
        assertEquals(usersBefore,rows(db,"SELECT * FROM users"))
        assertEquals(listOf(listOf(null,null,null,0L,1L),listOf(null,null,null,123456789L,1L)),
            rows(db,"SELECT user_id,last_authorized_at,revoked_at,updated_at,entity_version FROM device_installations ORDER BY created_at"))
        assertFk(db,"device_installations","user_id","users","user_id","RESTRICT")
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("UPDATE device_installations SET user_id='missing'") }
        db.execSQL("UPDATE device_installations SET user_id=? WHERE device_installation_id=?",arrayOf(uuid,uuid))
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("DELETE FROM users WHERE user_id=?",arrayOf(uuid)) }
    }

    @Test fun reconstructed12To13PreservesOrganizationsChildrenAndOnlyAppliesDeclaredMappings() = fixture(12,
        "CREATE TABLE organizations(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,remoteId TEXT,lifecycleStatus TEXT NOT NULL,remoteRevision INTEGER)",
        "CREATE TABLE locations(id INTEGER PRIMARY KEY,organization_id INTEGER NOT NULL,code TEXT,name TEXT,FOREIGN KEY(organization_id) REFERENCES organizations(id) ON DELETE CASCADE)"
    ) { db ->
        db.execSQL("""INSERT INTO organizations VALUES
            (1,' Org Alpha ',?,'ACTIVE',5),(2,'Org Beta',NULL,'INACTIVE',NULL),
            (3,'Org Gamma','not-a-uuid','ACTIVE',0)""",arrayOf(uuid))
        db.execSQL("INSERT INTO locations VALUES(10,1,'LOC',' Retained child '),(20,2,'LOC2','Other child')")
        val children=rows(db,"SELECT * FROM locations ORDER BY id")
        val identities=rows(db,"SELECT id,lifecycleStatus FROM organizations ORDER BY id")
        migrate(db,MIGRATION_12_13)
        assertEquals(identities,rows(db,"SELECT id,lifecycle_status FROM organizations ORDER BY id"))
        assertEquals(children,rows(db,"SELECT * FROM locations ORDER BY id"))
        // trim(name), UUID validation and revision>=1 are explicit EXISTING migration SQL mappings.
        assertEquals(listOf(listOf(1L,uuid,"Org Alpha",5L),listOf(2L,null,"Org Beta",null),listOf(3L,null,"Org Gamma",null)),
            rows(db,"SELECT id,organization_id,name,entity_version FROM organizations ORDER BY id"))
        assertAllNull(db,"SELECT code,address,city,country,created_at,created_by,updated_at,updated_by FROM organizations",3)
        assertFk(db,"locations","organization_id","organizations","id","CASCADE")
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("INSERT INTO locations VALUES(30,999,'BAD','Bad')") }
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("UPDATE organizations SET organization_id='invalid' WHERE id=2") }
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("UPDATE organizations SET organization_id=? WHERE id=2",arrayOf(uuid)) }
        assertEquals(children,rows(db,"SELECT * FROM locations ORDER BY id"))
    }

    @Test fun reconstructed13To14PreservesLegacyMembershipsAndExplicitRoleMappings() = fixture(13,
        "CREATE TABLE organizations(id INTEGER PRIMARY KEY,organization_id TEXT UNIQUE)",
        "CREATE TABLE organization_members(organization_id INTEGER NOT NULL,user_id TEXT NOT NULL,role TEXT NOT NULL,PRIMARY KEY(organization_id,user_id),FOREIGN KEY(organization_id) REFERENCES organizations(id) ON DELETE CASCADE)"
    ) { db ->
        db.execSQL("INSERT INTO organizations VALUES(1,?),(2,NULL)",arrayOf(uuid))
        db.execSQL("INSERT INTO organization_members VALUES(1,'usr-1','Admin'),(1,'usr-2','Standard User'),(2,'usr-3','Standard'),(1,'usr-4','SuperUser')")
        val before=rows(db,"SELECT * FROM organization_members ORDER BY user_id")
        val parents=rows(db,"SELECT * FROM organizations ORDER BY id")
        migrate(db,MIGRATION_13_14)
        assertEquals(before,rows(db,"SELECT * FROM organization_members ORDER BY user_id"))
        assertEquals(parents,rows(db,"SELECT * FROM organizations ORDER BY id"))
        assertEquals(before,rows(db,"SELECT legacy_organization_id,user_id,legacy_role FROM organization_memberships ORDER BY user_id"))
        assertEquals(listOf(listOf(uuid,"ADMIN"),listOf(uuid,"STANDARD"),listOf(null,"STANDARD"),listOf(uuid,null)),
            rows(db,"SELECT organization_id,role FROM organization_memberships ORDER BY user_id"))
        assertAllNull(db,"SELECT membership_id,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version FROM organization_memberships",4)
        assertFk(db,"organization_memberships","legacy_organization_id","organizations","id","RESTRICT")
        assertFk(db,"organization_memberships","organization_id","organizations","organization_id","RESTRICT")
        assertThrows(SQLiteConstraintException::class.java) { db.execSQL("DELETE FROM organizations WHERE id=1") }
        assertEquals(before,rows(db,"SELECT * FROM organization_members ORDER BY user_id"))
    }

    private fun fixture(version: Int,vararg ddl: String, action: (SupportSQLiteDatabase)->Unit) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) { ddl.forEach { db.execSQL(it) } }
                override fun onUpgrade(db: SupportSQLiteDatabase,oldVersion: Int,newVersion: Int) { error("Explicit real migration only") }
            }).build())
        try { action(helper.writableDatabase) } finally { helper.close() }
    }
    private fun migrate(db: SupportSQLiteDatabase,migration: Migration) {
        db.setForeignKeyConstraintsEnabled(false)
        db.beginTransaction()
        try { migration.migrate(db);db.setTransactionSuccessful() } finally { db.endTransaction() }
        db.setForeignKeyConstraintsEnabled(true)
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
    }
    private fun assertAllNull(db: SupportSQLiteDatabase,sql: String,expectedRows: Int) {
        db.query(sql).use { cursor ->
            var count=0
            while(cursor.moveToNext()) { count++;for(i in 0 until cursor.columnCount) assertTrue(cursor.isNull(i)) }
            assertEquals(expectedRows,count)
        }
    }
    private fun assertFk(db: SupportSQLiteDatabase,table: String,column: String,parent: String,key: String,onDelete: String) {
        db.query("PRAGMA foreign_key_list($table)").use { cursor ->
            var found=false
            while(cursor.moveToNext()) if(cursor.getString(2)==parent && cursor.getString(3)==column && cursor.getString(4)==key) {
                assertEquals(onDelete,cursor.getString(6));found=true
            }
            assertTrue("$table.$column -> $parent.$key",found)
        }
    }
    private fun rows(db: SupportSQLiteDatabase,sql: String): List<List<Any?>> = db.query(sql).use { cursor ->
        buildList { while(cursor.moveToNext()) add((0 until cursor.columnCount).map { i ->
            when(cursor.getType(i)) { Cursor.FIELD_TYPE_NULL -> null;Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i);else -> cursor.getString(i) }
        }) }
    }
}
