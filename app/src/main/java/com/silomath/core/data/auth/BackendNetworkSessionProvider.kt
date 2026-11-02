package com.silomath.core.data.auth

import com.silomath.core.data.network.api.CurrentUserApi
import com.silomath.core.data.network.auth.NetworkSessionProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendNetworkSessionProvider @Inject constructor(
    private val sessionStore: SessionStore,
    private val secureCredentialStore: SecureCredentialStore,
    private val supabaseAuth: SupabaseAuthGateway,
    private val currentUserApi: CurrentUserApi
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
            val authSession =
                supabaseAuth.refreshSession(refreshToken)
            val currentUser =
                currentUserApi.getCurrentUser(
                    "Bearer " + authSession.accessToken
                )

            check(
                currentUser.user.userId ==
                    authSession.userId
            ) {
                "Authenticated subject does not match canonical user"
            }

            val authorizedOrganizations =
                currentUser.toAuthorizedOrganizations()
            val currentState =
                sessionStore.sessionState.first()
            val activeOrganizationId =
                currentState.activeOrganizationId
                    ?.takeIf { activeId ->
                        authorizedOrganizations.any {
                            it.organizationId == activeId
                        }
                    }

            sessionStore.updateSession(
                currentState.copy(
                    userId = authSession.userId,
                    token = authSession.accessToken,
                    refreshToken =
                        authSession.refreshToken,
                    activeOrganizationId =
                        activeOrganizationId,
                    authorizedOrganizations =
                        authorizedOrganizations
                )
            )
            authSession.accessToken
                .takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
