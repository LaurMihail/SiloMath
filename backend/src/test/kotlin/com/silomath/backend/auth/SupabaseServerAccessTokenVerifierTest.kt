package com.silomath.backend.auth

import com.silomath.backend.security.AccessTokenVerificationResult
import com.silomath.backend.security.AuthenticatedSubject
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.io.IOException
import java.net.SocketTimeoutException
import java.sql.SQLException
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SupabaseServerAccessTokenVerifierTest {
    @Test
    fun `Supabase 401 and 403 remain invalid or expired`() {
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)) {
            verifyCase(status, expected = AccessTokenVerificationResult.InvalidOrExpired)
        }
    }

    @Test
    fun `Supabase 5xx are service unavailable`() {
        for (code in 500..599) {
            verifyCase(HttpStatusCode.fromValue(code))
        }
    }

    @Test
    fun `network failure is service unavailable`() =
        verifyCase(requestFailure = IOException("Connection unavailable"))

    @Test
    fun `network timeout is service unavailable`() =
        verifyCase(requestFailure = SocketTimeoutException("Timed out"))

    @Test
    fun `canonical directory failure is service unavailable`() =
        verifyCase(directoryFailure = SQLException("Directory unavailable"))

    @Test
    fun `malformed provider response is service unavailable`() =
        verifyCase(body = "not-json")

    @Test
    fun `active user remains authenticated`() =
        verifyCase(
            expected = AccessTokenVerificationResult.Authenticated(
                AuthenticatedSubject("user-1")
            )
        )

    @Test
    fun `disabled user remains disabled`() =
        verifyCase(
            lifecycle = CanonicalUserLifecycle.DISABLED,
            expected = AccessTokenVerificationResult.DisabledUser
        )

    @Test
    fun `missing canonical user remains invalid or expired`() =
        verifyCase(missingIdentity = true, expected = AccessTokenVerificationResult.InvalidOrExpired)

    @Test
    fun `coroutine cancellation is propagated`() =
        verifyCase(requestFailure = CancellationException("Cancelled"))

    private fun verifyCase(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"id":"user-1"}""",
        requestFailure: Exception? = null,
        directoryFailure: Exception? = null,
        lifecycle: CanonicalUserLifecycle = CanonicalUserLifecycle.ACTIVE,
        missingIdentity: Boolean = false,
        expected: AccessTokenVerificationResult = AccessTokenVerificationResult.ServiceUnavailable
    ) = testApplication {
        var directoryCalls = 0
        externalServices {
            hosts("https://supabase.test") {
                routing {
                    get("/auth/v1/user") {
                        assertEquals("test-secret", call.request.headers["apikey"])
                        assertEquals("Bearer test-token", call.request.headers["Authorization"])
                        call.respondText(body, ContentType.Application.Json, status)
                    }
                }
            }
        }
        val httpClient = createClient {
            install(ContentNegotiation) { json() }
        }
        if (requestFailure != null) {
            httpClient.requestPipeline.intercept(HttpRequestPipeline.Before) {
                throw requestFailure
            }
        }
        val directory = object : CanonicalUserDirectory {
            override fun findSessionIdentity(userId: String): CanonicalSessionIdentity? {
                directoryCalls += 1
                assertEquals("user-1", userId)
                if (directoryFailure != null) throw directoryFailure
                if (missingIdentity) return null
                return CanonicalSessionIdentity(
                    user = SessionUserSummary("user-1", "Test User", "user@example.test"),
                    lifecycle = lifecycle,
                    memberships = emptyList()
                )
            }
        }
        val verifier = SupabaseServerAccessTokenVerifier(
            baseUrl = "https://supabase.test",
            secretKey = "test-secret",
            userDirectory = directory,
            httpClient = httpClient
        )
        try {
            if (requestFailure is CancellationException) {
                assertFailsWith<CancellationException> { verifier.verify("test-token") }
            } else {
                assertEquals(expected, verifier.verify("test-token"))
            }
            val shouldReadDirectory = requestFailure == null && status == HttpStatusCode.OK && body.startsWith("{")
            assertEquals(if (shouldReadDirectory) 1 else 0, directoryCalls)
        } finally {
            httpClient.close()
        }
    }
}