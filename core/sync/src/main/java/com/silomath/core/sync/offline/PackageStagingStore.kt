package com.silomath.core.sync.offline

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

data class StagedPackage(val privateRef: String,val byteCount: Long)

interface PackageStagingStore {
    suspend fun write(manifestId: String,source: InputStream,expectedBytes: Long): StagedPackage
    fun open(privateRef: String): InputStream
}

/** No plaintext package file, no fallback when Keystore material is unavailable. */
class EncryptedPackageStagingStore(private val directory: File,private val key: () -> SecretKey) : PackageStagingStore {
    override suspend fun write(manifestId: String,source: InputStream,expectedBytes: Long): StagedPackage {
        require(expectedBytes>0)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create private package staging" }
        val target=File(directory,UUID.fromString(manifestId).toString()+".encrypted")
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        var count=0L
        FileOutputStream(target).use { out ->
            out.write(1);out.write(cipher.iv.size);out.write(cipher.iv)
            val buffer=ByteArray(64*1024)
            while(true) {
                coroutineContext.ensureActive()
                val size=source.read(buffer)
                if(size<0) break
                if(size==0) continue
                count+=size
                if(count>expectedBytes) throw IOException("Package exceeds manifest length")
                cipher.update(buffer,0,size)?.let(out::write)
            }
            if(count!=expectedBytes) throw IOException("Incomplete package body")
            out.write(cipher.doFinal())
            out.fd.sync()
        }
        return StagedPackage(target.name,count)
    }

    override fun open(privateRef: String): InputStream {
        require(privateRef.matches(Regex("[0-9a-f-]{36}\\.encrypted")))
        val input=FileInputStream(File(directory,privateRef))
        try {
            check(input.read()==1) { "Unsupported staging encryption format" }
            val ivLength=input.read()
            check(ivLength==12) { "Invalid encryption IV" }
            val iv=ByteArray(ivLength)
            var offset=0
            while(offset<iv.size) {
                val read=input.read(iv,offset,iv.size-offset)
                check(read>0) { "Incomplete encryption header" }
                offset+=read
            }
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv))
            return CipherInputStream(input,cipher)
        } catch (failure: Exception) {
            input.close();throw failure
        }
    }
}
