package com.silomath.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.silomath.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommodityVersionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(version: CommodityVersionRoomEntity)

    @Query("SELECT * FROM commodity_versions WHERE commodity_version_id = :id LIMIT 1")
    suspend fun getById(id: String): CommodityVersionRoomEntity?

    @Query("SELECT * FROM commodity_versions WHERE commodity_id = :commodityId ORDER BY version_number DESC")
    suspend fun getForCommodity(commodityId: String): List<CommodityVersionRoomEntity>
}

@Dao
interface CampaignSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: CampaignSnapshotRoomEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScopeItems(items: List<CampaignSnapshotScopeItemRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTeamItems(items: List<CampaignSnapshotTeamItemRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCommodityItems(items: List<CampaignSnapshotCommodityItemRoomEntity>)

    @Transaction
    suspend fun insertSnapshotGraph(
        snapshot: CampaignSnapshotRoomEntity,
        scopeItems: List<CampaignSnapshotScopeItemRoomEntity>,
        teamItems: List<CampaignSnapshotTeamItemRoomEntity>,
        commodityItems: List<CampaignSnapshotCommodityItemRoomEntity>
    ) {
        insertSnapshot(snapshot)
        if (scopeItems.isNotEmpty()) insertScopeItems(scopeItems)
        if (teamItems.isNotEmpty()) insertTeamItems(teamItems)
        if (commodityItems.isNotEmpty()) insertCommodityItems(commodityItems)
    }

    @Query("SELECT * FROM campaign_snapshots WHERE campaign_id = :campaignId LIMIT 1")
    suspend fun getByCampaignId(campaignId: String): CampaignSnapshotRoomEntity?

    @Query("SELECT * FROM campaign_snapshot_scope_items WHERE campaign_snapshot_id = :snapshotId ORDER BY storage_space_id")
    suspend fun getScopeItems(snapshotId: String): List<CampaignSnapshotScopeItemRoomEntity>

    @Query("SELECT * FROM campaign_snapshot_team_items WHERE campaign_snapshot_id = :snapshotId ORDER BY user_id")
    suspend fun getTeamItems(snapshotId: String): List<CampaignSnapshotTeamItemRoomEntity>

    @Query("SELECT * FROM campaign_snapshot_commodity_items WHERE campaign_snapshot_id = :snapshotId ORDER BY commodity_id, commodity_version_id")
    suspend fun getCommodityItems(snapshotId: String): List<CampaignSnapshotCommodityItemRoomEntity>
}

@Dao
interface OfflineWorkingSetManifestDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(manifest: OfflineWorkingSetManifestRoomEntity)

    @Update
    suspend fun update(manifest: OfflineWorkingSetManifestRoomEntity)

    @Query("SELECT * FROM offline_working_set_manifests WHERE campaign_id = :campaignId AND device_installation_id = :deviceId AND is_current = 1 LIMIT 1")
    suspend fun getCurrent(campaignId: String, deviceId: String): OfflineWorkingSetManifestRoomEntity?
}

@Dao
interface StocktakingRevisionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(revision: StocktakingRevisionRoomEntity)

    @Update
    suspend fun updateWorkingRevision(revision: StocktakingRevisionRoomEntity)

    @Query("SELECT * FROM stocktaking_revisions WHERE stocktaking_revision_id = :id LIMIT 1")
    suspend fun getById(id: String): StocktakingRevisionRoomEntity?

    @Query("SELECT * FROM stocktaking_revisions WHERE stocktaking_record_id = :recordId ORDER BY revision_number DESC")
    fun observeForRecord(recordId: String): Flow<List<StocktakingRevisionRoomEntity>>
}

@Dao
interface IssueHistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertResolution(resolution: IssueResolutionRoomEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStatusEvent(event: IssueStatusEventRoomEntity)

    @Query("SELECT * FROM issue_resolutions WHERE issue_id = :issueId ORDER BY resolved_at ASC")
    suspend fun getResolutions(issueId: String): List<IssueResolutionRoomEntity>

    @Query("SELECT * FROM issue_status_events WHERE issue_id = :issueId ORDER BY changed_at ASC")
    suspend fun getStatusEvents(issueId: String): List<IssueStatusEventRoomEntity>
}

