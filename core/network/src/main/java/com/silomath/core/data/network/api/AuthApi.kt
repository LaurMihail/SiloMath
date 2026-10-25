package com.silomath.core.data.network.api

import com.silomath.core.data.network.dto.AcceptedResponseDto
import com.silomath.core.data.network.dto.ForgotPasswordRequestDto
import com.silomath.core.data.network.dto.RefreshSessionRequestDto
import com.silomath.core.data.network.dto.ResetPasswordRequestDto
import com.silomath.core.data.network.dto.SessionResponseDto
import com.silomath.core.data.network.dto.SignInRequestDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/sign-in")
    suspend fun signIn(
        @Body request: SignInRequestDto
    ): SessionResponseDto

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequestDto
    ): AcceptedResponseDto

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequestDto
    ): AcceptedResponseDto

    @POST("api/v1/auth/session/refresh")
    suspend fun refreshSession(
        @Body request: RefreshSessionRequestDto
    ): SessionResponseDto

    @POST("api/v1/auth/sign-out")
    suspend fun signOut(
        @Header("Authorization") authorization: String
    )
}
