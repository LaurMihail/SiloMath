package com.silomath.backend.security

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationAuthorizationTest {

    @Test
    fun `active membership in requested organization authorizes tenant context`() =
        testApplication {
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
                                resolver = OrganizationAuthorizationResolver {
                                        userId,
                                        organizationId ->
                                    assertEquals("user-1", userId)
                                    OrganizationAuthorizationResult.Authorized(
                                        AuthorizedOrganizationMembership(
                                            membershipId = "membership-1",
                                            organizationId = organizationId,
                                            role = OrganizationRole.STANDARD
                                        )
                                    )
                                }
                            ) {
                                get {
                                    call.respondText(
                                        call
                                            .requireAuthorizedOrganizationMembership()
                                            .organizationId
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val response = client.get("/organizations/org-1") {
                header(HttpHeaders.Authorization, "Bearer valid")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("org-1", response.bodyAsText())
        }

    @Test
    fun `missing or inactive organization membership is forbidden`() =
        testApplication {
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
                                resolver = OrganizationAuthorizationResolver { _, _ ->
                                    OrganizationAuthorizationResult.Forbidden
                                }
                            ) {
                                get {
                                    call.respondText("should-not-run")
                                }
                            }
                        }
                    }
                }
            }

            val response = client.get("/organizations/other-org") {
                header(HttpHeaders.Authorization, "Bearer valid")
                header("X-Organization-Id", "other-org")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
}
