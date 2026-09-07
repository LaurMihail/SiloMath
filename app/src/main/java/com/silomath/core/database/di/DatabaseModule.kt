package com.silomath.core.database.di

import android.content.Context
import androidx.room.Room
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SiloMathDatabase {
        return Room.databaseBuilder(
            context,
            SiloMathDatabase::class.java,
            "silomath_db"
        ).build()
    }

    @Provides
    fun provideOrganizationDao(db: SiloMathDatabase): OrganizationDao = db.organizationDao()

    @Provides
    fun provideLocationDao(db: SiloMathDatabase): LocationDao = db.locationDao()

    @Provides
    fun provideStorageDao(db: SiloMathDatabase): StorageDao = db.storageDao()

    @Provides
    fun provideGeometryDao(db: SiloMathDatabase): GeometryDao = db.geometryDao()

    @Provides
    fun provideCommodityDao(db: SiloMathDatabase): CommodityDao = db.commodityDao()

    @Provides
    fun provideCampaignDao(db: SiloMathDatabase): CampaignDao = db.campaignDao()

    @Provides
    fun provideStocktakingDao(db: SiloMathDatabase): StocktakingDao = db.stocktakingDao()

    @Provides
    fun provideInventoryDao(db: SiloMathDatabase): InventoryDao = db.inventoryDao()

    @Provides
    fun provideSyncDao(db: SiloMathDatabase): SyncDao = db.syncDao()
}
