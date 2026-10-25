package com.silomath.backend.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal class SupabaseAuthenticationProvider(
    private val baseUrl: String,
    private val secretKey: String,
    private val httpClient: HttpClient = defaultHttpClient()
) : AuthenticationProvider {

    override suspend fun signIn(
        email: String,
        password: String
    ): ProviderSessionResult =
        runCatching {
            val response = httpClient.post(
                "$baseUrl/auth/v1/token?grant_type=password"
            ) {
                providerHeaders()
                contentType(ContentType.Application.Json)
                setBody(
                    SupabasePasswordGrantRequest(
                        email = email,
                        password = password
                    )
                )
            }

            when {
                response.status.isSuccess() ->
                    response.body<SupabaseSessionDto>().toProviderResult()
                response.status == HttpStatusCode.BadRequest ||
                    response.status == HttpStatusCode.Unauthorized ||
                    response.status == HttpStatusCode.Forbidden ->
                    ProviderSessionResult.InvalidCredentials
                else ->
                    ProviderSessionResult.ServiceUnavailable
            }
        }.getOrElse {
            ProviderSessionResult.ServiceUnavailable
        }

    override suspend fun refresh(
        refreshToken: String
    ): ProviderSessionResult =
        runCatching {
            val response = httpClient.post(
                "$baseUrl/auth/v1/token?grant_type=refresh_token"
            ) {
                providerHeaders()
                contentType(ContentType.Application.Json)
                setBody(SupabaseRefreshGrantRequest(refreshToken))
            }

            when {
                response.status.isSuccess() ->
                    response.body<SupabaseSessionDto>().toProviderResult()
                response.status == HttpStatusCode.BadRequest ||
                    response.status == HttpStatusCode.Unauthorized ||
                    response.status == HttpStatusCode.Forbidden ->
                    ProviderSessionResult.SessionExpired
                else ->
                    ProviderSessionResult.ServiceUnavailable
            }
        }.getOrElse {
            ProviderSessionResult.ServiceUnavailable
        }

    override suspend fun requestPasswordRecovery(
        email: String
    ): ProviderActionResult =
        runCatching {
            val response = httpClient.post("$baseUrl/auth/v1/recover") {
                providerHeaders()
                contentType(ContentType.Application.Json)
                setBody(SupabaseRecoverRequest(email))
            }

            when {
                response.status.isSuccess() -> ProviderActionResult.Success
                response.status.value in 400..499 -> ProviderActionResult.InvalidContext
                else -> ProviderActionResult.ServiceUnavailable
            }
        }.getOrElse {
            ProviderActionResult.ServiceUnavailable
        }

    override suspend fun resetPasswordAndRevokeAll(
        recoveryToken: String,
        newPassword: String
    ): ProviderActionResult =
        runCatching {
            val updateResponse = httpClient.put("$baseUrl/auth/v1/user") {
                providerHeaders()
                header(HttpHeaders.Authorization, "Bearer $recoveryToken")
                contentType(ContentType.Application.Json)
                setBody(SupabaseUpdatePasswordRequest(newPassword))
            }

            if (!updateResponse.status.isSuccess()) {
                return@runCatching when {
                    updateResponse.status.value in 400..499 ->
                        ProviderActionResult.InvalidContext
                    else ->
                        ProviderActionResult.ServiceUnavailable
                }
            }

            val revokeResponse = httpClient.post(
                "$baseUrl/auth/v1/logout?scope=global"
            ) {
                providerHeaders()
                header(HttpHeaders.Authorization, "Bearer $recoveryToken")
            }

            if (revokeResponse.status == HttpStatusCode.NoContent) {
                ProviderActionResult.Success
            } else {
                ProviderActionResult.ServiceUnavailable
            }
        }.getOrElse {
            ProviderActionResult.ServiceUnavailable
        }

    override suspend fun signOutLocal(
        accessToken: String
    ): ProviderActionResult =
        signOut(accessToken, "local")

    override suspend fun signOutGlobal(
        accessToken: String
    ): ProviderActionResult =
        signOut(accessToken, "global")

    private suspend fun signOut(
        accessToken: String,
        scope: String
    ): ProviderActionResult =
        runCatching {
            val response = httpClient.post(
                "$baseUrl/auth/v1/logout?scope=$scope"
            ) {
                providerHeaders()
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }

            when {
                response.status == HttpStatusCode.NoContent ->
                    ProviderActionResult.Success
                response.status == HttpStatusCode.Unauthorized ||
                    response.status == HttpStatusCode.Forbidden ->
                    ProviderActionResult.InvalidContext
                else ->
                    ProviderActionResult.ServiceUnavailable
            }
        }.getOrElse {
            ProviderActionResult.ServiceUnavailable
        }

    private fun io.ktor.client.request.HttpRequestBuilder.providerHeaders() {
        header("apikey", secretKey)
    }

    private fun SupabaseSessionDto.toProviderResult(): ProviderSessionResult =
        if (
            accessToken.isBlank() ||
            refreshToken.isBlank() ||
            user.id.isBlank()
        ) {
            ProviderSessionResult.ServiceUnavailable
        } else {
            ProviderSessionResult.Success(
                ProviderSession(
                    userId = user.id,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAtEpochSeconds = providerExpiry(
                        explicitEpochSeconds = expiresAtEpochSeconds,
                        expiresInSeconds = expiresInSeconds
                    )
                )
            )
        }

    companion object {
        private fun defaultHttpClient(): HttpClient =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                        }
                    )
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 15_000
                }
            }
    }
}

private fun HttpStatusCode.isSuccess(): Boolean =
    value in 200..299
