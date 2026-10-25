package com.silomath.backend.api

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiErrorResponderTest {

    @Test
    fun `explicit API error responder emits machine code and retryable flag`() =
        testApplication {
            application {
                routing {
                    get("/error") {
                        call.respondApiError(
                            status = HttpStatusCode.ServiceUnavailable,
                            code = ApiErrorCode.SERVICE_UNAVAILABLE,
                            message = "Service dependency is unavailable.",
                            retryable = true,
                            correlationId = "00000000-0000-0000-0000-000000000001"
                        )
                    }
                }
            }

            val response = client.get("/error")
            val body = Json.parseToJsonElement(
                response.bodyAsText()
            ).jsonObject

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals(
                "SERVICE_UNAVAILABLE",
                body.getValue("code").jsonPrimitive.content
            )
            assertEquals(
                "true",
                body.getValue("retryable").jsonPrimitive.content
            )
            assertEquals(
                "00000000-0000-0000-0000-000000000001",
                body.getValue("correlationId").jsonPrimitive.content
            )
        }
}
