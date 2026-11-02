package com.silomath.core.data.network.dto

import kotlinx.serialization.Serializable

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
data class CurrentUserResponseDto(
    val user: SessionUserSummaryDto,
    val memberships: List<AuthorizedMembershipSummaryDto>
)
