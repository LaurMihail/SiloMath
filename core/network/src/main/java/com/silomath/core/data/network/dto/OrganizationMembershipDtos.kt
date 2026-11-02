package com.silomath.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserMembershipResponseDto(
    val membershipId: String,
    val organizationId: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val userLifecycleStatus: String,
    val role: String,
    val membershipLifecycleStatus: String,
    val disabledAt: String? = null,
    val disabledBy: String? = null,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String,
    val entityVersion: Long
)

@Serializable
data class UserMembershipPageDto(
    val items: List<UserMembershipResponseDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)