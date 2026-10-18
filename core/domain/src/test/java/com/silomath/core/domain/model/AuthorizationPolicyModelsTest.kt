package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.UserId
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorizationPolicyModelsTest {

    private val userId = UserId("11111111-1111-1111-1111-111111111111")
    private val organizationId = OrganizationId("22222222-2222-2222-2222-222222222222")
    private val otherOrganizationId = OrganizationId("33333333-3333-3333-3333-333333333333")
    private val campaignId = CampaignId("44444444-4444-4444-4444-444444444444")

    private fun context(
        role: OrganizationRole = OrganizationRole.STANDARD,
        userStatus: UserStatus = UserStatus.ACTIVE,
        membershipStatus: MembershipStatus = MembershipStatus.ACTIVE,
        resourceOrganizationId: OrganizationId = organizationId,
        campaignStatus: CampaignStatus? = CampaignStatus.ACTIVE,
        campaignAccessAuthorized: Boolean = true,
        campaignTeamMember: Boolean = false,
        connectivity: ConnectivityState = ConnectivityState.ONLINE,
        offlineReady: Boolean = false,
        closeGuardsSatisfied: Boolean = false,
        includeCampaign: Boolean = true
    ) = AuthorizationContext(
        userId = userId,
        userStatus = userStatus,
        membershipOrganizationId = organizationId,
        membershipStatus = membershipStatus,
        role = role,
        resourceOrganizationId = resourceOrganizationId,
        campaignId = if (includeCampaign) campaignId else null,
        campaignStatus = if (includeCampaign) campaignStatus else null,
        campaignAccessAuthorized = campaignAccessAuthorized,
        campaignTeamMember = campaignTeamMember,
        connectivity = connectivity,
        offlineReady = offlineReady,
        closeGuardsSatisfied = closeGuardsSatisfied
    )

    @Test
    fun adminMayMutateCustomerManagedMasterDataAndStandardMayNot() {
        assertEquals(
            AuthorizationDecision.Allowed,
            AuthorizationPolicy.authorize(
                AuthorizationOperation.MASTER_DATA_MUTATE,
                context(role = OrganizationRole.ADMIN, includeCampaign = false)
            )
        )
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.ADMIN_REQUIRED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.MASTER_DATA_MUTATE,
                context(role = OrganizationRole.STANDARD, includeCampaign = false)
            )
        )
    }

    @Test
    fun stocktakingMutationRequiresCampaignTeamMembershipForBothRoles() {
        assertEquals(
            AuthorizationDecision.Denied(
                AuthorizationDenialReason.CAMPAIGN_TEAM_MEMBERSHIP_REQUIRED
            ),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.STOCKTAKING_MUTATE,
                context(role = OrganizationRole.ADMIN, campaignTeamMember = false)
            )
        )
        assertEquals(
            AuthorizationDecision.Allowed,
            AuthorizationPolicy.authorize(
                AuthorizationOperation.STOCKTAKING_MUTATE,
                context(role = OrganizationRole.STANDARD, campaignTeamMember = true)
            )
        )
    }

    @Test
    fun offlineStocktakingRequiresVerifiedOfflineWorkingSet() {
        assertEquals(
            AuthorizationDecision.Denied(
                AuthorizationDenialReason.OFFLINE_WORKING_SET_REQUIRED
            ),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.STOCKTAKING_MUTATE,
                context(
                    campaignTeamMember = true,
                    connectivity = ConnectivityState.OFFLINE,
                    offlineReady = false
                )
            )
        )
        assertEquals(
            AuthorizationDecision.Allowed,
            AuthorizationPolicy.authorize(
                AuthorizationOperation.STOCKTAKING_MUTATE,
                context(
                    campaignTeamMember = true,
                    connectivity = ConnectivityState.OFFLINE,
                    offlineReady = true
                )
            )
        )
    }

    @Test
    fun inventoryCloseIsAdminOnlyOnlineAndGuarded() {
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.ADMIN_REQUIRED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.INVENTORY_CLOSE,
                context(role = OrganizationRole.STANDARD, closeGuardsSatisfied = true)
            )
        )
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.ONLINE_REQUIRED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.INVENTORY_CLOSE,
                context(
                    role = OrganizationRole.ADMIN,
                    connectivity = ConnectivityState.OFFLINE,
                    closeGuardsSatisfied = true
                )
            )
        )
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.CLOSE_GUARDS_FAILED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.INVENTORY_CLOSE,
                context(role = OrganizationRole.ADMIN, closeGuardsSatisfied = false)
            )
        )
        assertEquals(
            AuthorizationDecision.Allowed,
            AuthorizationPolicy.authorize(
                AuthorizationOperation.INVENTORY_CLOSE,
                context(role = OrganizationRole.ADMIN, closeGuardsSatisfied = true)
            )
        )
    }

    @Test
    fun standardCannotViewAuditTrailButCanViewAuthorizedReports() {
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.ADMIN_REQUIRED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.AUDIT_TRAIL_VIEW,
                context(role = OrganizationRole.STANDARD, includeCampaign = false)
            )
        )
        assertEquals(
            AuthorizationDecision.Allowed,
            AuthorizationPolicy.authorize(
                AuthorizationOperation.REPORT_VIEW,
                context(role = OrganizationRole.STANDARD, campaignAccessAuthorized = true)
            )
        )
    }

    @Test
    fun crossOrganizationAndDisabledPrincipalsAreRejectedBeforeRoleRules() {
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.CROSS_ORGANIZATION),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.REPORT_VIEW,
                context(resourceOrganizationId = otherOrganizationId, includeCampaign = false)
            )
        )
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.USER_DISABLED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.REPORT_VIEW,
                context(userStatus = UserStatus.DISABLED, includeCampaign = false)
            )
        )
    }

    @Test
    fun completedCampaignRejectsOperationalMutation() {
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.ACTIVE_CAMPAIGN_REQUIRED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.ISSUE_REPORT,
                context(
                    campaignStatus = CampaignStatus.COMPLETED,
                    campaignTeamMember = true
                )
            )
        )
    }

    @Test
    fun organizationUsersCannotMutatePlatformGovernedOrSystemControlledRules() {
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.PLATFORM_GOVERNED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.PLATFORM_GOVERNED_MUTATE,
                context(role = OrganizationRole.ADMIN, includeCampaign = false)
            )
        )
        assertEquals(
            AuthorizationDecision.Denied(AuthorizationDenialReason.SYSTEM_CONTROLLED),
            AuthorizationPolicy.authorize(
                AuthorizationOperation.SYSTEM_RULE_MUTATE,
                context(role = OrganizationRole.ADMIN, includeCampaign = false)
            )
        )
    }
}
