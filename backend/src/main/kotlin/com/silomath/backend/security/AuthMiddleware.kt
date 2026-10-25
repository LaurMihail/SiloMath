package com.silomath.backend.security

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.util.AttributeKey

/**
 * Provider-neutral authenticated identity established from the server-issued
 * access credential. Token internals are intentionally not exposed as a domain
 * contract.
 */
data class AuthenticatedSubject(
    val userId: String
)

sealed interface AccessTokenVerificationResult {
    data class Authenticated(
        val subject: AuthenticatedSubject
    ) : AccessTokenVerificationResult

    data object InvalidOrExpired : AccessTokenVerificationResult

    data object DisabledUser : AccessTokenVerificationResult
}

/**
 * Boundary implemented by the identity/session adapter. Verification must be
 * authoritative server-side and must include the Active User lifecycle guard.
 */
fun interface AccessTokenVerifier {
    suspend fun verify(accessToken: String): AccessTokenVerificationResult
}

/**
 * Fail-closed default for runtime wiring before an identity provider adapter is
 * supplied. Protected endpoints must never become anonymous by accident.
 */
object RejectingAccessTokenVerifier : AccessTokenVerifier {
    override suspend fun verify(
        accessToken: String
    ): AccessTokenVerificationResult =
        AccessTokenVerificationResult.InvalidOrExpired
}

private val AuthenticatedSubjectKey =
    AttributeKey<AuthenticatedSubject>("SiloMathAuthenticatedSubject")

/**
 * Frozen V6 bearer-authentication boundary for protected routes.
 *
 * Responsibilities are intentionally limited to API001:
 * - require Authorization: Bearer <access-token>;
 * - establish the authenticated Active User subject;
 * - return 401 for missing/invalid/expired credentials;
 * - return 403 for a disabled authenticated user;
 * - never trust X-Organization-Id or other request context as authentication.
 *
 * Organization membership, role, Campaign Team, entity-state and connectivity
 * authorization belong to API002-API005 and compose after this middleware.
 */
fun Route.withActiveBearerAuthentication(
    verifier: AccessTokenVerifier,
    build: Route.() -> Unit
) {
    route("") {
        intercept(ApplicationCallPipeline.Plugins) {
            val token = extractBearerToken(
                call.request.headers[HttpHeaders.Authorization]
            )

            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized)
                finish()
                return@intercept
            }

            when (val result = verifier.verify(token)) {
                AccessTokenVerificationResult.InvalidOrExpired -> {
                    call.respond(HttpStatusCode.Unauthorized)
                    finish()
                }

                AccessTokenVerificationResult.DisabledUser -> {
                    call.respond(HttpStatusCode.Forbidden)
                    finish()
                }

                is AccessTokenVerificationResult.Authenticated -> {
                    call.attributes.put(
                        AuthenticatedSubjectKey,
                        result.subject
                    )
                }
            }
        }

        build()
    }
}

fun ApplicationCall.requireAuthenticatedSubject(): AuthenticatedSubject =
    attributes[AuthenticatedSubjectKey]

internal fun extractBearerToken(
    authorizationHeader: String?
): String? {
    if (authorizationHeader.isNullOrBlank()) {
        return null
    }

    val parts = authorizationHeader
        .trim()
        .split(Regex("\\s+"), limit = 2)

    if (
        parts.size != 2 ||
        !parts[0].equals("Bearer", ignoreCase = true) ||
        parts[1].isBlank()
    ) {
        return null
    }

    return parts[1]
}
