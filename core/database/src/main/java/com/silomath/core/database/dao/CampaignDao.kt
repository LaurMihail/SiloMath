package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.silomath.core.database.entity.CampaignAssignmentEntity
import com.silomath.core.database.entity.CampaignEntity
import com.silomath.core.database.entity.CampaignScopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {

    // -------------------------------------------------------------------------
    // CANONICAL V6 READ PROJECTIONS
    // -------------------------------------------------------------------------

    @Query(
        """
        SELECT COUNT(*)
        FROM campaigns
        WHERE canonical_status = 'ACTIVE'
        """
    )
    fun observeActiveCampaignCount(): Flow<Int>

    @Query(
        """
        SELECT *
        FROM campaigns
        WHERE canonical_organization_id = :organizationId
        ORDER BY name COLLATE NOCASE, campaign_id
        """
    )
    fun getCampaignsByCanonicalOrganization(
        organizationId: String
    ): Flow<List<CampaignEntity>>

    @Query(
        """
        SELECT *
        FROM campaigns
        WHERE campaign_id = :campaignId
        LIMIT 1
        """
    )
    suspend fun getCampaignByCampaignId(
        campaignId: String
    ): CampaignEntity?

    @Query(
        """
        SELECT *
        FROM campaign_scope
        WHERE campaign_scope_id = :campaignScopeId
        LIMIT 1
        """
    )
    suspend fun getScopeByCampaignScopeId(
        campaignScopeId: String
    ): CampaignScopeEntity?

    @Query(
        """
        SELECT *
        FROM campaign_assignments
        WHERE assignment_id = :assignmentId
        LIMIT 1
        """
    )
    suspend fun getAssignmentByAssignmentId(
        assignmentId: String
    ): CampaignAssignmentEntity?

    // -------------------------------------------------------------------------
    // LEGACY MIGRATION READS
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM campaigns WHERE organization_id = :organizationId")
    fun getCampaignsByOrganization(
        organizationId: Long
    ): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE status = 'Active'")
    fun getActiveCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE id = :id LIMIT 1")
    suspend fun getCampaignById(
        id: Long
    ): CampaignEntity?

    @Query("SELECT * FROM campaigns WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getCampaignByRemoteId(
        remoteId: String
    ): CampaignEntity?

    @Query("SELECT * FROM campaign_scope WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getScopeByRemoteId(
        remoteId: String
    ): CampaignScopeEntity?

    @Query("SELECT * FROM campaign_assignments WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getAssignmentByRemoteId(
        remoteId: String
    ): CampaignAssignmentEntity?

    // -------------------------------------------------------------------------
    // WRITES
    // -------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCampaign(
        campaign: CampaignEntity
    ): Long

    @Update
    suspend fun updateCampaign(
        campaign: CampaignEntity
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScopeItems(
        items: List<CampaignScopeEntity>
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScope(
        item: CampaignScopeEntity
    ): Long

    @Update
    suspend fun updateScope(
        scope: CampaignScopeEntity
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAssignment(
        assignment: CampaignAssignmentEntity
    ): Long

    @Update
    suspend fun updateAssignment(
        assignment: CampaignAssignmentEntity
    )

    @Query("SELECT * FROM campaign_assignments WHERE campaign_id = :campaignId")
    fun getAssignmentsByCampaign(
        campaignId: Long
    ): Flow<List<CampaignAssignmentEntity>>
}
