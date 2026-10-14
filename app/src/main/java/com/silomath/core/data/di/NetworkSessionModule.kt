package com.silomath.core.data.di

import com.silomath.core.data.auth.SupabaseNetworkSessionProvider
import com.silomath.core.data.network.auth.NetworkSessionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkSessionModule {

    @Binds
    @Singleton
    abstract fun bindNetworkSessionProvider(
        implementation: SupabaseNetworkSessionProvider
    ): NetworkSessionProvider
}
