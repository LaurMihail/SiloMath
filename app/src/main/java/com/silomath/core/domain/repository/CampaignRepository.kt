package com.silomath.core.domain.repository

import com.silomath.core.database.entity.CampaignAssignmentEntity
import com.silomath.core.database.entity.CampaignEntity
import com.silomath.core.database.entity.CampaignScopeEntity
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {
    fun getCampaigns(organizationId: Long): Flow<List<CampaignEntity>>
    fun getActiveCampaigns(): Flow<List<CampaignEntity>>
    suspend fun startCampaign(campaign: CampaignEntity, scopeItems: List<CampaignScopeEntity>, assignments: List<CampaignAssignmentEntity>)
}
