package com.silomath.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.offline.CanonicalOfflineSchema
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CanonicalOfflineExpand25To26Test {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    @get:Rule val helper=MigrationTestHelper(instrumentation,SiloMathDatabase::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())

    @Test fun expandsCanonicalTablesWithoutReinterpretingLegacyDataOrOutbox() {
        val name="canonical_expand_25_26.db"
        helper.createDatabase(name,25).use { db ->
            db.execSQL("INSERT INTO organizations(id,name) VALUES(9526,'Retained organization')")
            db.execSQL("INSERT INTO geometry_families(id,name,lifecycleStatus) VALUES(9526,'Legacy family','ACTIVE')")
            db.execSQL("INSERT INTO geometry_profiles(id,geometry_family_id,profile_code,name,lifecycleStatus) VALUES(9526,9526,'LEGACY','Legacy profile','ACTIVE')")
            db.execSQL("INSERT INTO geometry_versions(id,geometry_profile_id,version_number,fixed_parameters,required_dimensions_schema,measurement_schema,supported_units,calculation_method,validation_constraints) VALUES(9526,9526,1,'uninterpreted legacy parameters','legacy schema','legacy measurements','legacy units','legacy method','legacy constraints')")
            db.execSQL("INSERT INTO locations(id,organization_id,code,name,lifecycleStatus) VALUES(9526,9526,'LEG','Legacy location','ACTIVE')")
            db.execSQL("INSERT INTO storage_units(id,location_id,code,name,lifecycleStatus) VALUES(9526,9526,'LEG','Legacy unit','ACTIVE')")
            db.execSQL("INSERT INTO storage_spaces(id,code,storage_unit_id,geometry_profile_id,lifecycleStatus) VALUES(9526,'LEG',9526,9526,'ACTIVE')")
            db.execSQL("INSERT INTO campaigns(id,organization_id,name,status) VALUES(9526,9526,'Legacy campaign','Active')")
            db.execSQL("INSERT INTO stocktaking_records(id,campaign_id,storage_space_id,ownership,status) VALUES(9526,9526,9526,'Owned','To Do')")
            db.execSQL("INSERT INTO measurements(id,stocktaking_record_id,value,timestamp) VALUES(9526,9526,?,1)",arrayOf(1.234567890123))
            db.execSQL("INSERT INTO sync_outbox(client_operation_uuid,op_type,target_ref,payload_json,status,retry_count,createdAt,updatedAt) VALUES('legacy-pending','UPSERT','9526','uninterpreted payload','PENDING',0,1,1)")
        }
        val room=Room.databaseBuilder(instrumentation.targetContext,SiloMathDatabase::class.java,name).build()
        try {
            val db=room.openHelper.writableDatabase
            assertEquals(26,db.version)
            db.query("SELECT value FROM measurements WHERE id=9526").use { it.moveToFirst();assertEquals(1.234567890123,it.getDouble(0),0.0) }
            db.query("SELECT fixed_parameters FROM geometry_versions WHERE id=9526").use { it.moveToFirst();assertEquals("uninterpreted legacy parameters",it.getString(0)) }
            db.query("SELECT payload_json,status FROM sync_outbox WHERE client_operation_uuid='legacy-pending'").use { it.moveToFirst();assertEquals("uninterpreted payload",it.getString(0));assertEquals("PENDING",it.getString(1)) }
            for(table in CanonicalOfflineSchema.tables.values) {
                db.query("SELECT count(*) FROM ${table.localName}").use { it.moveToFirst();assertEquals(0,it.getInt(0)) }
                db.query("PRAGMA table_info(`${table.localName}`)").use { rows ->
                    while(rows.moveToNext()) assertNotEquals("Canonical V6 must not use REAL", "REAL", rows.getString(2).uppercase())
                }
            }
            db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            val id="00000000-0000-0000-0000-000000009526"
            db.execSQL("INSERT INTO v6_geometry_families(geometry_family_id,family_code,name,formula_version,parameter_contract) VALUES(?, 'CYL_FLAT_V1','Cylinder','formula-v1','contract-v1')",arrayOf(id))
            assertThrows(Exception::class.java) { db.execSQL("UPDATE v6_geometry_families SET name='Rewritten' WHERE geometry_family_id=?",arrayOf(id)) }
            db.query("SELECT name FROM v6_geometry_families WHERE geometry_family_id=?",arrayOf(id)).use { it.moveToFirst();assertEquals("Cylinder",it.getString(0)) }
        } finally { room.close() }
    }
}
