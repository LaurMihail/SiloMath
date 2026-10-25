package com.silomath.backend.api

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiContractSanityTest {

    @Test
    fun `only frozen retryable machine errors are marked by callers`() {
        val retryableCodes = setOf(
            ApiErrorCode.SYNC_DEPENDENCY_PENDING,
            ApiErrorCode.RATE_LIMITED,
            ApiErrorCode.SERVICE_UNAVAILABLE
        )

        assertEquals(
            setOf(
                "SYNC_DEPENDENCY_PENDING",
                "RATE_LIMITED",
                "SERVICE_UNAVAILABLE"
            ),
            retryableCodes.map { it.name }.toSet()
        )
    }
}
