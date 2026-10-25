package com.silomath.backend.security

import com.silomath.backend.api.ApiErrorCode
import com.silomath.backend.api.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept
import io.ktor.server.routing.route

sealed interface CampaignTeamAuthorizationResult {
    data object Authorized : CampaignTeamAuthorizationResult
    data object Forbidden : CampaignTeamAuthorizationResult
}

/**
 * Resolves the frozen Campaign Team authorization dimension for an already
 * authenticated Active User with an already authorized OrganizationMembership.
 */
fun interface CampaignTeamAuthorizationResolver {
    suspend fun authorizeMutation(
        userId: String,
        organizationId: String,
        campaignId: String
    ): CampaignTeamAuthorizationResult
}

/**
 * API004 guard for Stocktaking/other mutations whose endpoint contract requires
 * ACTIVE Campaign Team membership. ADMIN role alone never bypasses this guard.
 */
fun Route.requireCampaignTeamMutationAccess(
    resolver: CampaignTeamAuthorizationResolver,
    campaignParameterName: String = "campaignId",
    build: Route.() -> Unit
) {
    route("") {
        intercept(ApplicationCallPipeline.Plugins) {
            val call = context
            val subject = call.requireAuthenticatedSubject()
            val membership =
                call.requireAuthorizedOrganizationMembership()
            val campaignId =
                call.parameters[campaignParameterName]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error(
                        "Campaign-protected route must define " +
                            "{$campaignParameterName}."
                    )

            when (
                resolver.authorizeMutation(
                    userId = subject.userId,
                    organizationId = membership.organizationId,
                    campaignId = campaignId
                )
            ) {
                CampaignTeamAuthorizationResult.Authorized -> Unit
                CampaignTeamAuthorizationResult.Forbidden -> {
                    call.respondApiError(
                        status = HttpStatusCode.Forbidden,
                        code = ApiErrorCode.STOCKTAKING_NOT_TEAM_MEMBER,
                        message = "Active Campaign Team membership is required for this mutation."
                    )
                    finish()
                }
            }
        }

        build()
    }
}
