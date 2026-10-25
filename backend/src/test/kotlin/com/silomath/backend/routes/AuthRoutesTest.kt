package com.silomath.backend.routes

import com.silomath.backend.api.configureApiSerialization
import com.silomath.backend.api.configureCorrelationIds
import com.silomath.backend.auth.AcceptedResponse
import com.silomath.backend.auth.AuthOperationResult
import com.silomath.backend.auth.AuthService
import com.silomath.backend.auth.AuthorizedMembershipSummary
import com.silomath.backend.auth.SessionResponse
import com.silomath.backend.auth.SessionUserSummary
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest {

    @Test
    fun `sign in returns canonical session and correlation header`() =
        testApplication {
            application {
                configureCorrelationIds()
                configureApiSerialization()
                configureAuthRouting(FakeAuthService())
            }

            val response = client.post("/api/v1/auth/sign-in") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"email":"user@example.com","password":"secret"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(
                response.headers["X-Correlation-Id"].orEmpty().isNotBlank()
            )
            assertTrue(response.bodyAsText().contains("\"accessToken\":\"access-token\""))
        }

    @Test
    fun `forgot password response is accepted without account enumeration`() =
        testApplication {
            application {
                configureCorrelationIds()
                configureApiSerialization()
                configureAuthRouting(FakeAuthService())
            }

            val response = client.post("/api/v1/auth/forgot-password") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"email":"unknown@example.com"}""")
            }

            assertEquals(HttpStatusCode.Accepted, response.status)
            assertTrue(response.bodyAsText().contains("\"accepted\":true"))
        }

    @Test
    fun `sign out requires bearer credential`() =
        testApplication {
            application {
                configureCorrelationIds()
                configureApiSerialization()
                configureAuthRouting(FakeAuthService())
            }

            val response = client.post("/api/v1/auth/sign-out")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue(response.bodyAsText().contains("AUTH_INVALID_CREDENTIALS"))
        }

    private class FakeAuthService : AuthService {
        override suspend fun signIn(email: String, password: String) =
            AuthOperationResult.Success(
                SessionResponse(
                    user = SessionUserSummary(
                        userId = "user-1",
                        fullName = "Test User",
                        email = email
                    ),
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    expiresAtEpochSeconds = 1234L,
                    memberships = listOf(
                        AuthorizedMembershipSummary(
                            membershipId = "membership-1",
                            organizationId = "org-1",
                            organizationName = "Organization",
                            organizationCode = "ORG",
                            role = "STANDARD"
                        )
                    )
                )
            )

        override suspend fun refresh(refreshToken: String) =
            AuthOperationResult.SessionExpired

        override suspend fun forgotPassword(email: String) =
            AuthOperationResult.Success(AcceptedResponse())

        override suspend fun resetPassword(
            recoveryToken: String,
            newPassword: String
        ) = AuthOperationResult.Success(AcceptedResponse())

        override suspend fun signOut(accessToken: String) =
            AuthOperationResult.Success(Unit)
    }
}
