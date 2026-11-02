package com.silomath.core.data.network.client

import com.silomath.core.data.network.api.CurrentUserApi
import com.silomath.core.data.network.config.ApiEnvironmentConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Creates the authenticated-user bootstrap client without the regular bearer
 * interceptor. The freshly issued Supabase access token is supplied explicitly,
 * so membership loading cannot depend on an already-persisted session.
 */
@Singleton
class CurrentUserApiFactory @Inject constructor(
    private val json: Json
) {
    private val bootstrapClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    fun create(
        environmentConfig: ApiEnvironmentConfig
    ): CurrentUserApi {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(environmentConfig.baseUrl)
            .client(bootstrapClient)
            .addConverterFactory(
                json.asConverterFactory(contentType)
            )
            .build()
            .create(CurrentUserApi::class.java)
    }
}
