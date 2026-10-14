package com.silomath.core.model.campaign

data class CampaignTransferRecord(
    val id: String,
    val organizationId: String,
    val name: String,
    val status: String,
    val revision: Long,
    val campaignCode: String? = null,
    val inventoryType: String? = null,
    val startDate: String? = null,
    val plannedEndDate: String? = null,
    val launchedAt: String? = null,
    val launchedBy: String? = null,
    val completedAt: String? = null,
    val completedBy: String? = null,
    val campaignSnapshotId: String? = null,
    val createdAt: String? = null,
    val createdBy: String? = null,
    val updatedAt: String? = null,
    val updatedBy: String? = null
)

data class CampaignScopeTransferRecord(
    val id: String,
    val campaignId: String,
    val storageSpaceId: String,
    val geometryVersionId: String? = null,
    val snapshotHierarchyRef: String? = null,
    val createdAt: String? = null,
    val createdBy: String? = null
)

data class CampaignAssignmentTransferRecord(
    val id: String,
    val campaignId: String,
    val userId: String,
    val roleAtAssignment: String? = null,
    val organizationMembershipId: String? = null,
    val assignmentStatus: String? = null,
    val assignedAt: String? = null,
    val assignedBy: String? = null,
    val removedAt: String? = null,
    val removedBy: String? = null,
    val createdAt: String? = null,
    val createdBy: String? = null,
    val updatedAt: String? = null,
    val updatedBy: String? = null,
    val entityVersion: Long? = null
)

data class CampaignLaunchTeamMember(
    val userId: String,
    val roleAtAssignment: String
)

data class CampaignLaunchCommand(
    val organizationId: String,
    val name: String,
    val inventoryType: String,
    val startDate: String,
    val plannedEndDate: String,
    val scopeStorageSpaceIds: List<String>,
    val team: List<CampaignLaunchTeamMember>,
    val idempotencyKey: String
)

data class CampaignLaunchTransportResult(
    val campaignId: String,
    val launchSnapshotId: String,
    val campaign: CampaignTransferRecord? = null
)
