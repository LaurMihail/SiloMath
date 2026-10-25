package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.CampaignScopeId
import com.silomath.core.model.CampaignSnapshotId
import com.silomath.core.model.CampaignTeamAssignmentId
import com.silomath.core.model.CanonicalDecimal
import com.silomath.core.model.CommodityId
import com.silomath.core.model.CommodityVersionId
import com.silomath.core.model.GeometryProfileId
import com.silomath.core.model.GeometryProfileVersionId
import com.silomath.core.model.LocationId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.OrganizationMembershipId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.StorageUnitId
import com.silomath.core.model.UserId
import java.time.Instant
import java.time.LocalDate

private val campaignCodePattern = Regex("^INV-\\d{4}-\\d{4}$")
private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

private fun requireFrozenText(value: String, field: String) {
    require(value.isNotBlank() && value == value.trim()) {
        "$field must be non-blank and trimmed"
    }
}

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
        requireFrozenText(name, "InventoryCampaign name")
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
            CampaignStatus.ACTIVE -> require(completedAt == null && completedBy == null) {
                "ACTIVE Campaign must not define completion metadata"
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

    fun canTransitionTo(next: CampaignStatus): Boolean =
        status == CampaignStatus.ACTIVE && next == CampaignStatus.COMPLETED
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
        requireFrozenText(snapshotHierarchyRef, "CampaignScope snapshotHierarchyRef")
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
            CampaignTeamAssignmentStatus.ACTIVE -> require(removedAt == null && removedBy == null) {
                "ACTIVE CampaignTeamAssignment must not define removal metadata"
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

data class CampaignOrganizationSnapshot(
    val organizationId: OrganizationId,
    val name: String,
    val code: String,
    val address: String?,
    val city: String?,
    val country: String?
) {
    init {
        requireFrozenText(name, "CampaignOrganizationSnapshot name")
        requireFrozenText(code, "CampaignOrganizationSnapshot code")
        listOf(address, city, country).filterNotNull().forEach {
            require(it == it.trim()) { "CampaignOrganizationSnapshot optional text must be trimmed" }
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
        requireFrozenText(name, "CampaignConfigurationSnapshot name")
        require(!plannedEndDate.isBefore(startDate)) {
            "CampaignConfigurationSnapshot plannedEndDate must be on or after startDate"
        }
    }
}

data class CampaignScopeSnapshotEntry(
    val storageSpaceId: StorageSpaceId,
    val storageSpaceCode: String,
    val storageUnitId: StorageUnitId,
    val storageUnitCode: String,
    val storageUnitName: String,
    val locationId: LocationId,
    val locationCode: String,
    val locationName: String,
    val geometryProfileId: GeometryProfileId,
    val geometryProfileVersionId: GeometryProfileVersionId,
    val geometryProfileCode: String,
    val geometryProfileName: String,
    val geometryFamilyCode: String,
    val geometryFamilyName: String,
    val geometryFormulaVersion: String,
    val geometryDimensions: Map<String, CanonicalDecimal>,
    val measurementReferenceHeightM: CanonicalDecimal?,
    val maximumFillHeightM: CanonicalDecimal?,
    val maxCapacityM3: CanonicalDecimal?
) {
    init {
        listOf(
            storageSpaceCode,
            storageUnitCode,
            storageUnitName,
            locationCode,
            locationName,
            geometryProfileCode,
            geometryProfileName,
            geometryFamilyCode,
            geometryFamilyName,
            geometryFormulaVersion
        ).forEach { requireFrozenText(it, "CampaignScopeSnapshotEntry frozen display value") }
        require(geometryDimensions.keys.all { it.isNotBlank() && it == it.trim() }) {
            "CampaignScopeSnapshotEntry geometry dimension keys must be non-blank and trimmed"
        }
    }
}

data class CampaignCommodityCompatibilitySnapshotEntry(
    val commodityId: CommodityId,
    val commodityVersionId: CommodityVersionId
)

data class CampaignTeamSnapshotEntry(
    val userId: UserId,
    val organizationMembershipId: OrganizationMembershipId,
    val userFullName: String,
    val userEmail: String,
    val organizationRole: OrganizationRole
) {
    init {
        requireFrozenText(userFullName, "CampaignTeamSnapshotEntry userFullName")
        require(userEmail == userEmail.trim() && emailPattern.matches(userEmail)) {
            "CampaignTeamSnapshotEntry userEmail must be a trimmed email address"
        }
    }
}

data class CampaignSnapshot(
    val campaignSnapshotId: CampaignSnapshotId,
    val campaignId: CampaignId,
    val organizationSnapshot: CampaignOrganizationSnapshot,
    val campaignConfiguration: CampaignConfigurationSnapshot,
    val scopeSnapshot: List<CampaignScopeSnapshotEntry>,
    val commodityCompatibilitySnapshot: List<CampaignCommodityCompatibilitySnapshotEntry>,
    val teamSnapshot: List<CampaignTeamSnapshotEntry>,
    val calculationSchemaVersions: Set<String>,
    val calculationEngineVersion: String,
    val createdAt: Instant,
    val createdBy: UserId,
    val snapshotSchemaVersion: String
) {
    val organizationId: OrganizationId
        get() = organizationSnapshot.organizationId

    init {
        require(scopeSnapshot.isNotEmpty()) {
            "CampaignSnapshot requires non-empty scopeSnapshot"
        }
        require(teamSnapshot.isNotEmpty()) {
            "CampaignSnapshot requires at least one launch-time team member"
        }
        require(commodityCompatibilitySnapshot.isNotEmpty()) {
            "CampaignSnapshot requires explicit Commodity compatibility entries"
        }
        require(calculationSchemaVersions.isNotEmpty()) {
            "CampaignSnapshot requires calculationSchemaVersions"
        }
        require(calculationSchemaVersions.all { it.isNotBlank() && it == it.trim() }) {
            "CampaignSnapshot calculation schema versions must be non-blank and trimmed"
        }
        requireFrozenText(calculationEngineVersion, "CampaignSnapshot calculationEngineVersion")
        requireFrozenText(snapshotSchemaVersion, "CampaignSnapshot snapshotSchemaVersion")

        val scopeIds = scopeSnapshot.map { it.storageSpaceId }
        require(scopeIds.size == scopeIds.toSet().size) {
            "CampaignSnapshot scopeSnapshot must not duplicate StorageSpace identity"
        }

        val teamUsers = teamSnapshot.map { it.userId }
        require(teamUsers.size == teamUsers.toSet().size) {
            "CampaignSnapshot teamSnapshot must not duplicate User identity"
        }

        val teamMemberships = teamSnapshot.map { it.organizationMembershipId }
        require(teamMemberships.size == teamMemberships.toSet().size) {
            "CampaignSnapshot teamSnapshot must not duplicate OrganizationMembership identity"
        }

        val commodityPairs = commodityCompatibilitySnapshot.map {
            it.commodityId to it.commodityVersionId
        }
        require(commodityPairs.size == commodityPairs.toSet().size) {
            "CampaignSnapshot commodity compatibility entries must be unique"
        }
        require(commodityCompatibilitySnapshot.map { it.commodityId }.distinct().size ==
            commodityCompatibilitySnapshot.size) {
            "CampaignSnapshot must freeze at most one compatible CommodityVersion per Commodity"
        }
    }
}
