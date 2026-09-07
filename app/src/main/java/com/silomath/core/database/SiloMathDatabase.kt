package com.silomath.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.silomath.core.database.dao.*
import com.silomath.core.database.entity.*

@Database(
    entities = [
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
        SyncOutboxEntity::class,
        AuditTrailEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SiloMathDatabase : RoomDatabase() {
    abstract fun organizationDao(): OrganizationDao
    abstract fun locationDao(): LocationDao
    abstract fun storageDao(): StorageDao
    abstract fun geometryDao(): GeometryDao
    abstract fun commodityDao(): CommodityDao
    abstract fun campaignDao(): CampaignDao
    abstract fun stocktakingDao(): StocktakingDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun syncDao(): SyncDao
    abstract fun auditDao(): AuditDao
}
