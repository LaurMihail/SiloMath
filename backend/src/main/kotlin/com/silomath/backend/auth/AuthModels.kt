package com.silomath.backend.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val email: String,
    val password: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val recoveryToken: String,
    val newPassword: String
)

@Serializable
data class RefreshSessionRequest(
    val refreshToken: String
)

@Serializable
data class AcceptedResponse(
    val accepted: Boolean = true
)

@Serializable
data class SessionUserSummary(
    val userId: String,
    val fullName: String,
    val email: String
)

@Serializable
data class AuthorizedMembershipSummary(
    val membershipId: String,
    val organizationId: String,
    val organizationName: String,
    val organizationCode: String,
    val role: String
)

@Serializable
data class SessionResponse(
    val user: SessionUserSummary,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val memberships: List<AuthorizedMembershipSummary>
)

internal enum class CanonicalUserLifecycle {
    ACTIVE,
    DISABLED
}

internal data class CanonicalSessionIdentity(
    val user: SessionUserSummary,
    val lifecycle: CanonicalUserLifecycle,
    val memberships: List<AuthorizedMembershipSummary>
)

@Serializable
internal data class SupabasePasswordGrantRequest(
    val email: String,
    val password: String
)

@Serializable
internal data class SupabaseRefreshGrantRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

@Serializable
internal data class SupabaseRecoverRequest(
    val email: String
)

@Serializable
internal data class SupabaseUpdatePasswordRequest(
    val password: String
)

@Serializable
internal data class SupabaseUserDto(
    val id: String
)

@Serializable
internal data class SupabaseSessionDto(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("expires_in")
    val expiresInSeconds: Long? = null,
    @SerialName("expires_at")
    val expiresAtEpochSeconds: Long? = null,
    val user: SupabaseUserDto
)
