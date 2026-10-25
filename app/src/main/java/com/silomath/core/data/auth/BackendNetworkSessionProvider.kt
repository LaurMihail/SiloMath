package com.silomath.core.data.auth

import com.silomath.core.data.network.api.AuthApi
import com.silomath.core.data.network.auth.NetworkSessionProvider
import com.silomath.core.data.network.dto.RefreshSessionRequestDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendNetworkSessionProvider @Inject constructor(
    private val sessionStore: SessionStore,
    private val secureCredentialStore: SecureCredentialStore,
    private val authApi: AuthApi
) : NetworkSessionProvider {

    private val refreshMutex = Mutex()

    override fun currentAccessToken(): String? =
        secureCredentialStore
            .getAccessToken()
            ?.takeIf { it.isNotBlank() }

    override suspend fun refreshAccessToken(
        failedAccessToken: String?
    ): String? = refreshMutex.withLock {
        val currentAccessToken = currentAccessToken()
        if (
            !currentAccessToken.isNullOrBlank() &&
            !failedAccessToken.isNullOrBlank() &&
            currentAccessToken != failedAccessToken
        ) {
            return@withLock currentAccessToken
        }

        val refreshToken =
            secureCredentialStore
                .getRefreshToken()
                ?.takeIf { it.isNotBlank() }
                ?: return@withLock null

        try {
            val response = authApi.refreshSession(
                RefreshSessionRequestDto(refreshToken)
            )
            val currentState = sessionStore.sessionState.first()
            sessionStore.updateSession(
                currentState.copy(
                    userId = response.user.userId,
                    token = response.accessToken,
                    refreshToken = response.refreshToken
                )
            )
            response.accessToken.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
