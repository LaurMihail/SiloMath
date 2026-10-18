package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.CampaignScopeId
import com.silomath.core.model.CampaignSnapshotId
import com.silomath.core.model.CampaignTeamAssignmentId
import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.OrganizationMembershipId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.UserId
import java.time.Instant
import java.time.LocalDate

private val campaignCodePattern = Regex("^INV-\\d{4}-\\d{4}$")

enum class CampaignStatus {
    ACTIVE,
    COMPLETED
}

enum class CampaignTeamAssignmentStatus {
    ACTIVE,
    REMOVED
}

data class InventoryCampaign(
    val campaignId: CampaignId,
    val organizationId: OrganizationId,
    val campaignCode: String,
    val name: String,
    val inventoryType: CampaignInventoryType,
    val startDate: LocalDate,
    val plannedEndDate: LocalDate,
    val status: CampaignStatus,
    val launchedAt: Instant,
    val launchedBy: UserId,
    val completedAt: Instant?,
    val completedBy: UserId?,
    val campaignSnapshotId: CampaignSnapshotId,
    val createdAt: Instant,
    val createdBy: UserId,
    val updatedAt: Instant,
    val updatedBy: UserId,
    val entityVersion: Long
) {
    init {
        require(campaignCodePattern.matches(campaignCode)) {
            "InventoryCampaign campaignCode must match INV-YYYY-NNNN"
        }
        require(name.isNotBlank()) {
            "InventoryCampaign name must not be blank"
        }
        require(name == name.trim()) {
            "InventoryCampaign name must be trimmed"
        }
        require(!plannedEndDate.isBefore(startDate)) {
            "InventoryCampaign plannedEndDate must be on or after startDate"
        }
        require(entityVersion >= 1L) {
            "InventoryCampaign entityVersion must be >= 1"
        }
        require(!updatedAt.isBefore(createdAt)) {
            "InventoryCampaign updatedAt must not be before createdAt"
        }
        require(!launchedAt.isBefore(createdAt)) {
            "InventoryCampaign launchedAt must not be before createdAt"
        }

        when (status) {
            CampaignStatus.ACTIVE -> {
                require(completedAt == null && completedBy == null) {
                    "ACTIVE Campaign must not define completion metadata"
                }
            }

            CampaignStatus.COMPLETED -> {
                require(completedAt != null && completedBy != null) {
                    "COMPLETED Campaign requires completedAt and completedBy"
                }
                require(!completedAt.isBefore(launchedAt)) {
                    "InventoryCampaign completedAt must not be before launchedAt"
                }
            }
        }
    }
}

data class CampaignScope(
    val campaignScopeId: CampaignScopeId,
    val campaignId: CampaignId,
    val storageSpaceId: StorageSpaceId,
    val snapshotHierarchyRef: String,
    val createdAt: Instant,
    val createdBy: UserId
) {
    init {
        require(snapshotHierarchyRef.isNotBlank()) {
            "CampaignScope snapshotHierarchyRef must not be blank"
        }
        require(snapshotHierarchyRef == snapshotHierarchyRef.trim()) {
            "CampaignScope snapshotHierarchyRef must be trimmed"
        }
    }
}

data class CampaignTeamAssignment(
    val assignmentId: CampaignTeamAssignmentId,
    val campaignId: CampaignId,
    val userId: UserId,
    val organizationMembershipId: OrganizationMembershipId,
    val assignmentStatus: CampaignTeamAssignmentStatus,
    val assignedAt: Instant,
    val assignedBy: UserId,
    val removedAt: Instant?,
    val removedBy: UserId?,
    val createdAt: Instant,
    val createdBy: UserId,
    val updatedAt: Instant,
    val updatedBy: UserId,
    val entityVersion: Long
) {
    init {
        require(entityVersion >= 1L) {
            "CampaignTeamAssignment entityVersion must be >= 1"
        }
        require(!updatedAt.isBefore(createdAt)) {
            "CampaignTeamAssignment updatedAt must not be before createdAt"
        }
        require(!assignedAt.isBefore(createdAt)) {
            "CampaignTeamAssignment assignedAt must not be before createdAt"
        }

        when (assignmentStatus) {
            CampaignTeamAssignmentStatus.ACTIVE -> {
                require(removedAt == null && removedBy == null) {
                    "ACTIVE CampaignTeamAssignment must not define removal metadata"
                }
            }

            CampaignTeamAssignmentStatus.REMOVED -> {
                require(removedAt != null && removedBy != null) {
                    "REMOVED CampaignTeamAssignment requires removedAt and removedBy"
                }
                require(!removedAt.isBefore(assignedAt)) {
                    "CampaignTeamAssignment removedAt must not be before assignedAt"
                }
            }
        }
    }
}

