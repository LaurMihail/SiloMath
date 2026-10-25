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

class EntityStateGuardTest {

    @Test
    fun `active campaign state permits guarded operation`() =
        assertStateGuardResult(
            guardResult = EntityStateGuardResult.Allowed,
            expectedStatus = HttpStatusCode.OK
        )

    @Test
    fun `invalid entity state blocks mutation with conflict`() =
        assertStateGuardResult(
            guardResult = EntityStateGuardResult.Conflict,
            expectedStatus = HttpStatusCode.Conflict
        )

    private fun assertStateGuardResult(
        guardResult: EntityStateGuardResult,
        expectedStatus: HttpStatusCode
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
                                        role = OrganizationRole.ADMIN
                                    )
                                )
                            }
                        ) {
                            route("/campaigns/{campaignId}") {
                                requireEntityState(
                                    resolver = EntityStateGuardResolver { context ->
                                        assertEquals("user-1", context.userId)
                                        assertEquals("org-1", context.organizationId)
                                        assertEquals("campaign-1", context.campaignId)
                                        assertEquals(
                                            EntityStateRequirement.CAMPAIGN_ACTIVE,
                                            context.requirement
                                        )
                                        guardResult
                                    },
                                    requirement = EntityStateRequirement.CAMPAIGN_ACTIVE
                                ) {
                                    get("/state-probe") {
                                        call.respondText("OK")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val response = client.get(
            "/organizations/org-1/campaigns/campaign-1/state-probe"
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(expectedStatus, response.status)
    }
}
