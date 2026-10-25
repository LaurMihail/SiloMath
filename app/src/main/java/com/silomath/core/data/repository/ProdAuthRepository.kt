package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.data.network.api.AuthApi
import com.silomath.core.data.network.dto.ForgotPasswordRequestDto
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

    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        // The frozen reset endpoint requires a server-issued recovery context.
        // The current domain method does not carry that context; AUTH-006 UI
        // integration must supply it rather than bypassing the backend.
        return Result.failure(
            IllegalStateException("Recovery context is required by backend authentication contract")
        )
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
        // Session restoration/rotation is implemented through the canonical
        // NetworkSessionProvider in AUTH-003; this repository remains fail-closed
        // until that flow invokes the provider with stored protected credentials.
        return Result.failure(
            IllegalStateException("Session restoration flow not initialized")
        )
    }
}
