package com.silomath.core.data.network.auth

import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes one protected Retrofit operation with the canonical V6
 * authentication recovery behavior:
 *
 * 1. execute once with the current access token;
 * 2. on HTTP 401, attempt secure session refresh once;
 * 3. retry the same operation once when refresh succeeds;
 * 4. propagate failure when refresh fails or the retry fails.
 *
 * The retry boundary is suspendable so token refresh does not require
 * runBlocking inside OkHttp's synchronous Authenticator contract.
 */
@Singleton
class AuthenticatedApiCallExecutor @Inject constructor(
    private val sessionProvider: NetworkSessionProvider
) {

    suspend fun <T> execute(
        operation: suspend () -> T
    ): T {
        try {
            return operation()
        } catch (firstFailure: HttpException) {
            if (firstFailure.code() != HTTP_UNAUTHORIZED) {
                throw firstFailure
            }

            val failedAccessToken =
                firstFailure
                    .response()
                    ?.raw()
                    ?.request
                    ?.header(AUTHORIZATION_HEADER)
                    ?.removePrefix(BEARER_PREFIX)
                    ?.takeIf { it.isNotBlank() }

            val refreshedAccessToken =
                sessionProvider
                    .refreshAccessToken(
                        failedAccessToken = failedAccessToken
                    )
                    ?.takeIf { it.isNotBlank() }

            if (refreshedAccessToken == null) {
                throw firstFailure
            }

            // Exactly one retry. A second 401 is propagated to the caller
            // and must not initiate another refresh loop.
            return operation()
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
