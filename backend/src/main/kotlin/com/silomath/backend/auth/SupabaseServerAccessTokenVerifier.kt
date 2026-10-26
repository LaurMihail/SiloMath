package com.silomath.backend.auth

import com.silomath.backend.security.AccessTokenVerificationResult
import com.silomath.backend.security.AccessTokenVerifier
import com.silomath.backend.security.AuthenticatedSubject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

internal class SupabaseServerAccessTokenVerifier(
    private val baseUrl: String,
    private val secretKey: String,
    private val userDirectory: CanonicalUserDirectory,
    private val httpClient: HttpClient = defaultHttpClient()
) : AccessTokenVerifier {

    override suspend fun verify(
        accessToken: String
    ): AccessTokenVerificationResult {
        return try {
            val response =
                httpClient.get(
                    "$baseUrl/auth/v1/user"
                ) {
                    header(
                        "apikey",
                        secretKey
                    )
                    header(
                        HttpHeaders.Authorization,
                        "Bearer $accessToken"
                    )
                }

            when {
                response.status ==
                    HttpStatusCode.Unauthorized ||
                    response.status ==
                    HttpStatusCode.Forbidden ->
                    AccessTokenVerificationResult
                        .InvalidOrExpired

                response.status.value in 500..599 ->
                    AccessTokenVerificationResult
                        .ServiceUnavailable

                !response.status.isSuccess() ->
                    AccessTokenVerificationResult
                        .InvalidOrExpired

                else -> {
                    val providerUser =
                        response.body<SupabaseUserDto>()

                    val identity =
                        userDirectory.findSessionIdentity(
                            providerUser.id
                        )

                    if (identity == null) {
                        AccessTokenVerificationResult
                            .InvalidOrExpired
                    } else if (
                        identity.lifecycle ==
                        CanonicalUserLifecycle.DISABLED
                    ) {
                        AccessTokenVerificationResult
                            .DisabledUser
                    } else {
                        AccessTokenVerificationResult
                            .Authenticated(
                                AuthenticatedSubject(
                                    userId =
                                        providerUser.id
                                )
                            )
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AccessTokenVerificationResult
                .ServiceUnavailable
        }
    }

    companion object {
        private fun defaultHttpClient():
            HttpClient =
            HttpClient(CIO) {
                install(
                    ContentNegotiation
                ) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                        }
                    )
                }

                install(HttpTimeout) {
                    requestTimeoutMillis =
                        15_000
                    connectTimeoutMillis =
                        10_000
                    socketTimeoutMillis =
                        15_000
                }
            }
    }
}

private fun HttpStatusCode.isSuccess():
    Boolean =
    value in 200..299
