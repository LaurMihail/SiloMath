package com.silomath.backend.api

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiErrorCorrelationFallbackTest {

    @Test
    fun `server allocates and returns UUID correlation id when absent`() =
        testApplication {
            application {
                configureCorrelationIds()
                routing {
                    get("/error") {
                        call.respondApiError(
                            status = HttpStatusCode.Forbidden,
                            code = ApiErrorCode.AUTH_FORBIDDEN,
                            message = "Forbidden"
                        )
                    }
                }
            }

            val response = client.get("/error")
            val body = Json.parseToJsonElement(
                response.bodyAsText()
            ).jsonObject
            val correlationId =
                body.getValue("correlationId").jsonPrimitive.content

            assertEquals(HttpStatusCode.Forbidden, response.status)
            UUID.fromString(correlationId)
            assertEquals(
                correlationId,
                response.headers[CORRELATION_ID_HEADER]
            )
        }

    @Test
    fun `caller correlation id is propagated to response and error body`() =
        testApplication {
            application {
                configureCorrelationIds()
                routing {
                    get("/error") {
                        call.respondApiError(
                            status = HttpStatusCode.Forbidden,
                            code = ApiErrorCode.AUTH_FORBIDDEN,
                            message = "Forbidden"
                        )
                    }
                }
            }

            val supplied = "client-trace-123"
            val response = client.get("/error") {
                header(CORRELATION_ID_HEADER, supplied)
            }
            val body = Json.parseToJsonElement(
                response.bodyAsText()
            ).jsonObject

            assertEquals(
                supplied,
                response.headers[CORRELATION_ID_HEADER]
            )
            assertEquals(
                supplied,
                body.getValue("correlationId").jsonPrimitive.content
            )
        }
}
