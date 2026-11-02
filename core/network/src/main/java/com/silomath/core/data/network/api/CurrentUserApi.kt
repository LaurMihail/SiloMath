package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.CurrentUserResponseDto
import retrofit2.http.GET
import retrofit2.http.Header

interface CurrentUserApi {

    @GET("me")
    suspend fun getCurrentUser(
        @Header("Authorization")
        authorization: String
    ): CurrentUserResponseDto
}
