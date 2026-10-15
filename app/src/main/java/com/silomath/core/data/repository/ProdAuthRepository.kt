package com.silomath.core.data.repository

import com.silomath.core.data.auth.SessionStore
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProdAuthRepository @Inject constructor(
    private val sessionStore: SessionStore
) : AuthRepository {

    override val sessionState: Flow<SessionState> = sessionStore.sessionState

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return Result.failure(Exception("Backend authentication contract required"))
    }

    override suspend fun signOut(): Result<Unit> {
        sessionStore.clearSession()
        return Result.success(Unit)
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.failure(Exception("Backend authentication contract required"))
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return Result.failure(Exception("Backend authentication contract required"))
    }

    override suspend fun setActiveOrganization(organizationId: String): Result<Unit> {
        return try {
            val currentState = sessionStore.sessionState.first()
            val newState = currentState.copy(activeOrganizationId = organizationId)
            sessionStore.updateSession(newState)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreRemoteSession(): Result<Unit> {
        return Result.failure(Exception("Backend authentication contract required"))
    }
}
