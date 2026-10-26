package com.silomath.backend.routes

import com.silomath.backend.api.ApiErrorCode
import com.silomath.backend.api.ApiFieldError
import com.silomath.backend.api.ENTITY_VERSION_HEADER
import com.silomath.backend.api.IDEMPOTENCY_KEY_HEADER
import com.silomath.backend.api.baseEntityVersionOrNull
import com.silomath.backend.api.correlationId
import com.silomath.backend.api.idempotencyKeyOrNull
import com.silomath.backend.api.respondApiError
import com.silomath.backend.location.LocationCreateRequest
import com.silomath.backend.location.LocationListQuery
import com.silomath.backend.location.LocationMutationResult
import com.silomath.backend.location.LocationRuntime
import com.silomath.backend.security.requireAdmin
import com.silomath.backend.security.requireAdminOrStandard
import com.silomath.backend.security.requireAuthenticatedSubject
import com.silomath.backend.security.withActiveBearerAuthentication
import com.silomath.backend.security.withOrganizationAuthorization
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun Application.configureLocationRouting(
    runtime: LocationRuntime
) {
    routing {
        route(
            "/api/v1/organizations/{organizationId}/locations"
        ) {
            withActiveBearerAuthentication(
                runtime.accessTokenVerifier
            ) {
                withOrganizationAuthorization(
                    runtime.organizationAuthorizationResolver
                ) {
                    requireAdminOrStandard {
                        get {
                            val service =
                                runtime.readService

                            if (service == null) {
                                call.respondServiceUnavailable()
                                return@get
                            }

                            val organizationId =
                                call.parameters[
                                    "organizationId"
                                ]!!

                            val page =
                                runCatching {
                                    service.list(
                                        LocationListQuery(
                                            organizationId =
                                                organizationId,
                                            cursor =
                                                call.request
                                                    .queryParameters[
                                                        "cursor"
                                                    ],
                                            search =
                                                call.request
                                                    .queryParameters[
                                                        "search"
                                                    ],
                                            status =
                                                call.request
                                                    .queryParameters[
                                                        "status"
                                                    ]
                                        )
                                    )
                                }.getOrElse {
                                    call.respondValidationError(
                                        it.message
                                            ?: "Invalid Location query."
                                    )
                                    return@get
                                }

                            call.respond(
                                HttpStatusCode.OK,
                                page
                            )
                        }

                        get("/{locationId}") {
                            val service =
                                runtime.readService

                            if (service == null) {
                                call.respondServiceUnavailable()
                                return@get
                            }

                            val organizationId =
                                call.parameters[
                                    "organizationId"
                                ]!!

                            val locationId =
                                call.parameters[
                                    "locationId"
                                ]
                                    ?: run {
                                        call.respondValidationError(
                                            "locationId is required."
                                        )
                                        return@get
                                    }

                            val location =
                                service.get(
                                    organizationId =
                                        organizationId,
                                    locationId =
                                        locationId
                                )

                            if (location == null) {
                                call.respondApiError(
                                    status =
                                        HttpStatusCode.NotFound,
                                    code =
                                        ApiErrorCode
                                            .RESOURCE_NOT_FOUND,
                                    message =
                                        "The requested Location was not found."
                                )
                                return@get
                            }

                            call.respond(
                                HttpStatusCode.OK,
                                location
                            )
                        }
                    }

                    requireAdmin {
                        post {
                            val service =
                                runtime.mutationService

                            if (service == null) {
                                call.respondServiceUnavailable()
                                return@post
                            }

                            val organizationId =
                                call.parameters[
                                    "organizationId"
                                ]!!

                            val request =
                                call.receive<
                                    LocationCreateRequest
                                >()

                            val idempotencyKey =
                                call.idempotencyKeyOrNull()

                            if (idempotencyKey == null) {
                                call.respondRequiredHeader(
                                    IDEMPOTENCY_KEY_HEADER
                                )
                                return@post
                            }

                            if (
                                idempotencyKey !=
                                request.idempotencyKey
                            ) {
                                call.respondHeaderBodyMismatch(
                                    header =
                                        IDEMPOTENCY_KEY_HEADER,
                                    bodyField =
                                        "idempotencyKey"
                                )
                                return@post
                            }

                            val validationErrors =
                                validateCreateRequest(
                                    request
                                )

                            if (
                                validationErrors
                                    .isNotEmpty()
                            ) {
                                call.respondApiError(
                                    status =
                                        HttpStatusCode
                                            .UnprocessableEntity,
                                    code =
                                        ApiErrorCode
                                            .VALIDATION_FAILED,
                                    message =
                                        "The Location request is invalid.",
                                    fieldErrors =
                                        validationErrors
                                )
                                return@post
                            }

                            val subject =
                                call.requireAuthenticatedSubject()

                            val result =
                                runCatching {
                                    service.create(
                                        organizationId =
                                            organizationId,
                                        actorUserId =
                                            subject.userId,
                                        correlationId =
                                            call.correlationId(),
                                        request =
                                            request
                                    )
                                }.getOrElse {
                                    call.respondValidationError(
                                        it.message
                                            ?: "Invalid Location request."
                                    )
                                    return@post
                                }

                            call.respondMutationResult(
                                result = result,
                                successStatus =
                                    HttpStatusCode.Created
                            )
                        }

                        patch("/{locationId}") {
                            val service =
                                runtime.mutationService

                            if (service == null) {
                                call.respondServiceUnavailable()
                                return@patch
                            }

                            val organizationId =
                                call.parameters[
                                    "organizationId"
                                ]!!

                            val locationId =
                                call.parameters[
                                    "locationId"
                                ]
                                    ?: run {
                                        call.respondValidationError(
                                            "locationId is required."
                                        )
                                        return@patch
                                    }

                            val requestJson =
                                call.receive<JsonObject>()

                            val idempotencyKey =
                                call.idempotencyKeyOrNull()

                            if (idempotencyKey == null) {
                                call.respondRequiredHeader(
                                    IDEMPOTENCY_KEY_HEADER
                                )
                                return@patch
                            }

                            val bodyIdempotencyKey =
                                requestJson["idempotencyKey"]
                                    ?.let { it as? JsonPrimitive }
                                    ?.takeIf { it.isString }
                                    ?.content
                                    ?.takeIf { it.isNotBlank() }

                            if (
                                bodyIdempotencyKey == null ||
                                bodyIdempotencyKey != idempotencyKey
                            ) {
                                call.respondHeaderBodyMismatch(
                                    header =
                                        IDEMPOTENCY_KEY_HEADER,
                                    bodyField =
                                        "idempotencyKey"
                                )
                                return@patch
                            }

                            val headerBaseEntityVersion =
                                call.baseEntityVersionOrNull()

                            if (
                                headerBaseEntityVersion == null ||
                                headerBaseEntityVersion < 1L
                            ) {
                                call.respondRequiredHeader(
                                    ENTITY_VERSION_HEADER
                                )
                                return@patch
                            }

                            val bodyBaseEntityVersion =
                                requestJson["baseEntityVersion"]
                                    ?.let { it as? JsonPrimitive }
                                    ?.content
                                    ?.toLongOrNull()

                            if (
                                bodyBaseEntityVersion == null ||
                                bodyBaseEntityVersion < 1L ||
                                bodyBaseEntityVersion !=
                                headerBaseEntityVersion
                            ) {
                                call.respondHeaderBodyMismatch(
                                    header =
                                        ENTITY_VERSION_HEADER,
                                    bodyField =
                                        "baseEntityVersion"
                                )
                                return@patch
                            }

                            val baseEntityVersion =
                                headerBaseEntityVersion

                            val subject =
                                call.requireAuthenticatedSubject()

                            val result =
                                runCatching {
                                    service.patch(
                                        organizationId =
                                            organizationId,
                                        locationId =
                                            locationId,
                                        actorUserId =
                                            subject.userId,
                                        correlationId =
                                            call.correlationId(),
                                        baseEntityVersion =
                                            baseEntityVersion,
                                        requestJson =
                                            requestJson
                                    )
                                }.getOrElse {
                                    call.respondValidationError(
                                        it.message
                                            ?: "Invalid Location PATCH request."
                                    )
                                    return@patch
                                }

                            call.respondMutationResult(
                                result = result,
                                successStatus =
                                    HttpStatusCode.OK
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun validateCreateRequest(
    request: LocationCreateRequest
): List<ApiFieldError> =
    buildList {
        val canonicalName =
            request.name.trim()

        val canonicalCode =
            request.code
                .trim()
                .uppercase()

        if (canonicalName.isBlank()) {
            add(
                ApiFieldError(
                    field = "name",
                    code = "REQUIRED",
                    message = "Required"
                )
            )
        }

        if (canonicalName.length > 150) {
            add(
                ApiFieldError(
                    field = "name",
                    code = "TOO_LONG",
                    message =
                        "Must be at most 150 characters."
                )
            )
        }

        if (canonicalCode.isBlank()) {
            add(
                ApiFieldError(
                    field = "code",
                    code = "REQUIRED",
                    message = "Required"
                )
            )
        }

        if (canonicalCode.length > 50) {
            add(
                ApiFieldError(
                    field = "code",
                    code = "TOO_LONG",
                    message =
                        "Must be at most 50 characters."
                )
            )
        }

        if (
            request.address != null &&
            request.address.length > 250
        ) {
            add(
                ApiFieldError(
                    field = "address",
                    code = "TOO_LONG",
                    message =
                        "Must be at most 250 characters."
                )
            )
        }

        if (
            request.city != null &&
            request.city.length > 100
        ) {
            add(
                ApiFieldError(
                    field = "city",
                    code = "TOO_LONG",
                    message =
                        "Must be at most 100 characters."
                )
            )
        }

        if (
            request.notes != null &&
            request.notes.length > 500
        ) {
            add(
                ApiFieldError(
                    field = "notes",
                    code = "TOO_LONG",
                    message =
                        "Must be at most 500 characters."
                )
            )
        }

        if (
            request.lifecycleStatus != "ACTIVE" &&
            request.lifecycleStatus != "INACTIVE"
        ) {
            add(
                ApiFieldError(
                    field =
                        "lifecycleStatus",
                    code =
                        "INVALID",
                    message =
                        "Must be ACTIVE or INACTIVE."
                )
            )
        }

        if (request.idempotencyKey.isBlank()) {
            add(
                ApiFieldError(
                    field =
                        "idempotencyKey",
                    code =
                        "REQUIRED",
                    message =
                        "Required"
                )
            )
        }

        if (request.clientOperationId.isBlank()) {
            add(
                ApiFieldError(
                    field =
                        "clientOperationId",
                    code =
                        "REQUIRED",
                    message =
                        "Required"
                )
            )
        }
    }

private suspend fun io.ktor.server.application.ApplicationCall
    .respondMutationResult(
        result: LocationMutationResult,
        successStatus: HttpStatusCode
    ) {
    when (result) {
        is LocationMutationResult.Success ->
            respond(
                successStatus,
                result.location
            )

        is LocationMutationResult.EntityVersionConflict ->
            respondApiError(
                status =
                    HttpStatusCode.Conflict,
                code =
                    ApiErrorCode
                        .ENTITY_VERSION_CONFLICT,
                message =
                    "The Location has changed since the supplied base version."
            )

        LocationMutationResult.IdempotencyMismatch ->
            respondApiError(
                status =
                    HttpStatusCode.Conflict,
                code =
                    ApiErrorCode
                        .IDEMPOTENCY_KEY_REUSE_MISMATCH,
                message =
                    "The idempotency key was reused for a different logical request."
            )

        LocationMutationResult.NotFound ->
            respondApiError(
                status =
                    HttpStatusCode.NotFound,
                code =
                    ApiErrorCode
                        .RESOURCE_NOT_FOUND,
                message =
                    "The requested Location was not found."
            )
    }
}

private suspend fun io.ktor.server.application.ApplicationCall
    .respondValidationError(
        message: String
    ) {
    respondApiError(
        status =
            HttpStatusCode.UnprocessableEntity,
        code =
            ApiErrorCode.VALIDATION_FAILED,
        message =
            message
    )
}

private suspend fun io.ktor.server.application.ApplicationCall
    .respondRequiredHeader(
        header: String
    ) {
    respondApiError(
        status = HttpStatusCode.UnprocessableEntity,
        code = ApiErrorCode.VALIDATION_FAILED,
        message = "$header is required.",
        fieldErrors =
            listOf(
                ApiFieldError(
                    field = header,
                    code = "REQUIRED",
                    message = "Required"
                )
            )
    )
}

private suspend fun io.ktor.server.application.ApplicationCall
    .respondHeaderBodyMismatch(
        header: String,
        bodyField: String
    ) {
    respondApiError(
        status = HttpStatusCode.UnprocessableEntity,
        code = ApiErrorCode.VALIDATION_FAILED,
        message = "$header must match $bodyField.",
        fieldErrors =
            listOf(
                ApiFieldError(
                    field = header,
                    code = "MISMATCH",
                    message = "Must match $bodyField"
                )
            )
    )
}

private suspend fun io.ktor.server.application.ApplicationCall
    .respondServiceUnavailable() {
    respondApiError(
        status =
            HttpStatusCode.ServiceUnavailable,
        code =
            ApiErrorCode.SERVICE_UNAVAILABLE,
        message =
            "The Location service is temporarily unavailable.",
        retryable =
            true
    )
}
