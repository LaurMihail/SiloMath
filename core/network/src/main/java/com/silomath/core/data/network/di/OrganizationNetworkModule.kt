package com.silomath.core.data.network.di

import com.silomath.core.data.network.source.OrganizationRemoteDataSource
import com.silomath.core.data.network.source.RestOrganizationRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrganizationNetworkModule {

    @Binds
    @Singleton
    abstract fun bindOrganizationRemoteDataSource(
        implementation: RestOrganizationRemoteDataSource
    ): OrganizationRemoteDataSource
}
