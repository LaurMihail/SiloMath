package com.silomath.backend.offline

import com.silomath.backend.api.configureApiSerialization
import com.silomath.backend.routes.configureOfflinePackageRouting
import com.silomath.backend.security.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.*
import kotlin.test.*

class OfflinePackageRoutesTest {
    private val org="00000000-0000-0000-0000-000000000001"
    private val campaign="00000000-0000-0000-0000-000000000002"
    private val device="00000000-0000-0000-0000-000000000003"
    private val manifest="00000000-0000-0000-0000-000000000004"
    private val path get()="/api/v1/organizations/$org/campaigns/$campaign/offline-package?deviceInstallationId=$device&manifestId=$manifest&packageVersion=v1"
    private val verifier=AccessTokenVerifier { AccessTokenVerificationResult.Authenticated(AuthenticatedSubject("user-1")) }
    private val resolver=OrganizationAuthorizationResolver { _,o -> OrganizationAuthorizationResult.Authorized(AuthorizedOrganizationMembership("m",o,OrganizationRole.ADMIN)) }

    @Test fun `authorized exact version returns identical binary and integrity headers`() = testApplication {
        val bytes=byteArrayOf(0,1,2,3,-1)
        val metadata=OfflineManifestResponse(manifest,org,campaign,device,"v1",buildJsonObject {},OfflinePackageEncoder.sha256(bytes),"SHA-256",null,"2026-10-27T00:00:00Z")
        application {
            configureApiSerialization()
            configureOfflinePackageRouting(verifier,resolver,OfflinePackageService { user,o,c,d,v,m ->
                assertEquals(listOf("user-1",org,campaign,device,"v1",manifest),listOf(user,o,c,d,v,m))
                OfflinePackageResult.Found(MaterializedOfflinePackage(metadata,bytes))
            })
        }
        val response=client.get(path) { header("Authorization","Bearer test") }
        assertEquals(HttpStatusCode.OK,response.status)
        assertEquals(metadata.integrityHash,response.headers["X-Package-SHA256"])
        assertEquals("private, no-store",response.headers["Cache-Control"])
        assertContentEquals(bytes,response.body<ByteArray>())
    }

    @Test fun `missing exact manifest is rejected before service`() = testApplication {
        var calls=0
        application { configureOfflinePackageRouting(verifier,resolver,OfflinePackageService { _,_,_,_,_,_ -> calls++;OfflinePackageResult.NotFound }) }
        val response=client.get(path.replace("manifestId=$manifest&","")) { header("Authorization","Bearer test") }
        assertEquals(HttpStatusCode.BadRequest,response.status)
        assertEquals(0,calls)
    }

    @Test fun `storage dependency failure is retryable 503`() = testApplication {
        application { configureOfflinePackageRouting(verifier,resolver,OfflinePackageService { _,_,_,_,_,_ -> throw java.io.IOException("Storage offline") }) }
        val response=client.get(path) { header("Authorization","Bearer test") }
        assertEquals(HttpStatusCode.ServiceUnavailable,response.status)
        val body=Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("SERVICE_UNAVAILABLE",body.getValue("code").jsonPrimitive.content)
        assertEquals("true",body.getValue("retryable").jsonPrimitive.content)
    }

    @Test fun `campaign access denial returns forbidden`() = testApplication {
        application { configureOfflinePackageRouting(verifier,resolver,OfflinePackageService { _,_,_,_,_,_ -> OfflinePackageResult.Forbidden }) }
        val response=client.get(path) { header("Authorization","Bearer test") }
        assertEquals(HttpStatusCode.Forbidden,response.status)
    }
}
