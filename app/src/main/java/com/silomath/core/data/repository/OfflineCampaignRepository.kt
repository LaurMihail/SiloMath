package com.silomath.core.data.repository

import com.silomath.core.domain.model.AddCampaignTeamMemberCommand
import com.silomath.core.domain.model.CampaignSnapshot
import com.silomath.core.domain.model.CampaignTeamMutationResult
import com.silomath.core.domain.model.RemoveCampaignTeamMemberCommand
import com.silomath.core.domain.model.SaveAndLaunchCampaignInput
import com.silomath.core.domain.model.SaveAndLaunchCampaignResult
import com.silomath.core.domain.repository.CampaignRepository
import com.silomath.core.model.CampaignId
import com.silomath.core.model.OrganizationId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class OfflineCampaignRepository @Inject constructor() : CampaignRepository {

    override fun observeActiveCampaignCount(): Flow<Int> = flowOf(0)

    override suspend fun refreshCampaigns(
        organizationId: OrganizationId
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException("Campaign refresh implementation belongs to M3")
    )

    override suspend fun saveAndLaunchCampaign(
        input: SaveAndLaunchCampaignInput
    ): Result<SaveAndLaunchCampaignResult> = Result.failure(
        UnsupportedOperationException("Campaign Save & Launch implementation belongs to M4")
    )

    override suspend fun getCampaignSnapshot(
        organizationId: OrganizationId,
        campaignId: CampaignId
    ): Result<CampaignSnapshot> = Result.failure(
        UnsupportedOperationException("Campaign snapshot read implementation belongs to M4")
    )

    override suspend fun addTeamMember(
        command: AddCampaignTeamMemberCommand
    ): Result<CampaignTeamMutationResult> = Result.failure(
        UnsupportedOperationException("Campaign Team mutation implementation belongs to M4")
    )

    override suspend fun removeTeamMember(
        command: RemoveCampaignTeamMemberCommand
    ): Result<CampaignTeamMutationResult> = Result.failure(
        UnsupportedOperationException("Campaign Team mutation implementation belongs to M4")
    )
}
