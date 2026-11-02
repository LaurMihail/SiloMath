package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.UserId

data class CampaignTeamMutationControl(
    val expectedCampaignEntityVersion: Long,
    val idempotencyKey: String
) {
    init {
        require(expectedCampaignEntityVersion >= 1L) {
            "Campaign Team mutation expectedCampaignEntityVersion must be >= 1"
        }
        require(idempotencyKey.isNotBlank() && idempotencyKey == idempotencyKey.trim()) {
            "Campaign Team mutation idempotencyKey must be non-blank and trimmed"
        }
    }
}

data class AddCampaignTeamMemberCommand(
    val organizationId: OrganizationId,
    val campaignId: CampaignId,
    val userId: UserId,
    val mutationControl: CampaignTeamMutationControl
)

data class RemoveCampaignTeamMemberCommand(
    val organizationId: OrganizationId,
    val campaignId: CampaignId,
    val userId: UserId,
    val mutationControl: CampaignTeamMutationControl
)

data class CampaignTeamMutationResult(
    val campaignId: CampaignId,
    val campaignEntityVersion: Long,
    val assignment: CampaignTeamAssignment?
) {
    init {
        require(campaignEntityVersion >= 1L) {
            "Campaign Team mutation result campaignEntityVersion must be >= 1"
        }
        require(assignment == null || assignment.campaignId == campaignId) {
            "Campaign Team mutation assignment must belong to the Campaign"
        }
    }
}
