package com.silomath.core.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton

data class SupabaseAuthSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)

interface SupabaseAuthGateway {
    suspend fun signIn(email: String, password: String): SupabaseAuthSession
    suspend fun refreshSession(refreshToken: String): SupabaseAuthSession
    suspend fun signOut(accessToken: String, refreshToken: String?)
    suspend fun requestPasswordReset(email: String)
    suspend fun updatePassword(recoveryAccessToken: String, newPassword: String)
    suspend fun clearInMemorySession()
}

@Singleton
class DefaultSupabaseAuthGateway @Inject constructor(
    private val supabaseClient: SupabaseClient
) : SupabaseAuthGateway {

    override suspend fun signIn(
        email: String,
        password: String
    ): SupabaseAuthSession {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return requireNotNull(supabaseClient.auth.currentSessionOrNull()) {
            "Supabase Auth did not return a session"
        }.toAuthSession()
    }

    override suspend fun refreshSession(
        refreshToken: String
    ): SupabaseAuthSession =
        supabaseClient.auth
            .refreshSession(refreshToken)
            .toAuthSession()

    override suspend fun signOut(
        accessToken: String,
        refreshToken: String?
    ) {
        supabaseClient.auth.importAuthToken(
            accessToken = accessToken,
            refreshToken = refreshToken.orEmpty(),
            retrieveUser = false,
            autoRefresh = false
        )
        supabaseClient.auth.signOut()
    }

    override suspend fun requestPasswordReset(
        email: String
    ) {
        supabaseClient.auth.resetPasswordForEmail(email)
    }

    override suspend fun updatePassword(
        recoveryAccessToken: String,
        newPassword: String
    ) {
        supabaseClient.auth.importAuthToken(
            accessToken = recoveryAccessToken,
            retrieveUser = true,
            autoRefresh = false
        )
        supabaseClient.auth.updateUser {
            password = newPassword
        }
    }

    override suspend fun clearInMemorySession() {
        supabaseClient.auth.clearSession()
    }

    private fun io.github.jan.supabase.auth.user.UserSession
        .toAuthSession(): SupabaseAuthSession {
        val authenticatedUserId =
            requireNotNull(user?.id?.takeIf { it.isNotBlank() }) {
                "Supabase Auth session has no authenticated user"
            }

        return SupabaseAuthSession(
            userId = authenticatedUserId,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}
