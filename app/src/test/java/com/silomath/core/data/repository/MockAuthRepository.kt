package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MockAuthRepository @Inject constructor(
    private val sessionStore: SessionStore
) : AuthRepository {
    override val sessionState: Flow<SessionState> = sessionStore.sessionState

    override suspend fun signIn(email: String, password: String, rememberMe: Boolean): Result<Unit> {
        return if (email == "admin@silo.com" && password == "password") {
            sessionStore.updateSession(
                SessionState(
                    userId = "admin", 
                    token = "mock_token",
                    isRememberMeEnabled = rememberMe,
                    isOfflineCapable = rememberMe // Semantics: Remembered session is offline capable
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

    override suspend fun setActiveOrganization(organizationId: Long): Result<Unit> {
        return Result.success(Unit)
    }
}
