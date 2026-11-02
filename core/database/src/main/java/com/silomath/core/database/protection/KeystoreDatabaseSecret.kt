package com.silomath.core.database.protection

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal fun interface DatabaseSecretSource {
    fun loadOrCreate(allowCreate: Boolean): ByteArray
}

/** Only an authenticated envelope is persisted; raw secrets never leave the database-opening boundary. */
internal class KeystoreDatabaseSecret(private val context: Context, private val databaseName: String) : DatabaseSecretSource {
    internal val directory=File(context.noBackupFilesDir,"protected-room")
    internal val envelopeFile=File(directory,"$databaseName.key")
    internal val alias="silomath.room.wrap.v1."+MessageDigest.getInstance("SHA-256")
        .digest(databaseName.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)
    private val aad=("SiloMath.Room.Secret.v1|"+context.packageName+"|"+databaseName).toByteArray(Charsets.UTF_8)

    override fun loadOrCreate(allowCreate: Boolean): ByteArray = protect(DatabaseProtectionException.Reason.KEY_UNAVAILABLE) {
        check(directory.isDirectory || directory.mkdirs())
        val atomic=AtomicFile(envelopeFile)
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val exists=envelopeFile.exists() || File(envelopeFile.path+".bak").exists()
        if(exists) {
            val wrapping=store.getKey(alias,null) as? SecretKey
                ?: throw DatabaseProtectionException(DatabaseProtectionException.Reason.KEY_UNAVAILABLE)
            return@protect protect(DatabaseProtectionException.Reason.KEY_ENVELOPE_INVALID) {
                DataInputStream(atomic.openRead()).use { input ->
                    check(input.readInt()==0x534d4b31)
                    check(input.readUnsignedByte()==12)
                    val iv=ByteArray(12).also(input::readFully)
                    val encrypted=ByteArray(48).also(input::readFully)
                    check(input.read()==-1)
                    Cipher.getInstance("AES/GCM/NoPadding").run {
                        init(Cipher.DECRYPT_MODE,wrapping,GCMParameterSpec(128,iv));updateAAD(aad)
                        doFinal(encrypted).also { check(it.size==32) }
                    }
                }
            }
        }
        if(!allowCreate) throw DatabaseProtectionException(DatabaseProtectionException.Reason.KEY_UNAVAILABLE)
        val wrapping=(store.getKey(alias,null) as? SecretKey) ?: run {
            check(!store.containsAlias(alias))
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).setUserAuthenticationRequired(false).build())
            }.generateKey()
        }
        val secret=ByteArray(32).also(SecureRandom()::nextBytes)
        try {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE,wrapping);updateAAD(aad)
            }
            val encrypted=cipher.doFinal(secret)
            val output=atomic.startWrite()
            try {
                val data=DataOutputStream(output)
                data.writeInt(0x534d4b31);data.writeByte(cipher.iv.size);data.write(cipher.iv);data.write(encrypted);data.flush()
                atomic.finishWrite(output)
            } catch(failure: Exception) { atomic.failWrite(output);throw failure }
            secret
        } catch(failure: Exception) { secret.fill(0);throw failure }
    }
}
