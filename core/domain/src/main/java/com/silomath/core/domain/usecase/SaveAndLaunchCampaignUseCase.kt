package com.silomath.core.domain.usecase

import com.silomath.core.domain.model.SaveAndLaunchCampaignInput
import com.silomath.core.domain.model.SaveAndLaunchCampaignResult
import com.silomath.core.domain.repository.CampaignRepository
import javax.inject.Inject

class SaveAndLaunchCampaignUseCase @Inject constructor(
    private val campaignRepository: CampaignRepository
) {
    suspend operator fun invoke(
        input: SaveAndLaunchCampaignInput
    ): Result<SaveAndLaunchCampaignResult> {
        return campaignRepository.saveAndLaunchCampaign(input)
    }
}
