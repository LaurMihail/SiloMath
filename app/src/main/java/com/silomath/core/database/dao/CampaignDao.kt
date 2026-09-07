package com.silomath.core.database.dao

import androidx.room.*
import com.silomath.core.database.entity.CampaignAssignmentEntity
import com.silomath.core.database.entity.CampaignEntity
import com.silomath.core.database.entity.CampaignScopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns WHERE organization_id = :organizationId")
    fun getCampaignsByOrganization(organizationId: Long): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE status = 'Active'")
    fun getActiveCampaigns(): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScopeItems(items: List<CampaignScopeEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScope(item: CampaignScopeEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAssignment(assignment: CampaignAssignmentEntity): Long

    @Query("SELECT * FROM campaign_assignments WHERE campaign_id = :campaignId")
    fun getAssignmentsByCampaign(campaignId: Long): Flow<List<CampaignAssignmentEntity>>
}
