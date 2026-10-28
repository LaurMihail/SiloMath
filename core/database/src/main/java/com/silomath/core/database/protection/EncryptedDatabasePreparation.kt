package com.silomath.core.database.protection

import android.content.Context
import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import net.zetetic.database.DatabaseErrorHandler
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase

internal enum class ConversionCheckpoint { EXPORTED, VERIFIED, PROMOTED }

/** Runs before the operational helper opens; all names are local static database identifiers. */
internal class EncryptedDatabasePreparation(
    private val context: Context,
    private val name: String,
    private val checkpoint: (ConversionCheckpoint)->Unit = {}
) {
    val source=context.getDatabasePath(name)
    val candidate=File(source.path+".encrypted.pending")
    private val privateDirectory=File(context.noBackupFilesDir,"protected-room")
    private val proof=AtomicFile(File(privateDirectory,"$name.conversion"))
    private val initialized=AtomicFile(File(privateDirectory,"$name.initialized"))

    fun mayCreateSecret(): Boolean =
        !has(initialized) && !has(proof) && !candidate.exists() && (!source.exists() || isPlaintext(source))

    fun <T> locked(action: ()->T): T {
        check(privateDirectory.isDirectory || privateDirectory.mkdirs())
        RandomAccessFile(File(privateDirectory,"$name.lock"),"rw").use { file ->
            file.channel.lock().use { return action() }
        }
    }

    fun prepare(secret: ByteArray) = protect(DatabaseProtectionException.Reason.CONVERSION_FAILED) {
        check(source.parentFile!!.isDirectory || source.parentFile!!.mkdirs())
        if(has(initialized)) checkAuthenticated(initialized,"initialized".toByteArray(),secret)
        if(!source.exists()) {
            if(candidate.exists() && has(proof)) {
                val expected=readProof(secret)
                verifyCandidate(expected,secret)
                promote(secret)
            } else if(candidate.exists() || has(proof) || has(initialized)) {
                throw DatabaseProtectionException(DatabaseProtectionException.Reason.DATABASE_MISSING)
            }
            return@protect
        }
        if(source.length()<16) throw DatabaseProtectionException(DatabaseProtectionException.Reason.DATABASE_INVALID)
        if(!isPlaintext(source)) {
            openExisting(source,secret,true).use { db -> db.query("SELECT count(*) FROM sqlite_master").use { check(it.moveToFirst()) } }
            if(has(proof)) {
                val expected=readProof(secret)
                check(MessageDigest.isEqual(hashFile(source),expected.copyOfRange(32,64)))
                openExisting(source,secret,true).use { check(MessageDigest.isEqual(DatabaseContentsFingerprint.capture(it),expected.copyOfRange(0,32))) }
                proof.delete()
            }
            if(candidate.exists()) throw DatabaseProtectionException(DatabaseProtectionException.Reason.RECOVERY_REQUIRED)
            return@protect
        }
        openExisting(source,byteArrayOf(),false).use { plain ->
            plain.query("PRAGMA wal_checkpoint(TRUNCATE)").use { rows -> if(rows.moveToFirst()) check(rows.getInt(0)==0) }
            plain.disableWriteAheadLogging()
            if(has(proof)) {
                val expected=readProof(secret)
                plain.beginTransaction()
                try {
                    check(MessageDigest.isEqual(DatabaseContentsFingerprint.capture(plain),expected.copyOfRange(0,32)))
                    verifyCandidate(expected,secret)
                } finally { plain.endTransaction() }
            } else {
                if(candidate.exists()) {
                    // Unverified encrypted staging is retained for recovery, never promoted or substituted for the source.
                    Os.rename(candidate.path,candidate.path+".unverified."+UUID.randomUUID())
                }
                // The source deliberately lacks CREATE_IF_NECESSARY; ATTACH inherits that restriction.
                // Create only the new staging file, never the existing operational database.
                check(candidate.createNewFile())
                plain.execSQL("ATTACH DATABASE ? AS encrypted KEY ?",arrayOf(candidate.absolutePath,secret))
                val before: ByteArray
                plain.beginTransaction()
                try {
                    before=DatabaseContentsFingerprint.capture(plain)
                    plain.query("SELECT sqlcipher_export('encrypted')").use { check(it.moveToFirst()) }
                    plain.execSQL("PRAGMA encrypted.user_version = ${plain.version}")
                    val hasSequence=plain.query("SELECT 1 FROM sqlite_master WHERE name='sqlite_sequence'").use { it.moveToFirst() }
                    if(hasSequence) {
                        // Preserve the technical AUTOINCREMENT high-water mark, including removed historical IDs.
                        plain.execSQL("DELETE FROM encrypted.sqlite_sequence")
                        plain.execSQL("INSERT INTO encrypted.sqlite_sequence SELECT * FROM main.sqlite_sequence")
                    }
                    plain.setTransactionSuccessful()
                } finally { plain.endTransaction() }
                plain.execSQL("DETACH DATABASE encrypted")
                checkpoint(ConversionCheckpoint.EXPORTED)
                plain.beginTransaction()
                try {
                    check(MessageDigest.isEqual(DatabaseContentsFingerprint.capture(plain),before))
                    openExisting(candidate,secret,true).use { encrypted ->
                        check(MessageDigest.isEqual(DatabaseContentsFingerprint.capture(encrypted),before))
                        encrypted.query("PRAGMA cipher_integrity_check").use { result ->
                            while(result.moveToNext()) check(result.getString(0).equals("ok",true))
                        }
                    }
                    RandomAccessFile(candidate,"rw").use { it.fd.sync() }
                    writeAuthenticated(proof,before+hashFile(candidate),secret)
                    checkpoint(ConversionCheckpoint.VERIFIED)
                } finally { plain.endTransaction() }
            }
        }
        removeClosedSidecars()
        promote(secret)
    }

    fun markInitialized(secret: ByteArray) {
        if(!has(initialized)) writeAuthenticated(initialized,"initialized".toByteArray(),secret)
    }

    private fun verifyCandidate(expected: ByteArray,secret: ByteArray) {
        check(candidate.exists() && !isPlaintext(candidate))
        check(MessageDigest.isEqual(hashFile(candidate),expected.copyOfRange(32,64)))
        openExisting(candidate,secret,true).use { db ->
            check(MessageDigest.isEqual(DatabaseContentsFingerprint.capture(db),expected.copyOfRange(0,32)))
        }
    }

    private fun promote(secret: ByteArray) {
        check(candidate.exists() && has(proof))
        // POSIX rename in the same directory is the only promotion point; it replaces the physical file, never its logical rows.
        Os.rename(candidate.absolutePath,source.absolutePath)
        val directory=Os.open(source.parent!!,OsConstants.O_RDONLY,0)
        try { Os.fsync(directory) } finally { Os.close(directory) }
        checkpoint(ConversionCheckpoint.PROMOTED)
        proof.delete()
        markInitialized(secret)
    }

    private fun removeClosedSidecars() {
        val wal=File(source.path+"-wal")
        check(!wal.exists() || wal.length()==0L)
        for(suffix in listOf("-wal","-shm","-journal")) {
            val file=File(source.path+suffix)
            if(file.exists()) check(file.delete()) { "Cannot retire closed source sidecar" }
        }
    }

    private fun readProof(secret: ByteArray): ByteArray = protect(DatabaseProtectionException.Reason.VERIFICATION_FAILED) {
        val value=readAuthenticated(proof,secret)
        check(value.size==64);value
    }
    private fun checkAuthenticated(file: AtomicFile,expected: ByteArray,secret: ByteArray) {
        check(MessageDigest.isEqual(readAuthenticated(file,secret),expected))
    }
    private fun writeAuthenticated(file: AtomicFile,value: ByteArray,secret: ByteArray) {
        val signature=signature(file.baseFile.name,value,secret)
        val output=file.startWrite()
        try {
            val data=DataOutputStream(output)
            data.writeInt(value.size);data.write(value);data.write(signature);data.flush()
            file.finishWrite(output)
        } catch(failure: Exception) { file.failWrite(output);throw failure }
    }
    private fun readAuthenticated(file: AtomicFile,secret: ByteArray): ByteArray {
        DataInputStream(file.openRead()).use { input ->
            val length=input.readInt();check(length in 1..128)
            val value=ByteArray(length).also(input::readFully)
            val actual=ByteArray(32).also(input::readFully)
            check(input.read()==-1 && MessageDigest.isEqual(actual,signature(file.baseFile.name,value,secret)))
            return value
        }
    }
    private fun signature(scope: String,value: ByteArray,secret: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(secret,"HmacSHA256"));update(("SiloMath.Room.State.v1|$name|$scope|").toByteArray());doFinal(value)
        }
    private fun hashFile(file: File): ByteArray {
        val digest=MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input -> val buffer=ByteArray(65536);while(true) { val n=input.read(buffer);if(n<0) break;digest.update(buffer,0,n) } }
        return digest.digest()
    }
    private fun has(file: AtomicFile)=file.baseFile.exists() || File(file.baseFile.path+".bak").exists()

    companion object {
        internal fun isPlaintext(file: File): Boolean = FileInputStream(file).use { input ->
            val bytes=ByteArray(16);input.read(bytes)==16 && bytes.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
        }
        internal fun openExisting(file: File,key: ByteArray,readOnly: Boolean): CipherDatabase {
            val handler=DatabaseErrorHandler { _, _ -> throw DatabaseProtectionException(DatabaseProtectionException.Reason.DATABASE_INVALID) }
            return CipherDatabase.openDatabase(file.path,key,null,
                (if(readOnly) CipherDatabase.OPEN_READONLY else CipherDatabase.OPEN_READWRITE) or CipherDatabase.NO_LOCALIZED_COLLATORS,handler,null)
        }
    }
}
