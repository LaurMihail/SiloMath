package com.silomath.backend.security

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class RoleAuthorizationTest {

    @Test
    fun `admin guard permits admin membership`() =
        assertRoleResult(
            role = OrganizationRole.ADMIN,
            expected = HttpStatusCode.OK
        )

    @Test
    fun `admin guard rejects standard membership`() =
        assertRoleResult(
            role = OrganizationRole.STANDARD,
            expected = HttpStatusCode.Forbidden
        )

    private fun assertRoleResult(
        role: OrganizationRole,
        expected: HttpStatusCode
    ) = testApplication {
        application {
            routing {
                withActiveBearerAuthentication(
                    verifier = AccessTokenVerifier {
                        AccessTokenVerificationResult.Authenticated(
                            AuthenticatedSubject("user-1")
                        )
                    }
                ) {
                    route("/organizations/{organizationId}") {
                        withOrganizationAuthorization(
                            resolver = OrganizationAuthorizationResolver { _, organizationId ->
                                OrganizationAuthorizationResult.Authorized(
                                    AuthorizedOrganizationMembership(
                                        membershipId = "membership-1",
                                        organizationId = organizationId,
                                        role = role
                                    )
                                )
                            }
                        ) {
                            requireAdmin {
                                get {
                                    call.respondText("OK")
                                }
                            }
                        }
                    }
                }
            }
        }

        val response = client.get("/organizations/org-1") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(expected, response.status)
    }
}
