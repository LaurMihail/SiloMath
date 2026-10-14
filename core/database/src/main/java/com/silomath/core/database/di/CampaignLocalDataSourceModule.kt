package com.silomath.core.database.di

import com.silomath.core.database.source.CampaignLocalDataSource
import com.silomath.core.database.source.RoomCampaignLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CampaignLocalDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindCampaignLocalDataSource(
        implementation: RoomCampaignLocalDataSource
    ): CampaignLocalDataSource
}
