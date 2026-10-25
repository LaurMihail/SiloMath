package com.silomath.backend.routes

import com.silomath.backend.api.ApiErrorCode
import com.silomath.backend.api.ApiFieldError
import com.silomath.backend.api.respondApiError
import com.silomath.backend.auth.AcceptedResponse
import com.silomath.backend.auth.AuthOperationResult
import com.silomath.backend.auth.AuthService
import com.silomath.backend.auth.ForgotPasswordRequest
import com.silomath.backend.auth.RefreshSessionRequest
import com.silomath.backend.auth.ResetPasswordRequest
import com.silomath.backend.auth.SessionResponse
import com.silomath.backend.auth.SignInRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureAuthRouting(
    authService: AuthService
) {
    routing {
        route("/api/v1/auth") {
            post("/sign-in") {
                val request = call.receive<SignInRequest>()
                val validationErrors = buildList {
                    if (request.email.isBlank()) {
                        add(ApiFieldError("email", "REQUIRED", "Email is required."))
                    }
                    if (request.password.isBlank()) {
                        add(ApiFieldError("password", "REQUIRED", "Password is required."))
                    }
                }

                if (validationErrors.isNotEmpty()) {
                    call.respondApiError(
                        status = HttpStatusCode.BadRequest,
                        code = ApiErrorCode.VALIDATION_FAILED,
                        message = "The request is invalid.",
                        fieldErrors = validationErrors
                    )
                    return@post
                }

                call.respondAuthResult(
                    authService.signIn(
                        email = request.email.trim(),
                        password = request.password
                    )
                )
            }

            post("/forgot-password") {
                val request = call.receive<ForgotPasswordRequest>()
                if (request.email.isBlank()) {
                    call.respondApiError(
                        status = HttpStatusCode.BadRequest,
                        code = ApiErrorCode.VALIDATION_FAILED,
                        message = "The request is invalid.",
                        fieldErrors = listOf(
                            ApiFieldError("email", "REQUIRED", "Email is required.")
                        )
                    )
                    return@post
                }

                call.respondAuthResult(
                    authService.forgotPassword(request.email.trim())
                )
            }

            post("/reset-password") {
                val request = call.receive<ResetPasswordRequest>()
                val validationErrors = buildList {
                    if (request.recoveryToken.isBlank()) {
                        add(
                            ApiFieldError(
                                "recoveryToken",
                                "REQUIRED",
                                "Recovery context is required."
                            )
                        )
                    }
                    if (request.newPassword.isBlank()) {
                        add(
                            ApiFieldError(
                                "newPassword",
                                "REQUIRED",
                                "New password is required."
                            )
                        )
                    }
                }

                if (validationErrors.isNotEmpty()) {
                    call.respondApiError(
                        status = HttpStatusCode.BadRequest,
                        code = ApiErrorCode.VALIDATION_FAILED,
                        message = "The request is invalid.",
                        fieldErrors = validationErrors
                    )
                    return@post
                }

                call.respondAuthResult(
                    authService.resetPassword(
                        recoveryToken = request.recoveryToken,
                        newPassword = request.newPassword
                    )
                )
            }

            post("/session/refresh") {
                val request = call.receive<RefreshSessionRequest>()
                if (request.refreshToken.isBlank()) {
                    call.respondApiError(
                        status = HttpStatusCode.Unauthorized,
                        code = ApiErrorCode.AUTH_SESSION_EXPIRED,
                        message = "The session cannot be refreshed."
                    )
                    return@post
                }

                call.respondAuthResult(
                    authService.refresh(request.refreshToken)
                )
            }

            post("/sign-out") {
                val accessToken = call.bearerTokenOrNull()
                if (accessToken == null) {
                    call.respondApiError(
                        status = HttpStatusCode.Unauthorized,
                        code = ApiErrorCode.AUTH_INVALID_CREDENTIALS,
                        message = "Authentication is required."
                    )
                    return@post
                }

                call.respondAuthResult(authService.signOut(accessToken))
            }
        }
    }
}

private suspend fun <T> ApplicationCall.respondAuthResult(
    result: AuthOperationResult<T>
) {
    when (result) {
        is AuthOperationResult.Success -> {
            when (val value = result.value) {
                Unit -> respond(HttpStatusCode.NoContent)
                is AcceptedResponse -> respond(HttpStatusCode.Accepted, value)
                is SessionResponse -> respond(HttpStatusCode.OK, value)
                else -> error("Unsupported authentication response type")
            }
        }

        AuthOperationResult.InvalidCredentials ->
            respondApiError(
                status = HttpStatusCode.Unauthorized,
                code = ApiErrorCode.AUTH_INVALID_CREDENTIALS,
                message = "The credentials are invalid."
            )

        AuthOperationResult.SessionExpired ->
            respondApiError(
                status = HttpStatusCode.Unauthorized,
                code = ApiErrorCode.AUTH_SESSION_EXPIRED,
                message = "The session has expired or is no longer valid."
            )

        AuthOperationResult.UserDisabled ->
            respondApiError(
                status = HttpStatusCode.Forbidden,
                code = ApiErrorCode.AUTH_USER_DISABLED,
                message = "The user is disabled."
            )

        AuthOperationResult.Forbidden ->
            respondApiError(
                status = HttpStatusCode.Forbidden,
                code = ApiErrorCode.AUTH_FORBIDDEN,
                message = "The authenticated identity is not authorized."
            )

        AuthOperationResult.InvalidRecoveryContext ->
            respondApiError(
                status = HttpStatusCode.Unauthorized,
                code = ApiErrorCode.AUTH_SESSION_EXPIRED,
                message = "The recovery context is invalid or expired."
            )

        AuthOperationResult.ServiceUnavailable ->
            respondApiError(
                status = HttpStatusCode.ServiceUnavailable,
                code = ApiErrorCode.SERVICE_UNAVAILABLE,
                message = "The authentication service is temporarily unavailable.",
                retryable = true
            )
    }
}

private fun ApplicationCall.bearerTokenOrNull(): String? {
    val authorization = request.headers[HttpHeaders.Authorization]
        ?.trim()
        ?: return null

    val parts = authorization.split(' ', limit = 2)
    if (parts.size != 2 || !parts[0].equals("Bearer", ignoreCase = true)) {
        return null
    }

    return parts[1].trim().takeIf { it.isNotEmpty() }
}
