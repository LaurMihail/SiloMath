package com.silomath.backend.api

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class IdempotencyInfrastructureTest {

    @Test
    fun `missing key is typed validation failure`() = testApplication {
        val registry = RecordingRegistry()
        application {
            configureCorrelationIds()
            routing {
                get("/mutation") {
                    val result = registry.claimOrReject(
                        call = call,
                        namespace = "test-mutation",
                        requestFingerprint = "fingerprint-a"
                    )
                    if (result == IdempotencyClaimResult.New) {
                        call.respondText("applied")
                    }
                }
            }
        }

        val response = client.get("/mutation")
        val error = Json.parseToJsonElement(
            response.bodyAsText()
        ).jsonObject

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(
            "VALIDATION_FAILED",
            error.getValue("code").jsonPrimitive.content
        )
    }

    @Test
    fun `mismatched reuse returns canonical conflict`() = testApplication {
        val registry = RecordingRegistry(
            result = IdempotencyClaimResult.Mismatch
        )
        application {
            configureCorrelationIds()
            routing {
                get("/mutation") {
                    registry.claimOrReject(
                        call = call,
                        namespace = "test-mutation",
                        requestFingerprint = "fingerprint-b"
                    )
                }
            }
        }

        val response = client.get("/mutation") {
            header(IDEMPOTENCY_KEY_HEADER, "logical-op-1")
        }
        val error = Json.parseToJsonElement(
            response.bodyAsText()
        ).jsonObject

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(
            "IDEMPOTENCY_KEY_REUSE_MISMATCH",
            error.getValue("code").jsonPrimitive.content
        )
        assertEquals("logical-op-1", registry.lastKey)
    }

    private class RecordingRegistry(
        private val result: IdempotencyClaimResult =
            IdempotencyClaimResult.New
    ) : IdempotencyRegistry {
        var lastKey: String? = null

        override suspend fun claim(
            namespace: String,
            key: String,
            requestFingerprint: String
        ): IdempotencyClaimResult {
            lastKey = key
            return result
        }

        override suspend fun complete(record: IdempotencyRecord) = Unit
    }
}
