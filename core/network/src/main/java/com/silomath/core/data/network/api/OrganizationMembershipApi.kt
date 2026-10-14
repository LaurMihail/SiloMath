package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.UserMembershipPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OrganizationMembershipApi {

    @GET("api/v1/organizations/{organizationId}/users")
    suspend fun getOrganizationUsers(
        @Path("organizationId")
        organizationId: String,
        @Query("cursor")
        cursor: String? = null
    ): UserMembershipPageDto
}