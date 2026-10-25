package com.silomath.backend.auth

import java.time.Instant

sealed interface AuthOperationResult<out T> {
    data class Success<T>(val value: T) : AuthOperationResult<T>
    data object InvalidCredentials : AuthOperationResult<Nothing>
    data object UserDisabled : AuthOperationResult<Nothing>
    data object Forbidden : AuthOperationResult<Nothing>
    data object InvalidRecoveryContext : AuthOperationResult<Nothing>
    data object SessionExpired : AuthOperationResult<Nothing>
    data object ServiceUnavailable : AuthOperationResult<Nothing>
}

internal sealed interface ProviderSessionResult {
    data class Success(val session: ProviderSession) : ProviderSessionResult
    data object InvalidCredentials : ProviderSessionResult
    data object SessionExpired : ProviderSessionResult
    data object ServiceUnavailable : ProviderSessionResult
}

internal sealed interface ProviderActionResult {
    data object Success : ProviderActionResult
    data object InvalidContext : ProviderActionResult
    data object ServiceUnavailable : ProviderActionResult
}

internal data class ProviderSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long
)

internal interface AuthenticationProvider {
    suspend fun signIn(email: String, password: String): ProviderSessionResult
    suspend fun refresh(refreshToken: String): ProviderSessionResult
    suspend fun requestPasswordRecovery(email: String): ProviderActionResult
    suspend fun resetPasswordAndRevokeAll(
        recoveryToken: String,
        newPassword: String
    ): ProviderActionResult
    suspend fun signOutLocal(accessToken: String): ProviderActionResult
    suspend fun signOutGlobal(accessToken: String): ProviderActionResult
}

internal interface CanonicalUserDirectory {
    fun findSessionIdentity(userId: String): CanonicalSessionIdentity?
}

interface AuthService {
    suspend fun signIn(email: String, password: String): AuthOperationResult<SessionResponse>
    suspend fun refresh(refreshToken: String): AuthOperationResult<SessionResponse>
    suspend fun forgotPassword(email: String): AuthOperationResult<AcceptedResponse>
    suspend fun resetPassword(
        recoveryToken: String,
        newPassword: String
    ): AuthOperationResult<AcceptedResponse>
    suspend fun signOut(accessToken: String): AuthOperationResult<Unit>
}

internal class DefaultAuthService(
    private val provider: AuthenticationProvider,
    private val userDirectory: CanonicalUserDirectory
) : AuthService {

    override suspend fun signIn(
        email: String,
        password: String
    ): AuthOperationResult<SessionResponse> {
        val providerResult = provider.signIn(email, password)
        return when (providerResult) {
            ProviderSessionResult.InvalidCredentials ->
                AuthOperationResult.InvalidCredentials
            ProviderSessionResult.SessionExpired ->
                AuthOperationResult.InvalidCredentials
            ProviderSessionResult.ServiceUnavailable ->
                AuthOperationResult.ServiceUnavailable
            is ProviderSessionResult.Success ->
                authorizeIssuedSession(providerResult.session)
        }
    }

    override suspend fun refresh(
        refreshToken: String
    ): AuthOperationResult<SessionResponse> {
        return when (val providerResult = provider.refresh(refreshToken)) {
            ProviderSessionResult.InvalidCredentials,
            ProviderSessionResult.SessionExpired ->
                AuthOperationResult.SessionExpired
            ProviderSessionResult.ServiceUnavailable ->
                AuthOperationResult.ServiceUnavailable
            is ProviderSessionResult.Success ->
                authorizeIssuedSession(providerResult.session)
        }
    }

    override suspend fun forgotPassword(
        email: String
    ): AuthOperationResult<AcceptedResponse> {
        return when (provider.requestPasswordRecovery(email)) {
            ProviderActionResult.ServiceUnavailable ->
                AuthOperationResult.ServiceUnavailable
            ProviderActionResult.Success,
            ProviderActionResult.InvalidContext ->
                AuthOperationResult.Success(AcceptedResponse())
        }
    }

    override suspend fun resetPassword(
        recoveryToken: String,
        newPassword: String
    ): AuthOperationResult<AcceptedResponse> {
        return when (
            provider.resetPasswordAndRevokeAll(
                recoveryToken = recoveryToken,
                newPassword = newPassword
            )
        ) {
            ProviderActionResult.Success ->
                AuthOperationResult.Success(AcceptedResponse())
            ProviderActionResult.InvalidContext ->
                AuthOperationResult.InvalidRecoveryContext
            ProviderActionResult.ServiceUnavailable ->
                AuthOperationResult.ServiceUnavailable
        }
    }

    override suspend fun signOut(
        accessToken: String
    ): AuthOperationResult<Unit> {
        return when (provider.signOutLocal(accessToken)) {
            ProviderActionResult.Success,
            ProviderActionResult.InvalidContext ->
                AuthOperationResult.Success(Unit)
            ProviderActionResult.ServiceUnavailable ->
                AuthOperationResult.ServiceUnavailable
        }
    }

    private suspend fun authorizeIssuedSession(
        session: ProviderSession
    ): AuthOperationResult<SessionResponse> {
        val identity = runCatching {
            userDirectory.findSessionIdentity(session.userId)
        }.getOrElse {
            provider.signOutLocal(session.accessToken)
            return AuthOperationResult.ServiceUnavailable
        }

        if (identity == null) {
            provider.signOutLocal(session.accessToken)
            return AuthOperationResult.Forbidden
        }

        if (identity.lifecycle == CanonicalUserLifecycle.DISABLED) {
            provider.signOutGlobal(session.accessToken)
            return AuthOperationResult.UserDisabled
        }

        return AuthOperationResult.Success(
            SessionResponse(
                user = identity.user,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                expiresAtEpochSeconds = session.expiresAtEpochSeconds,
                memberships = identity.memberships
            )
        )
    }
}

internal class UnavailableAuthService : AuthService {
    override suspend fun signIn(email: String, password: String) =
        AuthOperationResult.ServiceUnavailable

    override suspend fun refresh(refreshToken: String) =
        AuthOperationResult.ServiceUnavailable

    override suspend fun forgotPassword(email: String) =
        AuthOperationResult.ServiceUnavailable

    override suspend fun resetPassword(recoveryToken: String, newPassword: String) =
        AuthOperationResult.ServiceUnavailable

    override suspend fun signOut(accessToken: String) =
        AuthOperationResult.ServiceUnavailable
}

internal fun providerExpiry(
    explicitEpochSeconds: Long?,
    expiresInSeconds: Long?
): Long =
    explicitEpochSeconds
        ?: Instant.now().epochSecond + (expiresInSeconds ?: 0L)
