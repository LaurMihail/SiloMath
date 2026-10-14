package com.silomath.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.silomath.core.database.dao.*
import com.silomath.core.database.entity.*
import com.silomath.core.database.migration.CampaignCanonicalBackfill17To18

@Database(
    entities = [
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
        AuditTrailEntity::class
    ],
    version = 20,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 15,
            to = 16
        ),
        AutoMigration(
            from = 16,
            to = 17
        ),
        AutoMigration(
            from = 17,
            to = 18,
            spec = CampaignCanonicalBackfill17To18::class
        ),
        AutoMigration(
            from = 18,
            to = 19
        ),
        AutoMigration(
            from = 19,
            to = 20
        )
    ]
)
@TypeConverters(SiloMathConverters::class)
abstract class SiloMathDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun organizationDao(): OrganizationDao

    abstract fun organizationMemberDao(): OrganizationMemberDao

    abstract fun organizationMembershipDao():
        OrganizationMembershipDao

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
}