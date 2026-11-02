package com.silomath.core.data.di

import com.silomath.core.data.auth.*
import com.silomath.core.data.repository.CanonicalStorageSpaceReader
import com.silomath.core.data.repository.OfflineCampaignRepository
import com.silomath.core.data.repository.OfflineLocationRepository
import com.silomath.core.data.repository.OfflineStocktakingRepository
import com.silomath.core.data.repository.OfflineStorageSpaceRepository
import com.silomath.core.data.repository.OfflineStorageUnitRepository
import com.silomath.core.domain.repository.CampaignRepository
import com.silomath.core.domain.repository.LocationRepository
import com.silomath.core.domain.repository.StocktakingRepository
import com.silomath.core.domain.repository.StorageSpaceReader
import com.silomath.core.domain.repository.StorageSpaceRepository
import com.silomath.core.domain.repository.StorageUnitRepository
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
    abstract fun bindSessionStore(
        impl: DataStoreSessionStore
    ): SessionStore

    @Binds
    @Singleton
    abstract fun bindSupabaseAuthGateway(
        impl: DefaultSupabaseAuthGateway
    ): SupabaseAuthGateway

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: OfflineLocationRepository
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindCampaignRepository(
        impl: OfflineCampaignRepository
    ): CampaignRepository

    @Binds
    @Singleton
    abstract fun bindStocktakingRepository(
        impl: OfflineStocktakingRepository
    ): StocktakingRepository

    @Binds
    @Singleton
    abstract fun bindStorageUnitRepository(
        impl: OfflineStorageUnitRepository
    ): StorageUnitRepository

    @Binds
    @Singleton
    abstract fun bindStorageSpaceReader(
        impl: CanonicalStorageSpaceReader
    ): StorageSpaceReader

    @Binds
    @Singleton
    abstract fun bindStorageSpaceRepository(
        impl: OfflineStorageSpaceRepository
    ): StorageSpaceRepository
}
