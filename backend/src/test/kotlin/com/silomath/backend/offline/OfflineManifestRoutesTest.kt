package com.silomath.backend.offline

import com.silomath.backend.api.configureApiSerialization
import com.silomath.backend.routes.configureOfflineManifestRouting
import com.silomath.backend.security.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.sql.SQLException
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

class OfflineManifestRoutesTest {
    private val organizationId = "00000000-0000-0000-0000-000000000001"
    private val campaignId = "00000000-0000-0000-0000-000000000002"
    private val deviceId = "00000000-0000-0000-0000-000000000003"
    private val path get() = "/api/v1/organizations/$organizationId/campaigns/$campaignId/offline-manifest"

    @Test fun `authorized manifest preserves complete identity and metadata`() = testApplication {
        val manifest = OfflineManifestResponse(
            "00000000-0000-0000-0000-000000000004", organizationId, campaignId, deviceId,
            "version-1", buildJsonObject { put("locations", 2) }, "a".repeat(64), "SHA-256",
            buildJsonObject { put("calculationSchemaVersion", "v1") }, "2026-10-27T00:00:00Z"
        )
        application {
            configureApiSerialization()
            configureOfflineManifestRouting(verifier, membership, OfflineManifestService { user, org, campaign, device, version ->
                assertEquals("user-1", user)
                assertEquals(organizationId, org)
                assertEquals(campaignId, campaign)
                assertEquals(deviceId, device)
                assertEquals("version-1", version)
                OfflineManifestResult.Found(manifest)
            })
        }
        val response = client.get("$path?deviceInstallationId=$deviceId&packageVersion=version-1") {
            header("Authorization", "Bearer test-token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(deviceId, body.getValue("deviceInstallationId").jsonPrimitive.content)
        assertEquals("a".repeat(64), body.getValue("integrityHash").jsonPrimitive.content)
        assertEquals("2", body.getValue("contentManifest").jsonObject.getValue("locations").jsonPrimitive.content)
        assertEquals(false, body.containsKey("offlineReady"))
    }

    @Test fun `missing bearer never calls manifest service`() = errorCase(HttpStatusCode.Unauthorized, bearer = false)
    @Test fun `tenant denial never calls manifest service`() = errorCase(HttpStatusCode.Forbidden, tenantDenied = true)
    @Test fun `missing device rejects request`() = errorCase(HttpStatusCode.BadRequest, query = "")
    @Test fun `malformed device rejects request`() = errorCase(HttpStatusCode.BadRequest, query = "?deviceInstallationId=abc")
    @Test fun `campaign access denial is forbidden`() = errorCase(HttpStatusCode.Forbidden, result = OfflineManifestResult.Forbidden)
    @Test fun `missing current version returns not found`() = errorCase(HttpStatusCode.NotFound)
    @Test fun `dependency failure is retryable 503`() = errorCase(HttpStatusCode.ServiceUnavailable, failure = true)

    private val verifier = AccessTokenVerifier {
        AccessTokenVerificationResult.Authenticated(AuthenticatedSubject("user-1"))
    }
    private val membership = OrganizationAuthorizationResolver { _, org ->
        OrganizationAuthorizationResult.Authorized(AuthorizedOrganizationMembership("membership-1", org, OrganizationRole.STANDARD))
    }

    private fun errorCase(
        expected: HttpStatusCode,
        bearer: Boolean = true,
        tenantDenied: Boolean = false,
        query: String = "?deviceInstallationId=$deviceId",
        result: OfflineManifestResult = OfflineManifestResult.NotFound,
        failure: Boolean = false
    ) = testApplication {
        var calls = 0
        application {
            configureApiSerialization()
            configureOfflineManifestRouting(
                verifier,
                if (tenantDenied) OrganizationAuthorizationResolver { _, _ -> OrganizationAuthorizationResult.Forbidden } else membership,
                OfflineManifestService { _, _, _, _, _ ->
                    calls++
                    if (failure) throw SQLException("Dependency unavailable")
                    result
                }
            )
        }
        val response = client.get(path + query) {
            if (bearer) header("Authorization", "Bearer test-token")
        }
        assertEquals(expected, response.status)
        assertEquals(if (!bearer || tenantDenied || expected == HttpStatusCode.BadRequest) 0 else 1, calls)
        if (failure) {
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals("SERVICE_UNAVAILABLE", body.getValue("code").jsonPrimitive.content)
            assertEquals("true", body.getValue("retryable").jsonPrimitive.content)
        }
    }
}
