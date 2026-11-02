package com.silomath.core.database

import com.silomath.core.database.entity.canonical.*
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.silomath.core.database.dao.*
import com.silomath.core.database.entity.*

@Database(
    entities = [
        V6CalculationResultsRow::class,
        V6CampaignScopesRow::class,
        V6CampaignSnapshotCommodityItemsRow::class,
        V6CampaignSnapshotScopeItemsRow::class,
        V6CampaignSnapshotTeamItemsRow::class,
        V6CampaignSnapshotsRow::class,
        V6CampaignTeamAssignmentsRow::class,
        V6ClientsRow::class,
        V6CommoditiesRow::class,
        V6CommodityVersionsRow::class,
        V6DeviceInstallationsRow::class,
        V6EvidenceRow::class,
        V6GeometryFamiliesRow::class,
        V6GeometryProfileVersionsRow::class,
        V6GeometryProfilesRow::class,
        V6InventoryCampaignsRow::class,
        V6IssueResolutionsRow::class,
        V6IssueStatusEventsRow::class,
        V6IssuesRow::class,
        V6LocationsRow::class,
        V6MeasurementsRow::class,
        V6OrganizationMembershipsRow::class,
        V6OrganizationsRow::class,
        V6StocktakingRecordsRow::class,
        V6StocktakingRevisionsRow::class,
        V6StorageSpacesRow::class,
        V6StorageUnitsRow::class,
        V6UsersRow::class,

        // V6 additive canonical Room baseline (schema 22).
        CommodityVersionRoomEntity::class,
        CampaignSnapshotRoomEntity::class,
        CampaignSnapshotScopeItemRoomEntity::class,
        CampaignSnapshotTeamItemRoomEntity::class,
        CampaignSnapshotCommodityItemRoomEntity::class,
        OfflineWorkingSetManifestRoomEntity::class,
        OfflinePackageDownloadEntity::class,
        StocktakingRevisionRoomEntity::class,
        IssueResolutionRoomEntity::class,
        IssueStatusEventRoomEntity::class,
        EvidenceRoomEntity::class,
        FinalInventorySnapshotRoomEntity::class,
        FinalInventorySnapshotEntryRoomEntity::class,
        SyncOperationDependencyRoomEntity::class,
        SyncConflictRoomEntity::class,
        SyncOperationEntity::class,
        AuditEventRoomEntity::class,
        NotificationRoomEntity::class,
        UserPreferenceRoomEntity::class,
        OrganizationConfigurationRoomEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(SiloMathConverters::class)
abstract class SiloMathDatabase : RoomDatabase() {

    abstract fun canonicalStorageSpaceReadDao(): CanonicalStorageSpaceReadDao
    abstract fun storageUnitCanonicalDao(): StorageUnitCanonicalDao
    abstract fun locationCanonicalDao(): LocationCanonicalDao
    abstract fun canonicalOfflineDao(): CanonicalOfflineDao
    abstract fun syncOperationDao(): SyncOperationDao

    // Canonical V6 DAO surface introduced with schema 22.
    abstract fun commodityVersionDao(): CommodityVersionDao
    abstract fun campaignSnapshotDao(): CampaignSnapshotDao
    abstract fun offlinePackageDownloadDao(): OfflinePackageDownloadDao
    abstract fun offlineWorkingSetManifestDao(): OfflineWorkingSetManifestDao
    abstract fun stocktakingRevisionDao(): StocktakingRevisionDao
    abstract fun issueHistoryDao(): IssueHistoryDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun finalInventorySnapshotDao(): FinalInventorySnapshotDao
    abstract fun syncDependencyConflictDao(): SyncDependencyConflictDao
    abstract fun canonicalAuditEventDao(): CanonicalAuditEventDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun organizationConfigurationDao(): OrganizationConfigurationDao
}
