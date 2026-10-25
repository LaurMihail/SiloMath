package com.silomath.backend.api

import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalErrorCodesTest {

    @Test
    fun `machine error registry matches frozen V6 codes`() {
        assertEquals(
            setOf(
                "AUTH_INVALID_CREDENTIALS",
                "AUTH_SESSION_EXPIRED",
                "AUTH_USER_DISABLED",
                "AUTH_FORBIDDEN",
                "TENANT_SCOPE_VIOLATION",
                "RESOURCE_NOT_FOUND",
                "VALIDATION_FAILED",
                "ENTITY_VERSION_CONFLICT",
                "IDEMPOTENCY_KEY_REUSE_MISMATCH",
                "CAMPAIGN_SCOPE_FROZEN",
                "CAMPAIGN_COMPLETED_IMMUTABLE",
                "CAMPAIGN_TEAM_REQUIRED",
                "LAST_ACTIVE_ADMIN",
                "STOCKTAKING_NOT_TEAM_MEMBER",
                "STOCKTAKING_REVISION_STALE",
                "STOCKTAKING_VALIDATION_FAILED",
                "CALCULATION_STALE_FINGERPRINT",
                "CALCULATION_INVALID_INPUT",
                "EVIDENCE_PHOTO_LIMIT",
                "EVIDENCE_HASH_MISMATCH",
                "EVIDENCE_TOO_LARGE",
                "EVIDENCE_IMMUTABLE",
                "ISSUE_ALREADY_STATE",
                "ISSUE_COMPLETED_CAMPAIGN",
                "CLOSE_GUARDS_FAILED",
                "SYNC_CONFLICT",
                "SYNC_DEPENDENCY_PENDING",
                "RATE_LIMITED",
                "SERVICE_UNAVAILABLE",
                "INTERNAL_ERROR"
            ),
            ApiErrorCode.entries.map { it.name }.toSet()
        )
    }
}
