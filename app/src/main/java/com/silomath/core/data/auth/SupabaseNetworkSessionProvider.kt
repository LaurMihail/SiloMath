package com.silomath.core.data.auth

import com.silomath.core.data.network.auth.NetworkSessionProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseNetworkSessionProvider @Inject constructor(
    private val sessionStore: SessionStore,
    private val secureCredentialStore: SecureCredentialStore,
    private val supabaseClient: SupabaseClient
) : NetworkSessionProvider {

    private val refreshMutex = Mutex()

    override fun currentAccessToken(): String? {
        return secureCredentialStore
            .getAccessToken()
            ?.takeIf { it.isNotBlank() }
    }

    override suspend fun refreshAccessToken(
        failedAccessToken: String?
    ): String? {
        return refreshMutex.withLock {

            val currentAccessToken =
                currentAccessToken()

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
                val newSession =
                    supabaseClient.auth
                        .refreshSession(refreshToken)

                val currentState =
                    sessionStore
                        .sessionState
                        .first()

                sessionStore.updateSession(
                    currentState.copy(
                        token =
                            newSession.accessToken,
                        refreshToken =
                            newSession.refreshToken
                    )
                )

                supabaseClient.auth.importSession(
                    session = newSession,
                    autoRefresh = false
                )

                newSession.accessToken
                    .takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
    }
}
