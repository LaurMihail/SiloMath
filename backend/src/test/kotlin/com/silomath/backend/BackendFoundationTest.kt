package com.silomath.backend

import com.silomath.backend.config.BackendConfig
import com.silomath.backend.routes.FoundationRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class BackendFoundationTest {

    private val testConfig =
        BackendConfig(
            host = "127.0.0.1",
            port = 8080,
            runtimeEnvironment = "test"
        )

    @Test
    fun `canonical API base path is frozen api v1`() {
        assertEquals(
            "/api/v1",
            FoundationRoutes.API_BASE_PATH
        )
    }

    @Test
    fun `liveness endpoint is available`() = testApplication {
        application {
            module(testConfig)
        }

        val response = client.get("/health/live")

        assertEquals(
            HttpStatusCode.OK,
            response.status
        )
        assertEquals(
            "OK",
            response.bodyAsText()
        )
    }

    @Test
    fun `readiness endpoint is available`() = testApplication {
        application {
            module(testConfig)
        }

        val response = client.get("/health/ready")

        assertEquals(
            HttpStatusCode.OK,
            response.status
        )
        assertEquals(
            "READY",
            response.bodyAsText()
        )
    }

    @Test
    fun `foundation exposes no business endpoint`() = testApplication {
        application {
            module(testConfig)
        }

        val response = client.get("/api/v1")

        assertEquals(
            HttpStatusCode.NotFound,
            response.status
        )
    }
}