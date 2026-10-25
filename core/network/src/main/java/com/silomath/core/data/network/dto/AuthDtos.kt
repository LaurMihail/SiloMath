package com.silomath.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignInRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class ForgotPasswordRequestDto(
    val email: String
)

@Serializable
data class ResetPasswordRequestDto(
    val recoveryToken: String,
    val newPassword: String
)

@Serializable
data class RefreshSessionRequestDto(
    val refreshToken: String
)

@Serializable
data class AcceptedResponseDto(
    val accepted: Boolean
)

@Serializable
data class SessionUserSummaryDto(
    val userId: String,
    val fullName: String,
    val email: String
)

@Serializable
data class AuthorizedMembershipSummaryDto(
    val membershipId: String,
    val organizationId: String,
    val organizationName: String,
    val organizationCode: String,
    val role: String
)

@Serializable
data class SessionResponseDto(
    val user: SessionUserSummaryDto,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val memberships: List<AuthorizedMembershipSummaryDto>
)
