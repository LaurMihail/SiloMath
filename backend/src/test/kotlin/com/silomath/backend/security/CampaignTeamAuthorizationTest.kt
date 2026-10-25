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

class CampaignTeamAuthorizationTest {

    @Test
    fun `admin without campaign team membership is forbidden for guarded mutation`() =
        assertTeamGuard(
            role = OrganizationRole.ADMIN,
            teamResult = CampaignTeamAuthorizationResult.Forbidden,
            expected = HttpStatusCode.Forbidden
        )

    @Test
    fun `standard with campaign team membership is allowed for guarded mutation`() =
        assertTeamGuard(
            role = OrganizationRole.STANDARD,
            teamResult = CampaignTeamAuthorizationResult.Authorized,
            expected = HttpStatusCode.OK
        )

    private fun assertTeamGuard(
        role: OrganizationRole,
        teamResult: CampaignTeamAuthorizationResult,
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
                            route("/campaigns/{campaignId}") {
                                requireCampaignTeamMutationAccess(
                                    resolver = CampaignTeamAuthorizationResolver {
                                            userId,
                                            organizationId,
                                            campaignId ->
                                        assertEquals("user-1", userId)
                                        assertEquals("org-1", organizationId)
                                        assertEquals("campaign-1", campaignId)
                                        teamResult
                                    }
                                ) {
                                    get("/mutation-probe") {
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
            "/organizations/org-1/campaigns/campaign-1/mutation-probe"
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(expected, response.status)
    }
}
