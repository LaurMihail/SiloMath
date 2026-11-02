package com.silomath.core.data.network.client

import com.silomath.core.data.network.config.ApiEnvironmentConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitFactory @Inject constructor(
    private val json: Json,
    private val okHttpClient: OkHttpClient
) {

    fun create(
        environmentConfig: ApiEnvironmentConfig
    ): Retrofit {
        val contentType =
            "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(environmentConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory(contentType)
            )
            .build()
    }
}
