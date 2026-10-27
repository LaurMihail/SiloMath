package com.silomath.app.navigation

import com.silomath.app.BuildConfig
import com.silomath.feature.masterdata.location.map.MapProviderConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object MapPresentationModule {
    @Provides @Singleton fun provider()=MapProviderConfig(BuildConfig.MAP_RASTER_TILE_TEMPLATE,
        BuildConfig.MAP_ATTRIBUTION_TEXT,BuildConfig.MAP_ATTRIBUTION_URL)
}
