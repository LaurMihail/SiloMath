package com.silomath.core.domain.repository

import com.silomath.core.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionState: Flow<SessionState>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun setActiveOrganization(organizationId: String): Result<Unit>
    suspend fun restoreRemoteSession(): Result<Unit>
}
