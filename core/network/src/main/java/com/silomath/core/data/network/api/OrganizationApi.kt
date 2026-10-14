package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.OrganizationResponseDto
import com.silomath.core.data.network.dto.OrganizationSummaryPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OrganizationApi {

    @GET("api/v1/organizations")
    suspend fun getOrganizations(
        @Query("cursor")
        cursor: String? = null
    ): OrganizationSummaryPageDto

    @GET("api/v1/organizations/{organizationId}")
    suspend fun getOrganization(
        @Path("organizationId")
        organizationId: String
    ): OrganizationResponseDto
}
