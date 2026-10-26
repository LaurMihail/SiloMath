package com.silomath.core.sync.offline

import com.silomath.core.data.network.dto.OfflineManifestDto
import com.silomath.core.data.network.source.OfflinePackageRemoteSource
import com.silomath.core.database.dao.OfflinePackageDownloadDao
import com.silomath.core.database.entity.OfflinePackageDownloadEntity
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class OfflinePackageDownloadTest {
    @get:Rule val temporary=TemporaryFolder()
    private val bytes="private campaign content".toByteArray()
    private val manifest=OfflineManifestDto(UUID.randomUUID().toString(),"org","campaign","device","version-1",
        buildJsonObject {},"a".repeat(64),"SHA-256",buildJsonObject { put("authorizedUserId","user");put("byteSize",bytes.size) },"2026-10-27T00:00:00Z")
    private val key=SecretKeySpec(ByteArray(32) { 7 },"AES")
    private fun staging()=EncryptedPackageStagingStore(temporary.root) { key }

    @Test fun completeDownloadIsEncryptedAndOnlyVerifying()=runBlocking {
        val journal=Journal();val remote=Remote()
        val record=OfflinePackageDownloadRunner(remote,journal,staging()).run("token","user","org","campaign","device")
        assertEquals("VERIFYING",record.state)
        assertNull(record.activatedAt);assertNull(record.verifiedAt)
        val ref=requireNotNull(record.stagingPrivateRef)
        val disk=temporary.root.resolve(ref).readBytes()
        assertFalse(disk.toString(Charsets.UTF_8).contains("private campaign"))
        assertArrayEquals(bytes,staging().open(ref).use { it.readBytes() })
    }

    @Test fun interruptedTransferRetainsIdentityForRetry()=runBlocking {
        val journal=Journal();val remote=Remote().apply { failure=IOException("Disconnected") }
        val runner=OfflinePackageDownloadRunner(remote,journal,staging())
        try { runner.run("token","user","org","campaign","device");fail() } catch(_: IOException) {}
        assertEquals("DOWNLOAD_ERROR",journal.record!!.state)
        remote.failure=null
        val result=runner.run("token","user","org","campaign","device")
        assertEquals(manifest.manifestId,result.manifestId)
        assertEquals(1,remote.manifestCalls)
        assertEquals("VERIFYING",result.state)
    }

    @Test fun cancellationRetainsRecoverableDownloadWithoutAuthenticationChange()=runBlocking {
        val journal=Journal();val remote=Remote().apply { failure=CancellationException("Stopped") }
        try { OfflinePackageDownloadRunner(remote,journal,staging()).run("token","user","org","campaign","device");fail() }
        catch(_: CancellationException) {}
        assertEquals("DOWNLOADING",journal.record!!.state)
        assertNull(journal.record!!.errorCode)
    }

    @Test fun wrongUserCannotCreateStagingRecord()=runBlocking {
        val journal=Journal()
        try { OfflinePackageDownloadRunner(Remote(),journal,staging()).run("token","another-user","org","campaign","device");fail() }
        catch(_: IllegalArgumentException) {}
        assertNull(journal.record)
    }

    @Test fun truncatedBodyNeverBecomesVerifying()=runBlocking {
        val journal=Journal();val remote=Remote().apply { content=byteArrayOf(1) }
        try { OfflinePackageDownloadRunner(remote,journal,staging()).run("token","user","org","campaign","device");fail() }
        catch(_: IOException) {}
        assertEquals("DOWNLOAD_ERROR",journal.record!!.state)
    }

    @Test fun tamperedEncryptedStagingFailsAuthentication()=runBlocking<Unit> {
        val staged=staging().write(manifest.manifestId,ByteArrayInputStream(bytes),bytes.size.toLong())
        RandomAccessFile(temporary.root.resolve(staged.privateRef),"rw").use { file -> file.seek(file.length()-1);val last=file.read();file.seek(file.length()-1);file.write(last xor 1) }
        assertThrows(IOException::class.java) { staging().open(staged.privateRef).use { it.readBytes() } }
    }

    @Test fun retryAfterOverridesLocalBackoff() {
        assertEquals(120000L,PackageDownloadRetryPolicy.delayMillis(0,"120",0,1.0))
        assertEquals(5000L,PackageDownloadRetryPolicy.delayMillis(1,null,0,1.0))
        assertEquals(300000L,PackageDownloadRetryPolicy.delayMillis(99,null,0,1.0))
        assertEquals(60000L,PackageDownloadRetryPolicy.delayMillis(0,"Thu, 1 Jan 1970 00:01:00 GMT",0,1.0))
    }

    private inner class Remote : OfflinePackageRemoteSource {
        var manifestCalls=0
        var failure: Exception?=null
        var content=bytes
        override suspend fun manifest(token: String,organizationId: String,campaignId: String,deviceId: String): OfflineManifestDto { manifestCalls++;return manifest }
        override suspend fun download(token: String,manifest: OfflineManifestDto): java.io.InputStream {
            failure?.let { throw it };return ByteArrayInputStream(content)
        }
    }
    private class Journal : OfflinePackageDownloadDao {
        var record: OfflinePackageDownloadEntity?=null
        override suspend fun insert(download: OfflinePackageDownloadEntity) { check(record==null);record=download }
        override suspend fun update(download: OfflinePackageDownloadEntity) { record=download }
        override suspend fun get(manifestId: String)=record?.takeIf { it.manifestId==manifestId }
        override fun observeForUser(userId: String): Flow<List<OfflinePackageDownloadEntity>> = flowOf(listOfNotNull(record))
        override suspend fun recoverableForUser(userId: String)=listOfNotNull(record).filter { it.state!="READY" }
    }
}
