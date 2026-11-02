package com.silomath.core.data.auth

import com.silomath.core.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

interface SessionStore {
    val sessionState: Flow<SessionState>
    suspend fun updateSession(sessionState: SessionState)
    suspend fun clearSession()
}
