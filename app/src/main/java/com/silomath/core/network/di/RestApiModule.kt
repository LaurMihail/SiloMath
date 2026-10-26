package com.silomath.core.network.di

import com.silomath.app.BuildConfig
import com.silomath.core.data.network.api.AuthApi
import com.silomath.core.data.network.api.CampaignApi
import com.silomath.core.data.network.api.LocationApi
import com.silomath.core.data.network.api.OrganizationApi
import com.silomath.core.data.network.api.OrganizationMembershipApi
import com.silomath.core.data.network.client.AuthApiFactory
import com.silomath.core.data.network.client.CampaignApiFactory
import com.silomath.core.data.network.client.RetrofitFactory
import com.silomath.core.data.network.config.ApiEnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RestApiModule {

    @Provides
    @Singleton
    fun provideApiEnvironmentConfig(): ApiEnvironmentConfig {
        return ApiEnvironmentConfig(
            baseUrl = BuildConfig.API_BASE_URL
        )
    }

    @Provides
    @Singleton
    fun provideAuthApi(
        authApiFactory: AuthApiFactory,
        environmentConfig: ApiEnvironmentConfig
    ): AuthApi {
        return authApiFactory.create(environmentConfig)
    }

    @Provides
    @Singleton
    fun provideCampaignApi(
        campaignApiFactory: CampaignApiFactory,
        environmentConfig: ApiEnvironmentConfig
    ): CampaignApi {
        return campaignApiFactory.create(
            environmentConfig
        )
    }

    @Provides
    @Singleton
    fun provideLocationApi(
        retrofitFactory: RetrofitFactory,
        environmentConfig: ApiEnvironmentConfig
    ): LocationApi {
        return retrofitFactory
            .create(environmentConfig)
            .create(LocationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOrganizationApi(
        retrofitFactory: RetrofitFactory,
        environmentConfig: ApiEnvironmentConfig
    ): OrganizationApi {
        return retrofitFactory
            .create(environmentConfig)
            .create(OrganizationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOrganizationMembershipApi(
        retrofitFactory: RetrofitFactory,
        environmentConfig: ApiEnvironmentConfig
    ): OrganizationMembershipApi {
        return retrofitFactory
            .create(environmentConfig)
            .create(OrganizationMembershipApi::class.java)
    }
}
