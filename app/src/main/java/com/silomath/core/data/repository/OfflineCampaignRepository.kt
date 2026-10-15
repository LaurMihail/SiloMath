package com.silomath.core.data.repository

import com.silomath.core.database.dao.CampaignDao
import com.silomath.core.domain.model.SaveAndLaunchCampaignInput
import com.silomath.core.domain.model.SaveAndLaunchCampaignResult
import com.silomath.core.domain.repository.CampaignRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineCampaignRepository @Inject constructor(
    private val campaignDao: CampaignDao
) : CampaignRepository {

    override fun observeActiveCampaignCount(): Flow<Int> =
        campaignDao.observeActiveCampaignCount()

    override suspend fun refreshCampaigns(
        organizationId: String
    ): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Campaign refresh implementation belongs to M3")
        )
    }

    override suspend fun saveAndLaunchCampaign(
        input: SaveAndLaunchCampaignInput
    ): Result<SaveAndLaunchCampaignResult> {
        return Result.failure(
            UnsupportedOperationException("Campaign Save & Launch implementation belongs to M3")
        )
    }
}
