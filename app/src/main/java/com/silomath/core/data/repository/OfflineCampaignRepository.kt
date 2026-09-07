package com.silomath.core.data.repository

import com.silomath.core.database.dao.CampaignDao
import com.silomath.core.database.entity.CampaignAssignmentEntity
import com.silomath.core.database.entity.CampaignEntity
import com.silomath.core.database.entity.CampaignScopeEntity
import com.silomath.core.domain.repository.CampaignRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineCampaignRepository @Inject constructor(
    private val campaignDao: CampaignDao
) : CampaignRepository {
    override fun getCampaigns(organizationId: Long): Flow<List<CampaignEntity>> = 
        campaignDao.getCampaignsByOrganization(organizationId)

    override fun getActiveCampaigns(): Flow<List<CampaignEntity>> = campaignDao.getActiveCampaigns()

    override suspend fun startCampaign(campaign: CampaignEntity, scopeItems: List<CampaignScopeEntity>, assignments: List<CampaignAssignmentEntity>) {
        val campaignId = campaignDao.insertCampaign(campaign)
        campaignDao.insertScopeItems(scopeItems.map { it.copy(campaignId = campaignId) })
        assignments.forEach { campaignDao.insertAssignment(it.copy(campaignId = campaignId)) }
    }
}
