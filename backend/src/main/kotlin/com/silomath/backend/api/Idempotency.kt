package com.silomath.backend.api

import io.ktor.server.application.ApplicationCall

const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

data class IdempotencyRecord(
    val namespace: String,
    val key: String,
    val requestFingerprint: String,
    val resultReference: String
)

sealed interface IdempotencyClaimResult {
    data object New : IdempotencyClaimResult
    data class Replay(val record: IdempotencyRecord) : IdempotencyClaimResult
    data object Mismatch : IdempotencyClaimResult
}

/**
 * Atomic server persistence boundary for one logical mutation namespace.
 * Implementations must make claim/complete durable enough to prevent duplicate
 * business effects across retry/restart; this interface deliberately does not
 * prescribe a persistence representation.
 */
interface IdempotencyRegistry {
    suspend fun claim(
        namespace: String,
        key: String,
        requestFingerprint: String
    ): IdempotencyClaimResult

    suspend fun complete(record: IdempotencyRecord)
}

fun ApplicationCall.idempotencyKeyOrNull(): String? =
    request.headers[IDEMPOTENCY_KEY_HEADER]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

suspend fun IdempotencyRegistry.claimOrReject(
    call: ApplicationCall,
    namespace: String,
    requestFingerprint: String
): IdempotencyClaimResult {
    val key = call.idempotencyKeyOrNull()
        ?: return IdempotencyClaimResult.Mismatch

    val result = claim(namespace, key, requestFingerprint)
    if (result == IdempotencyClaimResult.Mismatch) {
        call.respondApiError(
            status = io.ktor.http.HttpStatusCode.Conflict,
            code = ApiErrorCode.IDEMPOTENCY_KEY_REUSE_MISMATCH,
            message = "The idempotency key was reused for a different logical request."
        )
    }
    return result
}
