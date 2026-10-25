package com.silomath.backend.api

import io.ktor.client.request.get
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
    fun `error responder allocates UUID correlation id before API007`() =
        testApplication {
            application {
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

            assertEquals(HttpStatusCode.Forbidden, response.status)
            UUID.fromString(
                body.getValue("correlationId").jsonPrimitive.content
            )
        }
}
