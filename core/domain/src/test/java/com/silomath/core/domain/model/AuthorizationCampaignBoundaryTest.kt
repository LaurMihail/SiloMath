package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.UserId
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorizationCampaignBoundaryTest {

    private val userId = UserId("11111111-1111-4111-8111-111111111111")
    private val organizationId = OrganizationId("22222222-2222-4222-8222-222222222222")
    private val campaignId = CampaignId("33333333-3333-4333-8333-333333333333")

    @Test
    fun `campaign scope is manageable only before launch`() {
        assertEquals(
            AuthorizationDecision.Allowed,
            AuthorizationPolicy.authorize(
                AuthorizationOperation.CAMPAIGN_SCOPE_MANAGE,
                context(includeCampaign = false)
            )
        )
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.CAMPAIGN_SCOPE_IMMUTABLE),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.CAMPAIGN_SCOPE_MANAGE,
                context(includeCampaign = true, campaignStatus = CampaignStatus.ACTIVE)
            )
        )
    }

    @Test
    fun `campaign team is editable before launch and while launched campaign is active`() {
        assertEquals(
            AuthorizationDecision.Allowed,
            AuthorizationPolicy.authorize(
                AuthorizationOperation.CAMPAIGN_TEAM_ASSIGN,
                context(includeCampaign = false)
            )
        )
        assertEquals(
            AuthorizationDecision.Allowed,
            AuthorizationPolicy.authorize(
                AuthorizationOperation.CAMPAIGN_TEAM_ASSIGN,
                context(includeCampaign = true, campaignStatus = CampaignStatus.ACTIVE)
            )
        )
    }

    @Test
    fun `campaign team mutation is rejected after campaign completion`() {
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.ACTIVE_CAMPAIGN_REQUIRED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.CAMPAIGN_TEAM_ASSIGN,
                context(includeCampaign = true, campaignStatus = CampaignStatus.COMPLETED)
            )
        )
    }

    @Test
    fun `campaign administration mutations remain online only`() {
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.ONLINE_REQUIRED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.CAMPAIGN_TEAM_ASSIGN,
                context(
                    includeCampaign = true,
                    campaignStatus = CampaignStatus.ACTIVE,
                    connectivity = ConnectivityState.OFFLINE
                )
            )
        )
    }

    private fun context(
        includeCampaign: Boolean,
        campaignStatus: CampaignStatus? = CampaignStatus.ACTIVE,
        connectivity: ConnectivityState = ConnectivityState.ONLINE
    ) = AuthorizationContext(
        userId = userId,
        userStatus = UserStatus.ACTIVE,
        membershipOrganizationId = organizationId,
        membershipStatus = MembershipStatus.ACTIVE,
        role = OrganizationRole.ADMIN,
        resourceOrganizationId = organizationId,
        campaignId = if (includeCampaign) campaignId else null,
        campaignStatus = if (includeCampaign) campaignStatus else null,
        campaignAccessAuthorized = includeCampaign,
        campaignTeamMember = false,
        connectivity = connectivity,
        offlineReady = false,
        closeGuardsSatisfied = false
    )
}
