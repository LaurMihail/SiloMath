package com.silomath.core.data.network.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class BearerTokenInterceptor @Inject constructor(
    private val sessionProvider: NetworkSessionProvider
) : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {
        val originalRequest = chain.request()

        if (
            originalRequest.header("Authorization") != null
        ) {
            return chain.proceed(originalRequest)
        }

        val accessToken =
            sessionProvider.currentAccessToken()

        if (accessToken.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest =
            originalRequest
                .newBuilder()
                .header(
                    "Authorization",
                    "Bearer $accessToken"
                )
                .build()

        return chain.proceed(authenticatedRequest)
    }
}
