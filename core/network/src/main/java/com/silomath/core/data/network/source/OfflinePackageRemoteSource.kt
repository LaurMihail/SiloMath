package com.silomath.core.data.network.source

import com.silomath.core.data.network.api.OfflinePackageApi
import com.silomath.core.data.network.dto.OfflineManifestDto
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

class OfflinePackageHttpException(val status: Int, val retryAfter: String?) : IOException("Offline package HTTP $status")

interface OfflinePackageRemoteSource {
    suspend fun manifest(token: String,organizationId: String,campaignId: String,deviceId: String): OfflineManifestDto
    suspend fun download(token: String,manifest: OfflineManifestDto): InputStream
}

class RestOfflinePackageRemoteSource @Inject constructor(private val api: OfflinePackageApi) : OfflinePackageRemoteSource {
    override suspend fun manifest(token: String,organizationId: String,campaignId: String,deviceId: String)=
        try { api.manifest("Bearer $token",organizationId,campaignId,deviceId) }
        catch (failure: retrofit2.HttpException) {
            throw OfflinePackageHttpException(failure.code(),failure.response()?.headers()?.get("Retry-After"))
        }

    override suspend fun download(token: String,manifest: OfflineManifestDto): InputStream {
        val response=api.download("Bearer $token",manifest.organizationId,manifest.campaignId,
            manifest.deviceInstallationId,manifest.packageVersion,manifest.manifestId)
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw OfflinePackageHttpException(response.code(),response.headers()["Retry-After"])
        }
        val body=response.body() ?: throw IOException("Missing offline package body")
        if(response.headers()["X-Manifest-Id"]!=manifest.manifestId ||
            response.headers()["X-Package-Version"]!=manifest.packageVersion ||
            response.headers()["X-Package-SHA256"]!=manifest.integrityHash) {
            body.close()
            throw IOException("Offline package transport identity mismatch")
        }
        return body.byteStream()
    }
}
