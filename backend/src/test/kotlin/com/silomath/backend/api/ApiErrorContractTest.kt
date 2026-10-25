package com.silomath.backend.api

import com.silomath.backend.config.BackendConfig
import com.silomath.backend.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiErrorContractTest {

    private val testConfig =
        BackendConfig(
            host = "127.0.0.1",
            port = 8080,
            runtimeEnvironment = "test"
        )

    @Test
    fun `api 404 uses frozen JSON error envelope`() = testApplication {
        application {
            module(testConfig)
        }

        val response = client.get("/api/v1/missing")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        assertEquals(
            ApiErrorCode.RESOURCE_NOT_FOUND.name,
            body.getValue("code").jsonPrimitive.content
        )
        assertTrue(body.getValue("message").jsonPrimitive.content.isNotBlank())
        assertEquals(0, body.getValue("fieldErrors").jsonArray.size)
        assertEquals(false, body.getValue("retryable").jsonPrimitive.content.toBoolean())

        val correlationId = body["correlationId"]?.jsonPrimitive?.content
        assertNotNull(correlationId)
        UUID.fromString(correlationId)
    }
}
