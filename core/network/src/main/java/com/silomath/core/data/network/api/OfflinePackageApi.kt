package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.OfflineManifestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface OfflinePackageApi {
    @GET("api/v1/organizations/{organizationId}/campaigns/{campaignId}/offline-manifest")
    suspend fun manifest(@Header("Authorization") bearer: String,
        @Path("organizationId") organizationId: String, @Path("campaignId") campaignId: String,
        @Query("deviceInstallationId") deviceInstallationId: String): OfflineManifestDto

    @Streaming
    @GET("api/v1/organizations/{organizationId}/campaigns/{campaignId}/offline-package")
    suspend fun download(@Header("Authorization") bearer: String,
        @Path("organizationId") organizationId: String, @Path("campaignId") campaignId: String,
        @Query("deviceInstallationId") deviceInstallationId: String,
        @Query("packageVersion") packageVersion: String, @Query("manifestId") manifestId: String): Response<ResponseBody>
}
