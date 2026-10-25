package com.silomath.backend.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

const val ENTITY_VERSION_HEADER = "If-Match-Entity-Version"

sealed interface EntityVersionCheckResult {
    data object Match : EntityVersionCheckResult
    data class Conflict(
        val baseEntityVersion: Long,
        val authoritativeEntityVersion: Long
    ) : EntityVersionCheckResult
}

fun ApplicationCall.baseEntityVersionOrNull(): Long? =
    request.headers[ENTITY_VERSION_HEADER]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.toLongOrNull()

fun checkEntityVersion(
    baseEntityVersion: Long,
    authoritativeEntityVersion: Long
): EntityVersionCheckResult =
    if (baseEntityVersion == authoritativeEntityVersion) {
        EntityVersionCheckResult.Match
    } else {
        EntityVersionCheckResult.Conflict(
            baseEntityVersion = baseEntityVersion,
            authoritativeEntityVersion = authoritativeEntityVersion
        )
    }

suspend fun ApplicationCall.respondEntityVersionConflict(
    authoritativeEntityVersion: Long
) {
    respondApiError(
        status = HttpStatusCode.Conflict,
        code = ApiErrorCode.ENTITY_VERSION_CONFLICT,
        message = "The entity has changed since the supplied base version."
    )
}
