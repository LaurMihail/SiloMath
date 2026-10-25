package com.silomath.backend.security

import com.silomath.backend.api.ApiErrorCode
import com.silomath.backend.api.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept
import io.ktor.server.routing.route

/**
 * API003 role guard. Role authorization is evaluated only after API002 has
 * established an ACTIVE OrganizationMembership for the requested tenant.
 *
 * Campaign Team membership is deliberately not inferred from ADMIN/STANDARD.
 */
fun Route.requireOrganizationRole(
    allowedRoles: Set<OrganizationRole>,
    build: Route.() -> Unit
) {
    require(allowedRoles.isNotEmpty()) {
        "At least one organization role must be authorized."
    }

    route("") {
        intercept(ApplicationCallPipeline.Plugins) {
            val call = context
            val membership =
                call.requireAuthorizedOrganizationMembership()

            if (membership.role !in allowedRoles) {
                call.respondApiError(
                    status = HttpStatusCode.Forbidden,
                    code = ApiErrorCode.AUTH_FORBIDDEN,
                    message = "The authenticated user is not authorized for this operation."
                )
                finish()
            }
        }

        build()
    }
}

fun Route.requireAdmin(
    build: Route.() -> Unit
) = requireOrganizationRole(
    allowedRoles = setOf(OrganizationRole.ADMIN),
    build = build
)

fun Route.requireAdminOrStandard(
    build: Route.() -> Unit
) = requireOrganizationRole(
    allowedRoles = setOf(
        OrganizationRole.ADMIN,
        OrganizationRole.STANDARD
    ),
    build = build
)
