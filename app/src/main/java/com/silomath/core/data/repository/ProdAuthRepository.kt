package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.data.network.api.AuthApi
import com.silomath.core.data.network.dto.ForgotPasswordRequestDto
import com.silomath.core.data.network.dto.RefreshSessionRequestDto
import com.silomath.core.data.network.dto.ResetPasswordRequestDto
import com.silomath.core.data.network.dto.SignInRequestDto
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProdAuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val authApi: AuthApi
) : AuthRepository {

    override val sessionState: Flow<SessionState> = sessionStore.sessionState

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<Unit> = runCatching {
        val response = authApi.signIn(
            SignInRequestDto(
                email = email,
                password = password
            )
        )

        sessionStore.updateSession(
            SessionState(
                userId = response.user.userId,
                token = response.accessToken,
                refreshToken = response.refreshToken,
                activeOrganizationId = null,
                isOfflineCapable = false
            )
        )
    }

    override suspend fun signOut(): Result<Unit> {
        val accessToken = sessionStore.sessionState.first().token
        val serverResult = runCatching {
            if (!accessToken.isNullOrBlank()) {
                authApi.signOut("Bearer $accessToken")
            }
        }

        // Local auth credentials are removed even if the network call fails.
        // Business/Room data is deliberately untouched.
        sessionStore.clearSession()
        return serverResult
    }

    override suspend fun resetPassword(email: String): Result<Unit> =
        runCatching {
            authApi.forgotPassword(
                ForgotPasswordRequestDto(email = email)
            )
        }.map { Unit }

    override suspend fun updatePassword(
        recoveryContext: String,
        newPassword: String
    ): Result<Unit> {
        if (recoveryContext.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Recovery context is required")
            )
        }

        if (newPassword.isBlank()) {
            return Result.failure(
                IllegalArgumentException("New password is required")
            )
        }

        return runCatching {
            authApi.resetPassword(
                ResetPasswordRequestDto(
                    recoveryToken = recoveryContext,
                    newPassword = newPassword
                )
            )

            // The backend reports success only after password reset and global
            // session/refresh revocation. Local active auth credentials must
            // therefore be removed as well. Room/business data is untouched.
            sessionStore.clearSession()
        }
    }

    override suspend fun setActiveOrganization(
        organizationId: String
    ): Result<Unit> = runCatching {
        val currentState = sessionStore.sessionState.first()
        sessionStore.updateSession(
            currentState.copy(activeOrganizationId = organizationId)
        )
    }

    override suspend fun restoreRemoteSession(): Result<Unit> {
        val currentState = sessionStore.sessionState.first()
        val refreshToken = currentState.refreshToken?.takeIf { it.isNotBlank() }
            ?: return Result.failure(
                IllegalStateException("No protected refresh credential available")
            )

        return runCatching {
            val response = authApi.refreshSession(
                RefreshSessionRequestDto(refreshToken = refreshToken)
            )

            sessionStore.updateSession(
                currentState.copy(
                    userId = response.user.userId,
                    token = response.accessToken,
                    refreshToken = response.refreshToken
                )
            )
        }
    }
}