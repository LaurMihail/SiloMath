package com.silomath.core.data.di

import com.silomath.core.data.auth.*
import com.silomath.core.data.repository.*
import com.silomath.core.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSessionStore(impl: DataStoreSessionStore): SessionStore

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: ProdAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOrganizationRepository(impl: OfflineOrganizationRepository): OrganizationRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: OfflineLocationRepository): LocationRepository

    @Binds
    @Singleton
    abstract fun bindStorageUnitRepository(impl: OfflineStorageUnitRepository): StorageUnitRepository

    @Binds
    @Singleton
    abstract fun bindStorageSpaceRepository(impl: OfflineStorageSpaceRepository): StorageSpaceRepository

    @Binds
    @Singleton
    abstract fun bindClientRepository(impl: OfflineClientRepository): ClientRepository

    @Binds
    @Singleton
    abstract fun bindGeometryRepository(impl: OfflineGeometryRepository): GeometryRepository

    @Binds
    @Singleton
    abstract fun bindCommodityRepository(impl: OfflineCommodityRepository): CommodityRepository

    @Binds
    @Singleton
    abstract fun bindCampaignRepository(impl: OfflineCampaignRepository): CampaignRepository

    @Binds
    @Singleton
    abstract fun bindStocktakingRepository(impl: OfflineStocktakingRepository): StocktakingRepository

    @Binds
    @Singleton
    abstract fun bindInventorySnapshotRepository(impl: OfflineInventorySnapshotRepository): InventorySnapshotRepository

    @Binds
    @Singleton
    abstract fun bindReportingRepository(impl: OfflineReportingRepository): ReportingRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: OfflineSyncRepository): SyncRepository
}
