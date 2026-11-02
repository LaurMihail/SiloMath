package com.silomath.core.data.network.di

import com.silomath.core.data.network.source.CampaignRemoteDataSource
import com.silomath.core.data.network.source.RestCampaignRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CampaignNetworkModule {

    @Binds
    @Singleton
    abstract fun bindCampaignRemoteDataSource(
        implementation: RestCampaignRemoteDataSource
    ): CampaignRemoteDataSource
}
