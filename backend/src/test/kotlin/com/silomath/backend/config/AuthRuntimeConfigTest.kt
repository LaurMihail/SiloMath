package com.silomath.backend.config

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthRuntimeConfigTest {

    @Test
    fun `production rejects missing auth runtime secrets`() {
        assertFailsWith<IllegalArgumentException> {
            BackendConfig.fromEnvironment(
                mapOf(
                    "SILOMATH_BACKEND_ENVIRONMENT" to "production"
                )
            )
        }
    }

    @Test
    fun `development may start fail closed without auth runtime secrets`() {
        val config = BackendConfig.fromEnvironment(emptyMap())
        assertTrue(!config.authRuntimeConfigured)
    }
}
