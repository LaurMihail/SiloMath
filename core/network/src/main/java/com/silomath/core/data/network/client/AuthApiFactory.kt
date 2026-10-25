package com.silomath.core.data.network.client

import com.silomath.core.data.network.api.AuthApi
import com.silomath.core.data.network.config.ApiEnvironmentConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates the bootstrap authentication client without the bearer-token
 * interceptor. Sign-in and refresh must not depend on an already-authenticated
 * transport; sign-out sends its Authorization header explicitly.
 */
@Singleton
class AuthApiFactory @Inject constructor(
    private val json: Json
) {
    private val bootstrapClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    fun create(
        environmentConfig: ApiEnvironmentConfig
    ): AuthApi {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(environmentConfig.baseUrl)
            .client(bootstrapClient)
            .addConverterFactory(
                json.asConverterFactory(contentType)
            )
            .build()
            .create(AuthApi::class.java)
    }
}