@Dao
interface EvidenceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(evidence: EvidenceRoomEntity)

    @Update
    suspend fun updateWorkingOrTransferState(evidence: EvidenceRoomEntity)

    @Query("SELECT * FROM evidence WHERE evidence_id = :id LIMIT 1")
    suspend fun getById(id: String): EvidenceRoomEntity?

    @Query("SELECT * FROM evidence WHERE stocktaking_record_id = :recordId AND removed_at IS NULL ORDER BY created_at ASC")
    fun observeForRecord(recordId: String): Flow<List<EvidenceRoomEntity>>

    @Query("SELECT * FROM evidence WHERE issue_id = :issueId AND removed_at IS NULL ORDER BY created_at ASC")
    fun observeForIssue(issueId: String): Flow<List<EvidenceRoomEntity>>
}

@Dao
interface FinalInventorySnapshotDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: FinalInventorySnapshotRoomEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(entries: List<FinalInventorySnapshotEntryRoomEntity>)

    @Transaction
    suspend fun insertSnapshotGraph(
        snapshot: FinalInventorySnapshotRoomEntity,
        entries: List<FinalInventorySnapshotEntryRoomEntity>
    ) {
        insertSnapshot(snapshot)
        if (entries.isNotEmpty()) insertEntries(entries)
    }

    @Query("SELECT * FROM final_inventory_snapshots WHERE campaign_id = :campaignId LIMIT 1")
    suspend fun getByCampaignId(campaignId: String): FinalInventorySnapshotRoomEntity?

    @Query("SELECT * FROM final_inventory_snapshot_entries WHERE final_snapshot_id = :snapshotId ORDER BY storage_space_id")
    suspend fun getEntries(snapshotId: String): List<FinalInventorySnapshotEntryRoomEntity>
}

@Dao
interface SyncDependencyConflictDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDependency(dependency: SyncOperationDependencyRoomEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDependencies(dependencies: List<SyncOperationDependencyRoomEntity>)

    @Query("SELECT * FROM sync_operation_dependencies WHERE dependent_operation_id = :operationId ORDER BY created_at ASC")
    suspend fun getDependencies(operationId: String): List<SyncOperationDependencyRoomEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConflict(conflict: SyncConflictRoomEntity)

    @Update
    suspend fun updateConflict(conflict: SyncConflictRoomEntity)

    @Query("SELECT * FROM sync_conflicts WHERE status = 'OPEN' ORDER BY detected_at ASC")
    fun observeOpenConflicts(): Flow<List<SyncConflictRoomEntity>>
}

@Dao
interface CanonicalAuditEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: AuditEventRoomEntity)

    @Query("SELECT * FROM audit_events WHERE organization_id = :organizationId ORDER BY occurred_at DESC")
    fun observeForOrganization(organizationId: String): Flow<List<AuditEventRoomEntity>>
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(notification: NotificationRoomEntity)

    @Query("SELECT * FROM notifications WHERE recipient_user_id = :userId ORDER BY created_at DESC")
    fun observeForUser(userId: String): Flow<List<NotificationRoomEntity>>

    @Query("UPDATE notifications SET read_at = :readAt WHERE notification_id = :notificationId AND read_at IS NULL")
    suspend fun markRead(notificationId: String, readAt: Long): Int
}

@Dao
interface UserPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(preference: UserPreferenceRoomEntity)

    @Query("SELECT * FROM user_preferences WHERE user_id = :userId LIMIT 1")
    fun observe(userId: String): Flow<UserPreferenceRoomEntity?>
}

@Dao
interface OrganizationConfigurationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(configuration: OrganizationConfigurationRoomEntity)

    @Query("SELECT * FROM organization_configurations WHERE organization_id = :organizationId ORDER BY configuration_key")
    fun observeForOrganization(organizationId: String): Flow<List<OrganizationConfigurationRoomEntity>>
}
