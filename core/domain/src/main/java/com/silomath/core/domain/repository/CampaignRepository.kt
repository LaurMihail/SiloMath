package com.silomath.core.domain.repository

import com.silomath.core.domain.model.SaveAndLaunchCampaignInput
import com.silomath.core.domain.model.SaveAndLaunchCampaignResult
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {

    /**
     * Room-backed local-first projection exposed as a domain-safe scalar.
     */
    fun observeActiveCampaignCount(): Flow<Int>

    /**
     * Refreshes Campaign data for the canonical Organization UUID.
     */
    suspend fun refreshCampaigns(
        organizationId: String
    ): Result<Unit>

    /**
     * Atomic online Save & Launch.
     */
    suspend fun saveAndLaunchCampaign(
        input: SaveAndLaunchCampaignInput
    ): Result<SaveAndLaunchCampaignResult>
}