data class CampaignConfigurationSnapshot(
    val campaignCode: String,
    val name: String,
    val inventoryType: CampaignInventoryType,
    val startDate: LocalDate,
    val plannedEndDate: LocalDate
) {
    init {
        require(campaignCodePattern.matches(campaignCode)) {
            "CampaignConfigurationSnapshot campaignCode must match INV-YYYY-NNNN"
        }
        require(name.isNotBlank() && name == name.trim()) {
            "CampaignConfigurationSnapshot name must be non-blank and trimmed"
        }
        require(!plannedEndDate.isBefore(startDate)) {
            "CampaignConfigurationSnapshot plannedEndDate must be on or after startDate"
        }
    }
}

data class CampaignScopeSnapshotEntry(
    val storageSpaceId: StorageSpaceId,
    val snapshotHierarchyRef: String
) {
    init {
        require(snapshotHierarchyRef.isNotBlank()) {
            "CampaignScopeSnapshotEntry snapshotHierarchyRef must not be blank"
        }
        require(snapshotHierarchyRef == snapshotHierarchyRef.trim()) {
            "CampaignScopeSnapshotEntry snapshotHierarchyRef must be trimmed"
        }
    }
}

data class CampaignGeometrySnapshotEntry(
    val storageSpaceId: StorageSpaceId,
    val geometryProfileVersionId: GeometryProfileVersionId
)

data class CampaignCommodityCompatibilitySnapshotEntry(
    val commodityId: CommodityId,
    val commodityVersionId: CommodityVersionId
)

data class CampaignTeamSnapshotEntry(
    val userId: UserId,
    val organizationMembershipId: OrganizationMembershipId
)

data class CampaignSnapshot(
    val campaignSnapshotId: CampaignSnapshotId,
    val campaignId: CampaignId,
    val organizationId: OrganizationId,
    val campaignConfiguration: CampaignConfigurationSnapshot,
    val scopeSnapshot: List<CampaignScopeSnapshotEntry>,
    val geometrySnapshot: List<CampaignGeometrySnapshotEntry>,
    val commodityCompatibilitySnapshot:
        List<CampaignCommodityCompatibilitySnapshotEntry>,
    val teamSnapshot: List<CampaignTeamSnapshotEntry>,
    val calculationSchemaVersions: Set<String>,
    val createdAt: Instant,
    val createdBy: UserId,
    val snapshotSchemaVersion: String
) {
    init {
        require(scopeSnapshot.isNotEmpty()) {
            "CampaignSnapshot requires non-empty scopeSnapshot"
        }
        require(teamSnapshot.isNotEmpty()) {
            "CampaignSnapshot requires at least one launch-time team member"
        }
        require(calculationSchemaVersions.isNotEmpty()) {
            "CampaignSnapshot requires calculationSchemaVersions"
        }
        require(calculationSchemaVersions.all { it.isNotBlank() && it == it.trim() }) {
            "CampaignSnapshot calculation schema versions must be non-blank and trimmed"
        }
        require(snapshotSchemaVersion.isNotBlank()) {
            "CampaignSnapshot snapshotSchemaVersion must not be blank"
        }
        require(snapshotSchemaVersion == snapshotSchemaVersion.trim()) {
            "CampaignSnapshot snapshotSchemaVersion must be trimmed"
        }

        val scopeIds = scopeSnapshot.map { it.storageSpaceId }
        require(scopeIds.size == scopeIds.toSet().size) {
            "CampaignSnapshot scopeSnapshot must not duplicate StorageSpace identity"
        }

        val geometrySpaceIds = geometrySnapshot.map { it.storageSpaceId }
        require(geometrySpaceIds.size == geometrySpaceIds.toSet().size) {
            "CampaignSnapshot geometrySnapshot must not duplicate StorageSpace identity"
        }
        require(geometrySpaceIds.all { it in scopeIds }) {
            "CampaignSnapshot geometrySnapshot must reference scoped StorageSpaces"
        }

        val teamUsers = teamSnapshot.map { it.userId }
        require(teamUsers.size == teamUsers.toSet().size) {
            "CampaignSnapshot teamSnapshot must not duplicate User identity"
        }

        val commodityPairs = commodityCompatibilitySnapshot.map {
            it.commodityId to it.commodityVersionId
        }
        require(commodityPairs.size == commodityPairs.toSet().size) {
            "CampaignSnapshot commodity compatibility entries must be unique"
        }
    }
}
