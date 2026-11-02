package com.silomath.core.data.network.source

import com.silomath.core.model.campaign.CampaignAssignmentTransferRecord
import com.silomath.core.model.campaign.CampaignLaunchCommand
import com.silomath.core.model.campaign.CampaignLaunchTransportResult
import com.silomath.core.model.campaign.CampaignScopeTransferRecord
import com.silomath.core.model.campaign.CampaignTransferRecord

interface CampaignRemoteDataSource {

    suspend fun getCampaigns(
        organizationId: String
    ): Result<List<CampaignTransferRecord>>

    suspend fun getCampaignScope(
        organizationId: String,
        campaignIds: List<String>
    ): Result<List<CampaignScopeTransferRecord>>

    suspend fun getCampaignAssignments(
        organizationId: String,
        campaignIds: List<String>
    ): Result<List<CampaignAssignmentTransferRecord>>

    suspend fun saveAndLaunchCampaign(
        command: CampaignLaunchCommand
    ): Result<CampaignLaunchTransportResult>
}
