package com.silomath.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CampaignLaunchTeamMemberDto(
    val userId: String
)

@Serializable
data class CampaignLaunchRequestDto(
    val name: String,
    val inventoryType: String,
    val startDate: String,
    val plannedEndDate: String,
    val scopeStorageSpaceIds: List<String>,
    val team: List<CampaignLaunchTeamMemberDto>
)

@Serializable
data class CampaignResponseDto(
    val id: String,
    val organizationId: String,
    val campaignCode: String,
    val name: String,
    val inventoryType: String,
    val startDate: String,
    val plannedEndDate: String,
    val status: String,
    val launchedAt: String,
    val launchedBy: String,
    val completedAt: String? = null,
    val completedBy: String? = null,
    val campaignSnapshotId: String,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String,
    val entityVersion: Long
)

@Serializable
data class CampaignSummaryDto(
    val id: String,
    val organizationId: String,
    val campaignCode: String,
    val name: String,
    val inventoryType: String,
    val startDate: String,
    val plannedEndDate: String,
    val status: String,
    val entityVersion: Long
)

@Serializable
data class CampaignSummaryPageDto(
    val items: List<CampaignSummaryDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

@Serializable
data class CampaignScopeItemDto(
    val id: String,
    val campaignId: String,
    val storageSpaceId: String,
    val snapshotHierarchyRef: String,
    val createdAt: String,
    val createdBy: String
)

@Serializable
data class CampaignScopeItemResponseDto(
    val items: List<CampaignScopeItemDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

@Serializable
data class CampaignTeamAssignmentDto(
    val id: String,
    val campaignId: String,
    val userId: String,
    val organizationMembershipId: String,
    val assignmentStatus: String,
    val assignedAt: String,
    val assignedBy: String,
    val removedAt: String? = null,
    val removedBy: String? = null,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String,
    val entityVersion: Long
)

@Serializable
data class CampaignTeamAssignmentPageDto(
    val items: List<CampaignTeamAssignmentDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)
