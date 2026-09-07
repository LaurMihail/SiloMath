package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DebugAuthRepository @Inject constructor(
    private val sessionStore: SessionStore
) : AuthRepository {
    override val sessionState: Flow<SessionState> = sessionStore.sessionState

    override suspend fun signIn(email: String, password: String, rememberMe: Boolean): Result<Unit> {
        return if (email == "laurentiu.caramarin@gmail.com" && password == "123456") {
            sessionStore.updateSession(
                SessionState(
                    userId = "debug_user",
                    token = "debug_token",
                    isRememberMeEnabled = rememberMe,
                    isOfflineCapable = true
                )
            )
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid debug credentials"))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        sessionStore.clearSession()
        return Result.success(Unit)
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.failure(Exception("Not available in debug auth"))
    }

    override suspend fun setActiveOrganization(organizationId: Long): Result<Unit> {
        return try {
            val currentState = sessionStore.sessionState.first()
            val newState = currentState.copy(activeOrganizationId = organizationId)
            sessionStore.updateSession(newState)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
