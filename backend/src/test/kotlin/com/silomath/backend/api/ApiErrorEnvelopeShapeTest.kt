package com.silomath.backend.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiErrorEnvelopeShapeTest {

    @Test
    fun `error envelope defaults preserve frozen field semantics`() {
        val error = ApiErrorResponse(
            code = ApiErrorCode.AUTH_FORBIDDEN,
            message = "Forbidden",
            correlationId = "00000000-0000-0000-0000-000000000001",
            retryable = false
        )

        assertEquals(emptyList(), error.fieldErrors)
        assertNull(error.details)
        assertEquals(false, error.retryable)
    }
}
