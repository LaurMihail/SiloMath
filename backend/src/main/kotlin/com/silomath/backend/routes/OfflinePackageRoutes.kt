package com.silomath.backend.routes

import com.silomath.backend.api.ApiErrorCode
import com.silomath.backend.api.respondApiError
import com.silomath.backend.offline.OfflinePackageService
import com.silomath.backend.offline.OfflinePackageResult
import com.silomath.backend.security.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.*
import java.util.UUID
import kotlinx.coroutines.CancellationException

fun Application.configureOfflinePackageRouting(
    verifier: AccessTokenVerifier,
    organizationResolver: OrganizationAuthorizationResolver,
    service: OfflinePackageService?
) {
    routing {
        route("/api/v1/organizations/{organizationId}/campaigns/{campaignId}/offline-package") {
            withActiveBearerAuthentication(verifier) {
                withOrganizationAuthorization(organizationResolver) {
                    get {
                        val org=call.parameters["organizationId"]!!
                        val campaign=call.parameters["campaignId"]!!
                        val device=call.request.queryParameters["deviceInstallationId"]
                        val manifest=call.request.queryParameters["manifestId"]
                        val version=call.request.queryParameters["packageVersion"]
                        if (listOf(org,campaign,device,manifest).any { value -> value == null ||
                                runCatching { UUID.fromString(value).toString()!=value.lowercase() }.getOrDefault(true) } ||
                            version.isNullOrBlank() || version.length>256) {
                            call.respondApiError(HttpStatusCode.BadRequest,ApiErrorCode.VALIDATION_FAILED,
                                "An exact manifest, package version and device installation are required.")
                            return@get
                        }
                        try {
                            if (service==null) {
                                call.respondApiError(HttpStatusCode.ServiceUnavailable,ApiErrorCode.SERVICE_UNAVAILABLE,
                                    "Offline package storage is not configured.",retryable=true)
                                return@get
                            }
                            when(val result=service.download(call.requireAuthenticatedSubject().userId,org,campaign,device!!,version,manifest!!)) {
                                OfflinePackageResult.Forbidden -> call.respondApiError(HttpStatusCode.Forbidden,ApiErrorCode.AUTH_FORBIDDEN,"Campaign or installation access is denied.")
                                OfflinePackageResult.NotFound -> call.respondApiError(HttpStatusCode.NotFound,ApiErrorCode.RESOURCE_NOT_FOUND,"No current package matches this manifest.")
                                is OfflinePackageResult.Found -> {
                                    val artifact=result.artifact
                                    call.response.headers.append("Cache-Control","private, no-store")
                                    call.response.headers.append("X-Package-SHA256",artifact.manifest.integrityHash)
                                    call.response.headers.append("X-Package-Version",artifact.manifest.packageVersion)
                                    call.response.headers.append("X-Manifest-Id",artifact.manifest.manifestId)
                                    call.respondBytes(artifact.bytes,ContentType.Application.OctetStream)
                                }
                            }
                        } catch(cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            call.respondApiError(HttpStatusCode.ServiceUnavailable,ApiErrorCode.SERVICE_UNAVAILABLE,
                                "Offline package dependency is unavailable.",retryable=true)
                        }
                    }
                }
            }
        }
    }
}
