package com.silomath.core.domain.model

import java.time.Instant
import java.time.LocalDate

enum class CampaignStatus {
    ACTIVE,
    COMPLETED
}

enum class CampaignTeamAssignmentStatus {
    ACTIVE,
    REMOVED
}

data class InventoryCampaign(
    val campaignId: String,
    val organizationId: String,
    val campaignCode: String,
    val name: String,
    val inventoryType: CampaignInventoryType,
    val startDate: LocalDate,
    val plannedEndDate: LocalDate,
    val status: CampaignStatus,
    val launchedAt: Instant,
    val launchedBy: String,
    val completedAt: Instant?,
    val completedBy: String?,
    val campaignSnapshotId: String,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
)

data class CampaignScope(
    val campaignScopeId: String,
    val campaignId: String,
    val storageSpaceId: String,
    val snapshotHierarchyRef: String,
    val createdAt: Instant,
    val createdBy: String
)

data class CampaignTeamAssignment(
    val assignmentId: String,
    val campaignId: String,
    val userId: String,
    val organizationMembershipId: String,
    val assignmentStatus: CampaignTeamAssignmentStatus,
    val assignedAt: Instant,
    val assignedBy: String,
    val removedAt: Instant?,
    val removedBy: String?,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant,
    val updatedBy: String,
    val entityVersion: Long
)
