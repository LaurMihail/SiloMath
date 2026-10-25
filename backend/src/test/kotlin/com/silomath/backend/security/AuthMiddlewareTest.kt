package com.silomath.backend.security

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthMiddlewareTest {

    @Test
    fun `missing bearer credential returns 401 without invoking verifier`() =
        testApplication {
            var verifierCalls = 0

            application {
                routing {
                    withActiveBearerAuthentication(
                        verifier = AccessTokenVerifier {
                            verifierCalls += 1
                            AccessTokenVerificationResult.Authenticated(
                                AuthenticatedSubject("user-1")
                            )
                        }
                    ) {
                        get("/protected") {
                            call.respondText("protected")
                        }
                    }
                }
            }

            val response = client.get("/protected")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals(0, verifierCalls)
        }

    @Test
    fun `invalid or expired bearer credential returns 401`() =
        testApplication {
            application {
                routing {
                    withActiveBearerAuthentication(
                        verifier = AccessTokenVerifier {
                            AccessTokenVerificationResult.InvalidOrExpired
                        }
                    ) {
                        get("/protected") {
                            call.respondText("protected")
                        }
                    }
                }
            }

            val response = client.get("/protected") {
                header(HttpHeaders.Authorization, "Bearer expired-token")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `disabled authenticated user returns 403`() =
        testApplication {
            application {
                routing {
                    withActiveBearerAuthentication(
                        verifier = AccessTokenVerifier {
                            AccessTokenVerificationResult.DisabledUser
                        }
                    ) {
                        get("/protected") {
                            call.respondText("protected")
                        }
                    }
                }
            }

            val response = client.get("/protected") {
                header(HttpHeaders.Authorization, "Bearer valid-token")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `valid bearer credential establishes canonical authenticated subject`() =
        testApplication {
            application {
                routing {
                    withActiveBearerAuthentication(
                        verifier = AccessTokenVerifier { token ->
                            assertEquals("valid-token", token)
                            AccessTokenVerificationResult.Authenticated(
                                AuthenticatedSubject("user-42")
                            )
                        }
                    ) {
                        get("/protected") {
                            call.respondText(
                                call.requireAuthenticatedSubject().userId
                            )
                        }
                    }
                }
            }

            val response = client.get("/protected") {
                header(HttpHeaders.Authorization, "Bearer valid-token")
                header("X-Organization-Id", "must-not-authenticate")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("user-42", response.bodyAsText())
        }

    @Test
    fun `non bearer authorization scheme is rejected`() =
        testApplication {
            application {
                routing {
                    withActiveBearerAuthentication(
                        verifier = AccessTokenVerifier {
                            AccessTokenVerificationResult.Authenticated(
                                AuthenticatedSubject("user-1")
                            )
                        }
                    ) {
                        get("/protected") {
                            call.respondText("protected")
                        }
                    }
                }
            }

            val response = client.get("/protected") {
                header(HttpHeaders.Authorization, "Basic abc123")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
