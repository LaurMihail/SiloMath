package com.silomath.backend.routes

import com.silomath.backend.api.ApiErrorCode
import com.silomath.backend.api.respondApiError
import com.silomath.backend.security.AccessTokenVerifier
import com.silomath.backend.security.OrganizationAuthorizationResolver
import com.silomath.backend.offline.OfflineManifestResult
import com.silomath.backend.offline.OfflineManifestService
import com.silomath.backend.security.requireAuthenticatedSubject
import com.silomath.backend.security.withActiveBearerAuthentication
import com.silomath.backend.security.withOrganizationAuthorization
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID
import kotlinx.coroutines.CancellationException

fun Application.configureOfflineManifestRouting(
    accessTokenVerifier: AccessTokenVerifier,
    organizationAuthorizationResolver: OrganizationAuthorizationResolver,
    service: OfflineManifestService?
) {
    routing {
        route("/api/v1/organizations/{organizationId}/campaigns/{campaignId}/offline-manifest") {
            withActiveBearerAuthentication(accessTokenVerifier) {
                withOrganizationAuthorization(organizationAuthorizationResolver) {
                    get {
                        val organizationId = call.parameters["organizationId"]!!
                        val campaignId = call.parameters["campaignId"]!!
                        val deviceId = call.request.queryParameters["deviceInstallationId"]
                        val version = call.request.queryParameters["packageVersion"]
                        if (listOf(organizationId, campaignId, deviceId).any { !isCanonicalUuid(it) } ||
                            version != null && (version.isBlank() || version.length > 256)) {
                            call.respondApiError(
                                HttpStatusCode.BadRequest, ApiErrorCode.VALIDATION_FAILED,
                                "Valid organization, campaign and device installation UUIDs are required."
                            )
                            return@get
                        }
                        try {
                            if (service == null) {
                                call.respondApiError(
                                    HttpStatusCode.ServiceUnavailable, ApiErrorCode.SERVICE_UNAVAILABLE,
                                    "Offline manifest service is unavailable.", retryable = true
                                )
                                return@get
                            }
                            when (val result = service.current(
                                call.requireAuthenticatedSubject().userId,
                                organizationId, campaignId, deviceId!!, version
                            )) {
                                is OfflineManifestResult.Found -> call.respond(result.manifest)
                                OfflineManifestResult.Forbidden -> call.respondApiError(
                                    HttpStatusCode.Forbidden, ApiErrorCode.AUTH_FORBIDDEN,
                                    "Campaign access is required."
                                )
                                OfflineManifestResult.NotFound -> call.respondApiError(
                                    HttpStatusCode.NotFound, ApiErrorCode.RESOURCE_NOT_FOUND,
                                    "No current authorized manifest matches this installation and version."
                                )
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            call.respondApiError(
                                HttpStatusCode.ServiceUnavailable, ApiErrorCode.SERVICE_UNAVAILABLE,
                                "Offline manifest dependency is unavailable.", retryable = true
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isCanonicalUuid(value: String?): Boolean =
    value != null && runCatching { UUID.fromString(value).toString() == value.lowercase() }.getOrDefault(false)
