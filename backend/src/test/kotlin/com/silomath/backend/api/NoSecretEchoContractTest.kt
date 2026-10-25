package com.silomath.backend.api

import kotlin.test.Test
import kotlin.test.assertFalse

class NoSecretEchoContractTest {

    @Test
    fun `standard error DTO has no credential fields`() {
        val fieldNames = ApiErrorResponse::class.java.declaredFields
            .map { it.name }
            .toSet()

        assertFalse("authorization" in fieldNames)
        assertFalse("token" in fieldNames)
        assertFalse("accessToken" in fieldNames)
        assertFalse("refreshToken" in fieldNames)
    }
}
