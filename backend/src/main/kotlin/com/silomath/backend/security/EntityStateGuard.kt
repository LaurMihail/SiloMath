package com.silomath.backend.security

import com.silomath.backend.api.ApiErrorCode
import com.silomath.backend.api.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

/**
 * Frozen state requirements referenced by protected endpoint contracts.
 * These are guards over existing V6 states, not new workflow states.
 */
enum class EntityStateRequirement {
    CAMPAIGN_ACTIVE,
    CAMPAIGN_COMPLETED,
    STOCKTAKING_WORKING_MUTABLE,
    EVIDENCE_WORKING_MUTABLE,
    ISSUE_REOPENABLE
}

data class EntityStateGuardContext(
    val userId: String,
    val organizationId: String,
    val campaignId: String?,
    val entityId: String?,
    val requirement: EntityStateRequirement
)

sealed interface EntityStateGuardResult {
    data object Allowed : EntityStateGuardResult

    data class Conflict(
        val code: ApiErrorCode,
        val message: String
    ) : EntityStateGuardResult
}

/**
 * Adapter boundary for authoritative state evaluation. Implementations should
 * delegate to canonical domain/server workflow policy and current persisted
 * state; this transport layer does not recreate the state machine.
 */
fun interface EntityStateGuardResolver {
    suspend fun evaluate(
        context: EntityStateGuardContext
    ): EntityStateGuardResult
}

fun Route.requireEntityState(
    resolver: EntityStateGuardResolver,
    requirement: EntityStateRequirement,
    campaignParameterName: String? = "campaignId",
    entityParameterName: String? = null,
    build: Route.() -> Unit
) {
    route("") {
        intercept(ApplicationCallPipeline.Plugins) {
            val subject = call.requireAuthenticatedSubject()
            val membership =
                call.requireAuthorizedOrganizationMembership()

            val campaignId = campaignParameterName?.let { name ->
                call.parameters[name]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error(
                        "State-guarded campaign route must define {$name}."
                    )
            }

            val entityId = entityParameterName?.let { name ->
                call.parameters[name]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error(
                        "State-guarded entity route must define {$name}."
                    )
            }

            when (
                val result = resolver.evaluate(
                    EntityStateGuardContext(
                        userId = subject.userId,
                        organizationId = membership.organizationId,
                        campaignId = campaignId,
                        entityId = entityId,
                        requirement = requirement
                    )
                )
            ) {
                EntityStateGuardResult.Allowed -> Unit
                is EntityStateGuardResult.Conflict -> {
                    call.respondApiError(
                        status = HttpStatusCode.Conflict,
                        code = result.code,
                        message = result.message
                    )
                    finish()
                }
            }
        }

        build()
    }
}
