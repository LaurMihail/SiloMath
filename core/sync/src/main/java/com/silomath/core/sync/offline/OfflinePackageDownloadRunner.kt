package com.silomath.core.sync.offline

import com.silomath.core.data.network.dto.OfflineManifestDto
import com.silomath.core.data.network.source.OfflinePackageRemoteSource
import com.silomath.core.data.network.source.OfflinePackageHttpException
import com.silomath.core.database.dao.OfflinePackageDownloadDao
import com.silomath.core.database.entity.OfflinePackageDownloadEntity
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/** Transfer only: a completed download is VERIFYING, never READY. */
class OfflinePackageDownloadRunner(
    private val remote: OfflinePackageRemoteSource,
    private val journal: OfflinePackageDownloadDao,
    private val staging: PackageStagingStore,
    private val json: Json = Json { encodeDefaults=true }
) {
    suspend fun run(token: String,userId: String,organizationId: String,campaignId: String,deviceId: String): OfflinePackageDownloadEntity {
        val resumable=journal.recoverableForUser(userId).firstOrNull {
            it.organizationId==organizationId && it.campaignId==campaignId && it.deviceInstallationId==deviceId && it.errorCode!="PACKAGE_HTTP_404"
        }
        if(resumable?.state=="VERIFYING") return resumable
        val manifest=if(resumable!=null) json.decodeFromString<OfflineManifestDto>(resumable.manifestJson)
            else remote.manifest(token,organizationId,campaignId,deviceId)
        require(manifest.organizationId==organizationId && manifest.campaignId==campaignId && manifest.deviceInstallationId==deviceId)
        require(manifest.verificationMetadata?.get("authorizedUserId")?.jsonPrimitive?.content==userId)
        val expected=manifest.verificationMetadata?.get("byteSize")?.jsonPrimitive?.long ?: error("Missing package length")
        var record=resumable ?: OfflinePackageDownloadEntity(
            manifestId=manifest.manifestId,campaignId=campaignId,organizationId=organizationId,
            deviceInstallationId=deviceId,authorizedUserId=userId,packageVersion=manifest.packageVersion,
            state="DOWNLOAD_PENDING",manifestJson=json.encodeToString(manifest),stagingPrivateRef=null,
            downloadedBytes=0,verifiedAt=null,activatedAt=null,errorCode=null,updatedAt=System.currentTimeMillis()
        ).also { journal.insert(it) }
        try {
            record=record.copy(state="DOWNLOADING",errorCode=null,updatedAt=System.currentTimeMillis())
            journal.update(record)
            val staged=remote.download(token,manifest).use { staging.write(manifest.manifestId,it,expected) }
            record=record.copy(state="VERIFYING",stagingPrivateRef=staged.privateRef,downloadedBytes=staged.byteCount,
                updatedAt=System.currentTimeMillis())
            journal.update(record)
            return record
        } catch(cancelled: CancellationException) {
            // Durable DOWNLOADING recovers with the same manifest/version. No credential state is changed.
            throw cancelled
        } catch(failure: Exception) {
            journal.update(record.copy(state="DOWNLOAD_ERROR",errorCode=(failure as? OfflinePackageHttpException)?.let { "PACKAGE_HTTP_${it.status}" } ?: "PACKAGE_TRANSFER_FAILED",updatedAt=System.currentTimeMillis()))
            throw failure
        }
    }
}
