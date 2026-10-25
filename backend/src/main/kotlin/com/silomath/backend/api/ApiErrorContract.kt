package com.silomath.backend.api

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.application.install
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
enum class ApiErrorCode {
    AUTH_INVALID_CREDENTIALS,
    AUTH_SESSION_EXPIRED,
    AUTH_USER_DISABLED,
    AUTH_FORBIDDEN,
    TENANT_SCOPE_VIOLATION,
    RESOURCE_NOT_FOUND,
    VALIDATION_FAILED,
    ENTITY_VERSION_CONFLICT,
    IDEMPOTENCY_KEY_REUSE_MISMATCH,
    CAMPAIGN_SCOPE_FROZEN,
    CAMPAIGN_COMPLETED_IMMUTABLE,
    CAMPAIGN_TEAM_REQUIRED,
    LAST_ACTIVE_ADMIN,
    STOCKTAKING_NOT_TEAM_MEMBER,
    STOCKTAKING_REVISION_STALE,
    STOCKTAKING_VALIDATION_FAILED,
    CALCULATION_STALE_FINGERPRINT,
    CALCULATION_INVALID_INPUT,
    EVIDENCE_PHOTO_LIMIT,
    EVIDENCE_HASH_MISMATCH,
    EVIDENCE_TOO_LARGE,
    EVIDENCE_IMMUTABLE,
    ISSUE_ALREADY_STATE,
    ISSUE_COMPLETED_CAMPAIGN,
    CLOSE_GUARDS_FAILED,
    SYNC_CONFLICT,
    SYNC_DEPENDENCY_PENDING,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    INTERNAL_ERROR
}

@Serializable
data class ApiFieldError(
    val field: String,
    val code: String,
    val message: String
)

@Serializable
data class ApiErrorResponse(
    val code: ApiErrorCode,
    val message: String,
    val fieldErrors: List<ApiFieldError> = emptyList(),
    val correlationId: String,
    val retryable: Boolean,
    val details: JsonObject? = null
)

fun Application.configureApiSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = true
                explicitNulls = false
            }
        )
    }
}

/**
 * Standard frozen V6 JSON error responder.
 *
 * API007 will replace the fallback UUID generation with the request-scoped
 * correlation-id pipeline. Until then every error still receives a valid
 * opaque correlation identifier and no secret/request payload is reflected.
 */
suspend fun ApplicationCall.respondApiError(
    status: HttpStatusCode,
    code: ApiErrorCode,
    message: String,
    retryable: Boolean = false,
    fieldErrors: List<ApiFieldError> = emptyList(),
    details: JsonObject? = null,
    correlationId: String = UUID.randomUUID().toString()
) {
    respond(
        status = status,
        message = ApiErrorResponse(
            code = code,
            message = message,
            fieldErrors = fieldErrors,
            correlationId = correlationId,
            retryable = retryable,
            details = details
        )
    )
}
