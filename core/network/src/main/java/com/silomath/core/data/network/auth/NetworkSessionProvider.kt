package com.silomath.core.data.network.auth

interface NetworkSessionProvider {

    fun currentAccessToken(): String?

    suspend fun refreshAccessToken(
        failedAccessToken: String?
    ): String?
}
