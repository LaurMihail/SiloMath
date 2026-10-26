package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.LocationCreateRequestDto
import com.silomath.core.data.network.dto.LocationPageDto
import com.silomath.core.data.network.dto.LocationResponseDto
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LocationApi {

    @GET("api/v1/organizations/{organizationId}/locations")
    suspend fun getLocations(
        @Path("organizationId")
        organizationId: String,
        @Query("cursor")
        cursor: String? = null,
        @Query("search")
        search: String? = null,
        @Query("status")
        status: String? = null
    ): LocationPageDto

    @GET(
        "api/v1/organizations/{organizationId}/locations/{locationId}"
    )
    suspend fun getLocation(
        @Path("organizationId")
        organizationId: String,
        @Path("locationId")
        locationId: String
    ): LocationResponseDto

    @POST("api/v1/organizations/{organizationId}/locations")
    suspend fun createLocation(
        @Path("organizationId")
        organizationId: String,
        @Header("Idempotency-Key")
        idempotencyKey: String,
        @Body
        request: LocationCreateRequestDto
    ): LocationResponseDto

    @PATCH(
        "api/v1/organizations/{organizationId}/locations/{locationId}"
    )
    suspend fun patchLocation(
        @Path("organizationId")
        organizationId: String,
        @Path("locationId")
        locationId: String,
        @Header("Idempotency-Key")
        idempotencyKey: String,
        @Header("If-Match-Entity-Version")
        baseEntityVersion: Long,
        @Body
        request: JsonObject
    ): LocationResponseDto
}
