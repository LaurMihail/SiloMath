package com.silomath.core.data.network.di

import com.silomath.core.data.network.source.OrganizationMembershipRemoteDataSource
import com.silomath.core.data.network.source.RestOrganizationMembershipRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrganizationMembershipNetworkModule {

    @Binds
    @Singleton
    abstract fun bindOrganizationMembershipRemoteDataSource(
        implementation: RestOrganizationMembershipRemoteDataSource
    ): OrganizationMembershipRemoteDataSource
}