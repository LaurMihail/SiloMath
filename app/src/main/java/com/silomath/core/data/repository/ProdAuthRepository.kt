package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.data.auth.SupabaseAuthGateway
import com.silomath.core.data.auth.SupabaseAuthSession
import com.silomath.core.data.auth.toAuthorizedOrganizations
import com.silomath.core.data.network.api.CurrentUserApi
import com.silomath.core.data.network.dto.CurrentUserResponseDto
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProdAuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val supabaseAuth: SupabaseAuthGateway,
    private val currentUserApi: CurrentUserApi
) : AuthRepository {

    override val sessionState: Flow<SessionState> =
        sessionStore.sessionState

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            val authSession =
                supabaseAuth.signIn(
                    email = email,
                    password = password
                )
            val currentUser =
                loadCurrentUser(authSession)

            sessionStore.updateSession(
                SessionState(
                    userId = authSession.userId,
                    token = authSession.accessToken,
                    refreshToken = authSession.refreshToken,
                    activeOrganizationId = null,
                    authorizedOrganizations =
                        currentUser.toAuthorizedOrganizations(),
                    isOfflineCapable = false
                )
            )
            Result.success(Unit)
        } catch (error: Exception) {
            runCatching {
                supabaseAuth.clearInMemorySession()
            }
            Result.failure(error)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        val currentState =
            sessionStore.sessionState.first()

        val remoteResult =
            runCatching {
                val accessToken =
                    currentState.token
                        ?.takeIf { it.isNotBlank() }
                if (accessToken != null) {
                    supabaseAuth.signOut(
                        accessToken = accessToken,
                        refreshToken =
                            currentState.refreshToken
                    )
                } else {
                    supabaseAuth.clearInMemorySession()
                }
            }

        sessionStore.clearSession()
        return remoteResult
    }

    override suspend fun resetPassword(
        email: String
    ): Result<Unit> =
        runCatching {
            supabaseAuth.requestPasswordReset(email)
        }

    override suspend fun updatePassword(
        recoveryContext: String,
        newPassword: String
    ): Result<Unit> {
        if (recoveryContext.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "Recovery context is required"
                )
            )
        }

        if (newPassword.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "New password is required"
                )
            )
        }

        return runCatching {
            try {
                supabaseAuth.updatePassword(
                    recoveryAccessToken =
                        recoveryContext,
                    newPassword = newPassword
                )
                sessionStore.clearSession()
            } finally {
                supabaseAuth.clearInMemorySession()
            }
        }
    }

    override suspend fun setActiveOrganization(
        organizationId: String
    ): Result<Unit> {
        val normalizedOrganizationId =
            organizationId.trim()

        if (normalizedOrganizationId.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Organization id is required"
                )
            )
        }

        return runCatching {
            val currentState =
                sessionStore.sessionState.first()

            val isAuthorized =
                currentState.authorizedOrganizations.any {
                    it.organizationId ==
                        normalizedOrganizationId
                }

            check(isAuthorized) {
                "Organization is not authorized for current user"
            }

            sessionStore.updateSession(
                currentState.copy(
                    activeOrganizationId =
                        normalizedOrganizationId
                )
            )
        }
    }

    override suspend fun restoreRemoteSession():
        Result<Unit> {
        val currentState =
            sessionStore.sessionState.first()

        val refreshToken =
            currentState.refreshToken
                ?.takeIf { it.isNotBlank() }
                ?: return Result.failure(
                    IllegalStateException(
                        "No protected refresh credential available"
                    )
                )

        return runCatching {
            val authSession =
                supabaseAuth.refreshSession(
                    refreshToken
                )
            val currentUser =
                loadCurrentUser(authSession)
            val authorizedOrganizations =
                currentUser.toAuthorizedOrganizations()

            val restoredActiveOrganizationId =
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
                        restoredActiveOrganizationId,
                    authorizedOrganizations =
                        authorizedOrganizations
                )
            )
        }
    }

    private suspend fun loadCurrentUser(
        authSession: SupabaseAuthSession
    ): CurrentUserResponseDto {
        val currentUser =
            currentUserApi.getCurrentUser(
                authorization =
                    "Bearer " + authSession.accessToken
            )

        check(
            currentUser.user.userId ==
                authSession.userId
        ) {
            "Authenticated subject does not match canonical user"
        }
        return currentUser
    }
}
