package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.CampaignLaunchRequestDto
import com.silomath.core.data.network.dto.CampaignResponseDto
import com.silomath.core.data.network.dto.CampaignScopeItemResponseDto
import com.silomath.core.data.network.dto.CampaignSummaryPageDto
import com.silomath.core.data.network.dto.CampaignTeamAssignmentPageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CampaignApi {

    @GET("api/v1/organizations/{organizationId}/campaigns")
    suspend fun getCampaigns(
        @Path("organizationId")
        organizationId: String,
        @Query("cursor")
        cursor: String? = null
    ): CampaignSummaryPageDto

    @POST("api/v1/organizations/{organizationId}/campaigns:launch")
    suspend fun launchCampaign(
        @Path("organizationId")
        organizationId: String,
        @Header("Idempotency-Key")
        idempotencyKey: String,
        @Body
        request: CampaignLaunchRequestDto
    ): CampaignResponseDto

    @GET("api/v1/organizations/{organizationId}/campaigns/{campaignId}/scope")
    suspend fun getCampaignScope(
        @Path("organizationId")
        organizationId: String,
        @Path("campaignId")
        campaignId: String,
        @Query("cursor")
        cursor: String? = null
    ): CampaignScopeItemResponseDto

    @GET("api/v1/organizations/{organizationId}/campaigns/{campaignId}/team")
    suspend fun getCampaignTeam(
        @Path("organizationId")
        organizationId: String,
        @Path("campaignId")
        campaignId: String,
        @Query("cursor")
        cursor: String? = null
    ): CampaignTeamAssignmentPageDto
}
