package com.silomath.core.database

import com.silomath.core.database.entity.canonical.*
import com.silomath.core.database.migration.CanonicalOfflineExpand25To26
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.silomath.core.database.dao.*
import com.silomath.core.database.entity.*
import com.silomath.core.database.migration.CampaignCanonicalBackfill17To18
import com.silomath.core.database.migration.MasterDataCanonicalBackfill20To21

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
        UserEntity::class,
        OrganizationEntity::class,
        LocationEntity::class,
        StorageUnitEntity::class,
        StorageSpaceEntity::class,
        GeometryFamilyEntity::class,
        GeometryProfileEntity::class,
        GeometryVersionEntity::class,
        CommodityEntity::class,
        ClientEntity::class,
        CampaignEntity::class,
        CampaignScopeEntity::class,
        CampaignAssignmentEntity::class,
        StocktakingRecordEntity::class,
        MeasurementEntity::class,
        CalculationResultEntity::class,
        IssueEntity::class,
        InventorySnapshotEntity::class,
        InventorySnapshotItemEntity::class,
        OrganizationMemberEntity::class,
        OrganizationMembershipEntity::class,
        SyncOutboxEntity::class,
        SyncOperationEntity::class,
        DeviceInstallationEntity::class,
        AuditTrailEntity::class,

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
        AuditEventRoomEntity::class,
        NotificationRoomEntity::class,
        UserPreferenceRoomEntity::class,
        OrganizationConfigurationRoomEntity::class
    ],
    version = 27,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18, spec = CampaignCanonicalBackfill17To18::class),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21, spec = MasterDataCanonicalBackfill20To21::class),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26, spec = CanonicalOfflineExpand25To26::class),
        AutoMigration(from = 26, to = 27)
    ]
)
@TypeConverters(SiloMathConverters::class)
abstract class SiloMathDatabase : RoomDatabase() {

    abstract fun canonicalStorageSpaceReadDao(): CanonicalStorageSpaceReadDao
    abstract fun storageUnitCanonicalDao(): StorageUnitCanonicalDao
    abstract fun canonicalOfflineDao(): CanonicalOfflineDao
    abstract fun userDao(): UserDao
    abstract fun organizationDao(): OrganizationDao
    abstract fun organizationMemberDao(): OrganizationMemberDao
    abstract fun organizationMembershipDao(): OrganizationMembershipDao
    abstract fun locationDao(): LocationDao
    abstract fun storageDao(): StorageDao
    abstract fun geometryDao(): GeometryDao
    abstract fun commodityDao(): CommodityDao
    abstract fun clientDao(): ClientDao
    abstract fun campaignDao(): CampaignDao
    abstract fun stocktakingDao(): StocktakingDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun syncDao(): SyncDao
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun deviceInstallationDao(): DeviceInstallationDao
    abstract fun auditDao(): AuditDao

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
