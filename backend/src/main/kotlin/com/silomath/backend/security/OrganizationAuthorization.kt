package com.silomath.backend.security

import com.silomath.backend.api.ApiErrorCode
import com.silomath.backend.api.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept
import io.ktor.server.routing.route
import io.ktor.util.AttributeKey

enum class OrganizationRole {
    ADMIN,
    STANDARD
}

data class AuthorizedOrganizationMembership(
    val membershipId: String,
    val organizationId: String,
    val role: OrganizationRole
)

sealed interface OrganizationAuthorizationResult {
    data class Authorized(
        val membership: AuthorizedOrganizationMembership
    ) : OrganizationAuthorizationResult

    data object Forbidden : OrganizationAuthorizationResult
}

/**
 * Server-authoritative lookup of an ACTIVE OrganizationMembership for the
 * already authenticated Active User.
 */
fun interface OrganizationAuthorizationResolver {
    suspend fun authorize(
        userId: String,
        organizationId: String
    ): OrganizationAuthorizationResult
}

private val OrganizationMembershipKey =
    AttributeKey<AuthorizedOrganizationMembership>(
        "SiloMathAuthorizedOrganizationMembership"
    )

/**
 * API002 tenant/membership guard. Must be installed inside API001 bearer auth.
 * Client-supplied Organization context never becomes proof of authority.
 */
fun Route.withOrganizationAuthorization(
    resolver: OrganizationAuthorizationResolver,
    organizationParameterName: String = "organizationId",
    build: Route.() -> Unit
) {
    route("") {
        intercept(ApplicationCallPipeline.Plugins) {
            val call = context
            val subject = call.requireAuthenticatedSubject()
            val organizationId =
                call.parameters[organizationParameterName]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error(
                        "Protected organization route must define " +
                            "{$organizationParameterName}."
                    )

            when (
                val result = resolver.authorize(
                    userId = subject.userId,
                    organizationId = organizationId
                )
            ) {
                OrganizationAuthorizationResult.Forbidden -> {
                    call.respondApiError(
                        status = HttpStatusCode.Forbidden,
                        code = ApiErrorCode.TENANT_SCOPE_VIOLATION,
                        message = "The requested organization is outside the authorized scope."
                    )
                    finish()
                }

                is OrganizationAuthorizationResult.Authorized -> {
                    require(
                        result.membership.organizationId == organizationId
                    ) {
                        "Organization authorization resolver returned a " +
                            "membership for a different tenant."
                    }
                    call.attributes.put(
                        OrganizationMembershipKey,
                        result.membership
                    )
                }
            }
        }

        build()
    }
}

fun ApplicationCall.requireAuthorizedOrganizationMembership():
    AuthorizedOrganizationMembership =
    attributes[OrganizationMembershipKey]
