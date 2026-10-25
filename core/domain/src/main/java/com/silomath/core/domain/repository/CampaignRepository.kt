package com.silomath.core.domain.repository

import com.silomath.core.domain.model.AddCampaignTeamMemberCommand
import com.silomath.core.domain.model.CampaignSnapshot
import com.silomath.core.domain.model.CampaignTeamMutationResult
import com.silomath.core.domain.model.RemoveCampaignTeamMemberCommand
import com.silomath.core.domain.model.SaveAndLaunchCampaignInput
import com.silomath.core.domain.model.SaveAndLaunchCampaignResult
import com.silomath.core.model.CampaignId
import com.silomath.core.model.OrganizationId
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {

    /** Room-backed local-first projection. */
    fun observeActiveCampaignCount(): Flow<Int>

    /** Refreshes authorized Campaign data for the Organization. */
    suspend fun refreshCampaigns(
        organizationId: OrganizationId
    ): Result<Unit>

    /** Atomic online Save & Launch. No Draft state exists in V6. */
    suspend fun saveAndLaunchCampaign(
        input: SaveAndLaunchCampaignInput
    ): Result<SaveAndLaunchCampaignResult>

    /** Reads the immutable launch-time CampaignSnapshot. */
    suspend fun getCampaignSnapshot(
        organizationId: OrganizationId,
        campaignId: CampaignId
    ): Result<CampaignSnapshot>

    /** Admin-only server-authorized mutation while Campaign is ACTIVE. */
    suspend fun addTeamMember(
        command: AddCampaignTeamMemberCommand
    ): Result<CampaignTeamMutationResult>

    /**
     * Admin-only server-authorized mutation while Campaign is ACTIVE.
     * Removal preserves durable CampaignTeamAssignment history.
     */
    suspend fun removeTeamMember(
        command: RemoveCampaignTeamMemberCommand
    ): Result<CampaignTeamMutationResult>
}
