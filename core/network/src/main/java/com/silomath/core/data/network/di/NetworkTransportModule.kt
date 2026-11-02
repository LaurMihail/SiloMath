package com.silomath.core.data.network.di

import com.silomath.core.data.network.auth.BearerTokenInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkTransportModule {

    @Provides
    @Singleton
    fun provideNetworkJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        bearerTokenInterceptor: BearerTokenInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(bearerTokenInterceptor)
            .build()
    }
}
