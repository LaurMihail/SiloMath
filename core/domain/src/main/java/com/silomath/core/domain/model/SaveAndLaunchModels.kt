package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.CampaignSnapshotId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.StorageSpaceId
import com.silomath.core.model.UserId
import java.time.LocalDate

enum class CampaignInventoryType {
    ANNUAL,
    MONTHLY,
    CONTROL
}

/**
 * Initial Campaign Team selection. Organization role is not client-authored;
 * the server resolves the active OrganizationMembership and freezes the role
 * in CampaignSnapshot at launch.
 */
data class SaveAndLaunchTeamMember(
    val userId: UserId
)

data class SaveAndLaunchCampaignInput(
    val organizationId: OrganizationId,
    val name: String,
    val inventoryType: CampaignInventoryType,
    val startDate: LocalDate,
    val plannedEndDate: LocalDate,
    val scopeStorageSpaceIds: List<StorageSpaceId>,
    val team: List<SaveAndLaunchTeamMember>,
    val idempotencyKey: String
) {
    init {
        require(name.isNotBlank() && name == name.trim()) {
            "Campaign name must be non-blank and trimmed"
        }
        require(!plannedEndDate.isBefore(startDate)) {
            "Campaign plannedEndDate must be on or after startDate"
        }
        require(scopeStorageSpaceIds.isNotEmpty()) {
            "Campaign launch requires at least one StorageSpace in scope"
        }
        require(scopeStorageSpaceIds.distinct().size == scopeStorageSpaceIds.size) {
            "Campaign launch scope must not duplicate StorageSpace identity"
        }
        require(team.isNotEmpty()) {
            "Campaign launch requires at least one Team member"
        }
        require(team.map { it.userId }.distinct().size == team.size) {
            "Campaign launch Team must not duplicate User identity"
        }
        require(idempotencyKey.isNotBlank() && idempotencyKey == idempotencyKey.trim()) {
            "Campaign launch idempotencyKey must be non-blank and trimmed"
        }
    }
}

data class SaveAndLaunchCampaignResult(
    val campaignId: CampaignId,
    val campaignSnapshotId: CampaignSnapshotId
)
