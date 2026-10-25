package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MockAuthRepository @Inject constructor(
    private val sessionStore: SessionStore
) : AuthRepository {

    override val sessionState: Flow<SessionState> = sessionStore.sessionState

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<Unit> {
        return if (email == "admin@silo.com" && password == "password") {
            sessionStore.updateSession(
                SessionState(
                    userId = "admin",
                    token = "mock_token",
                    refreshToken = "mock_refresh_token",
                    isOfflineCapable = true
                )
            )
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        sessionStore.clearSession()
        return Result.success(Unit)
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun setActiveOrganization(
        organizationId: String
    ): Result<Unit> {
        return runCatching {
            val current = sessionStore.sessionState.first()
            sessionStore.updateSession(
                current.copy(activeOrganizationId = organizationId)
            )
        }
    }

    override suspend fun restoreRemoteSession(): Result<Unit> {
        return Result.success(Unit)
    }
}