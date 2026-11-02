package com.silomath.core.database.protection

import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlainDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.offline.CanonicalOfflineGuards
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedRoomDatabaseTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private fun name()="sqlcipher-test-"+UUID.randomUUID()+".db"
    private fun encrypted(name: String,configure: (EncryptedRoomOpenHelperFactory)->Unit={}): SiloMathDatabase {
        val factory=EncryptedRoomOpenHelperFactory(context).also(configure)
        return Room.databaseBuilder(context,SiloMathDatabase::class.java,name).openHelperFactory(factory).build()
    }
    private fun touch(db: SiloMathDatabase)=db.openHelper.writableDatabase
    private fun hash(file: File)=MessageDigest.getInstance("SHA-256").digest(file.readBytes())

    @Test fun freshInstallationUsesWrappedRandomSecretAndReopensEncrypted() {
        val name=name();val file=context.getDatabasePath(name)
        val first=encrypted(name)
        try { touch(first).execSQL("INSERT INTO organizations(name) VALUES('Protected installation')") } finally { first.close() }
        assertFalse(EncryptedDatabasePreparation.isPlaintext(file))
        val keys=KeystoreDatabaseSecret(context,name)
        val key=keys.loadOrCreate(false)
        try {
            assertEquals(32,key.size)
            assertTrue(keys.envelopeFile.exists())
            assertFalse(keys.envelopeFile.readBytes().asList().windowed(32).any { it==key.asList() })
        } finally { key.fill(0) }
        val again=encrypted(name)
        try { touch(again).query("SELECT name FROM organizations").use { it.moveToFirst();assertEquals("Protected installation",it.getString(0)) } }
        finally { again.close() }
        assertTrue(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(keys.alias))
    }

    @Test fun populatedPlaintextConversionPreservesEveryTableAndPendingOperation() {
        val name=name();val before=plainFixture(name)
        val db=encrypted(name)
        try {
            val sql=touch(db)
            assertArrayEquals(before,DatabaseContentsFingerprint.capture(sql))
            assertPending(sql)
            sql.query("SELECT diameter_m,typeof(diameter_m) FROM v6_geometry_profile_versions").use {
                it.moveToFirst();assertEquals("12345678901234.123456",it.getString(0));assertEquals("text",it.getString(1))
            }
        } finally { db.close() }
        assertFalse(EncryptedDatabasePreparation.isPlaintext(context.getDatabasePath(name)))
    }

    @Test fun standardSqliteCannotReadProtectedDatabaseAndDoesNotDeleteIt() {
        val name=name();plainFixture(name)
        val db=encrypted(name);try { touch(db) } finally { db.close() }
        val file=context.getDatabasePath(name);val before=hash(file)
        assertThrows(Exception::class.java) {
            PlainDatabase.openDatabase(file.path,null,PlainDatabase.OPEN_READONLY,
                android.database.DatabaseErrorHandler { throw IllegalStateException("Test preserves protected files") }).use {
                it.rawQuery("SELECT count(*) FROM organizations",null).use { rows -> rows.moveToFirst() }
            }
        }
        assertArrayEquals(before,hash(file))
    }

    @Test fun wrongKeyFailsClosedWithoutCorruptionDeletion() {
        val name=name();plainFixture(name)
        val db=encrypted(name);try { touch(db) } finally { db.close() }
        val file=context.getDatabasePath(name);val before=hash(file)
        NativeSqlCipher.load()
        assertThrows(Exception::class.java) {
            EncryptedDatabasePreparation.openExisting(file,ByteArray(32) { 42 },true).use { it.query("SELECT count(*) FROM organizations").use { rows -> rows.moveToFirst() } }
        }
        val wrong=encrypted(name) { it.secretOverride=DatabaseSecretSource { ByteArray(32) { 42 } } }
        try { assertThrows(DatabaseProtectionException::class.java) { touch(wrong) } } finally { wrong.close() }
        assertArrayEquals(before,hash(file))
        val correct=encrypted(name);try { assertPending(touch(correct)) } finally { correct.close() }
    }

    @Test fun missingEnvelopeNeverRegeneratesOverExistingData() {
        val name=name();plainFixture(name)
        val db=encrypted(name);try { touch(db) } finally { db.close() }
        val file=context.getDatabasePath(name);val before=hash(file)
        val keys=KeystoreDatabaseSecret(context,name)
        assertTrue(keys.envelopeFile.delete()) // Test-owned secret envelope only.
        val reopened=encrypted(name)
        try { assertThrows(DatabaseProtectionException::class.java) { touch(reopened) } } finally { reopened.close() }
        assertFalse(keys.envelopeFile.exists());assertArrayEquals(before,hash(file))
    }

    @Test fun corruptEnvelopeNeverCreatesAnEmptyReplacement() {
        val name=name();plainFixture(name)
        val db=encrypted(name);try { touch(db) } finally { db.close() }
        val file=context.getDatabasePath(name);val before=hash(file)
        val envelope=KeystoreDatabaseSecret(context,name).envelopeFile
        RandomAccessFile(envelope,"rw").use { it.seek(it.length()-1);val value=it.read();it.seek(it.length()-1);it.write(value xor 1) }
        val reopened=encrypted(name)
        try { assertThrows(DatabaseProtectionException::class.java) { touch(reopened) } } finally { reopened.close() }
        assertArrayEquals(before,hash(file))
    }

    @Test fun unavailableWrappingKeyPreservesRecoverableDatabase() {
        val name=name();plainFixture(name)
        val db=encrypted(name);try { touch(db) } finally { db.close() }
        val file=context.getDatabasePath(name);val before=hash(file)
        val keys=KeystoreDatabaseSecret(context,name)
        KeyStore.getInstance("AndroidKeyStore").apply { load(null);deleteEntry(keys.alias) } // Test-owned alias only.
        val reopened=encrypted(name)
        try { assertThrows(DatabaseProtectionException::class.java) { touch(reopened) } } finally { reopened.close() }
        assertArrayEquals(before,hash(file))
    }

    @Test fun initializedMissingDatabaseIsNotSilentlyRecreated() {
        val name=name();plainFixture(name)
        val db=encrypted(name);try { touch(db) } finally { db.close() }
        val file=context.getDatabasePath(name)
        val retained=File(context.noBackupFilesDir,name+".test-retained")
        assertTrue(file.renameTo(retained)) // Preserve the test data while simulating a missing main file.
        val before=hash(retained)
        val reopened=encrypted(name)
        try { assertThrows(DatabaseProtectionException::class.java) { touch(reopened) } } finally { reopened.close() }
        assertFalse(file.exists());assertArrayEquals(before,hash(retained))
    }

    @Test fun interruptionsBeforeAndAfterPromotionRecoverWithoutDataLoss() {
        for(fault in ConversionCheckpoint.entries) {
            val name=name();val before=plainFixture(name)
            val interrupted=encrypted(name) { factory -> factory.checkpoint={ if(it==fault) throw IOException("Controlled interruption") } }
            try { assertThrows(DatabaseProtectionException::class.java) { touch(interrupted) } } finally { interrupted.close() }
            val recovered=encrypted(name)
            try { assertArrayEquals(before,DatabaseContentsFingerprint.capture(touch(recovered)));assertPending(touch(recovered)) }
            finally { recovered.close() }
        }
    }

    @Test fun failedCandidateVerificationNeverPromotesOrChangesOriginalData() {
        val name=name();val before=plainFixture(name)
        val file=context.getDatabasePath(name)
        val broken=encrypted(name) { factory -> factory.checkpoint={ phase ->
            if(phase==ConversionCheckpoint.EXPORTED) {
                RandomAccessFile(File(file.path+".encrypted.pending"),"rw").use { out -> out.seek(0);val value=out.read();out.seek(0);out.write(value xor 1) }
            }
        }}
        try { assertThrows(DatabaseProtectionException::class.java) { touch(broken) } } finally { broken.close() }
        assertTrue(EncryptedDatabasePreparation.isPlaintext(file))
        NativeSqlCipher.load()
        EncryptedDatabasePreparation.openExisting(file,byteArrayOf(),true).use {
            assertArrayEquals(before,DatabaseContentsFingerprint.capture(it));assertPending(it)
        }
        val recovered=encrypted(name)
        try { assertArrayEquals(before,DatabaseContentsFingerprint.capture(touch(recovered))) } finally { recovered.close() }
    }

    private fun assertPending(db: SupportSQLiteDatabase) {
        db.query("SELECT payload_json,status FROM sync_outbox WHERE client_operation_uuid='legacy-pending'").use {
            assertTrue(it.moveToFirst());assertEquals("{\"legacy\":true}",it.getString(0));assertEquals("PENDING",it.getString(1))
        }
        db.query("SELECT state,base_entity_version,payload_json FROM sync_operations").use {
            assertTrue(it.moveToFirst());assertEquals("PENDING",it.getString(0));assertTrue(it.isNull(1));assertEquals("{\"exact\":\"1.000001\"}",it.getString(2))
        }
    }

    private fun plainFixture(name: String): ByteArray {
        val room=Room.databaseBuilder(context,SiloMathDatabase::class.java,name).build()
        try {
            val db=touch(room)
            CanonicalOfflineGuards.install(db)
            db.execSQL("INSERT INTO organizations(id,name) VALUES(31003,'Retained legacy')")
            db.execSQL("INSERT INTO organizations(id,name) VALUES(91003,'Sequence sentinel')")
            db.execSQL("DELETE FROM organizations WHERE id=91003") // Fixture establishes an AUTOINCREMENT high-water mark.
            db.execSQL("INSERT INTO sync_outbox(client_operation_uuid,op_type,target_ref,payload_json,status,retry_count,createdAt,updatedAt) VALUES('legacy-pending','UPSERT','31003',?,'PENDING',0,1,1)",arrayOf("{\"legacy\":true}"))
            val user="00000000-0000-0000-0000-000000003101"
            val org="00000000-0000-0000-0000-000000003102"
            val family="00000000-0000-0000-0000-000000003103"
            val profile="00000000-0000-0000-0000-000000003104"
            val version="00000000-0000-0000-0000-000000003105"
            db.beginTransaction()
            try {
                db.execSQL("INSERT INTO v6_users(user_id,full_name,email,email_normalized,lifecycle_status,created_at,updated_at,updated_by,entity_version) VALUES(?,'Test User','user@example.test','user@example.test','ACTIVE',1,1,?,1)",arrayOf(user,user))
                db.execSQL("INSERT INTO v6_organizations(organization_id,name,code,lifecycle_status,created_at,created_by,updated_at,updated_by,entity_version) VALUES(?,'Canonical','CANON','ACTIVE',1,?,1,?,1)",arrayOf(org,user,user))
                db.execSQL("INSERT INTO v6_geometry_families(geometry_family_id,family_code,name,formula_version,parameter_contract) VALUES(?,'CYL_FLAT_V1','Cylinder','v1','test-contract')",arrayOf(family))
                db.execSQL("INSERT INTO v6_geometry_profiles(geometry_profile_id,organization_id,name,code,geometry_family_id,lifecycle_status,current_version_id,created_at,created_by,updated_at,updated_by,entity_version) VALUES(?,?,'Profile','PROFILE',?,'ACTIVE',?,1,?,1,?,1)",arrayOf(profile,org,family,version,user,user))
                db.execSQL("INSERT INTO v6_geometry_profile_versions(geometry_profile_version_id,geometry_profile_id,version_number,geometry_family_id,diameter_m,height_m,measurement_reference_height_m,maximum_fill_height_m,geometry_origin,max_capacity_m3,formula_version,schema_version,created_at,created_by) VALUES(?,?,1,?,'12345678901234.123456','10.000000','10.000000','10.000000','BOTTOM','31.415927','v1','v1',1,?)",arrayOf(version,profile,family,user))
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            db.execSQL("INSERT INTO sync_operations(sync_operation_id,idempotency_key,organization_id,entity_type,entity_id,operation_type,event_at,payload_json,state,attempt_count,created_at,updated_at) VALUES(?,?,?,'STOCKTAKING_RECORD',?,'CREATE',1,?,'PENDING',0,1,1)",arrayOf(UUID.randomUUID().toString(),UUID.randomUUID().toString(),org,UUID.randomUUID().toString(),"{\"exact\":\"1.000001\"}"))
            return DatabaseContentsFingerprint.capture(db)
        } finally { room.close() }
    }
}
