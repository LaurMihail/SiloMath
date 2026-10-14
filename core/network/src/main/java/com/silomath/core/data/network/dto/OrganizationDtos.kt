package com.silomath.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class OrganizationSummaryDto(
    val id: String
)

@Serializable
data class OrganizationSummaryPageDto(
    val items: List<OrganizationSummaryDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

@Serializable
data class OrganizationResponseDto(
    val id: String,
    val name: String,
    val code: String,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val lifecycleStatus: String,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String,
    val entityVersion: Long
)
