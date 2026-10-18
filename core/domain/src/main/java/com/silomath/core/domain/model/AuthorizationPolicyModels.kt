package com.silomath.core.domain.model

import com.silomath.core.model.CampaignId
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.UserId

enum class ConnectivityState {
    ONLINE,
    OFFLINE
}

enum class AuthorizationOperation {
    MASTER_DATA_MUTATE,
    CAMPAIGN_CREATE_LAUNCH,
    CAMPAIGN_SCOPE_MANAGE,
    CAMPAIGN_TEAM_ASSIGN,
    STOCKTAKING_MUTATE,
    ISSUE_REPORT,
    ISSUE_RESOLVE,
    CLOSE_FOLLOW_UP_ACCEPT,
    INVENTORY_CLOSE,
    REPORT_VIEW,
    AUDIT_TRAIL_VIEW,
    USER_ADMIN,
    ORGANIZATION_CONFIG_MUTATE,
    PLATFORM_GOVERNED_MUTATE,
    SYSTEM_RULE_MUTATE,
    NOTIFICATION_READ,
    SYNC_STATUS_VIEW
}

enum class AuthorizationDenialReason {
    USER_DISABLED,
    MEMBERSHIP_DISABLED,
    CROSS_ORGANIZATION,
    ADMIN_REQUIRED,
    CAMPAIGN_CONTEXT_REQUIRED,
    CAMPAIGN_ACCESS_REQUIRED,
    CAMPAIGN_TEAM_MEMBERSHIP_REQUIRED,
    ACTIVE_CAMPAIGN_REQUIRED,
    ONLINE_REQUIRED,
    OFFLINE_WORKING_SET_REQUIRED,
    CLOSE_GUARDS_FAILED,
    PLATFORM_GOVERNED,
    SYSTEM_CONTROLLED
}

sealed interface AuthorizationDecision {
    data object Allowed : AuthorizationDecision

    data class Denied(
        val reason: AuthorizationDenialReason
    ) : AuthorizationDecision
}

data class AuthorizationContext(
    val userId: UserId,
    val userStatus: UserStatus,
    val membershipOrganizationId: OrganizationId,
    val membershipStatus: MembershipStatus,
    val role: OrganizationRole,
    val resourceOrganizationId: OrganizationId,
    val campaignId: CampaignId? = null,
    val campaignStatus: CampaignStatus? = null,
    val campaignAccessAuthorized: Boolean = false,
    val campaignTeamMember: Boolean = false,
    val connectivity: ConnectivityState = ConnectivityState.ONLINE,
    val offlineReady: Boolean = false,
    val closeGuardsSatisfied: Boolean = false
)

object AuthorizationPolicy {

    fun authorize(
        operation: AuthorizationOperation,
        context: AuthorizationContext
    ): AuthorizationDecision {
        baseDenial(context)?.let { return AuthorizationDecision.Denied(it) }

        return when (operation) {
            AuthorizationOperation.MASTER_DATA_MUTATE ->
                requireAdmin(context)

            AuthorizationOperation.CAMPAIGN_CREATE_LAUNCH,
            AuthorizationOperation.CAMPAIGN_SCOPE_MANAGE,
            AuthorizationOperation.CAMPAIGN_TEAM_ASSIGN ->
                requireAdminOnline(context)

            AuthorizationOperation.STOCKTAKING_MUTATE,
            AuthorizationOperation.ISSUE_REPORT ->
                requireCampaignMutationContext(context, requireTeamMembership = true)

            AuthorizationOperation.ISSUE_RESOLVE ->
                requireCampaignMutationContext(context, requireTeamMembership = false)

            AuthorizationOperation.CLOSE_FOLLOW_UP_ACCEPT ->
                requireAdminCampaignOnline(context, requireCloseGuards = false)

            AuthorizationOperation.INVENTORY_CLOSE ->
                requireAdminCampaignOnline(context, requireCloseGuards = true)

            AuthorizationOperation.REPORT_VIEW ->
                requireAuthorizedCampaignWhenSpecified(context)

            AuthorizationOperation.AUDIT_TRAIL_VIEW,
            AuthorizationOperation.USER_ADMIN,
            AuthorizationOperation.ORGANIZATION_CONFIG_MUTATE ->
                requireAdmin(context)

            AuthorizationOperation.PLATFORM_GOVERNED_MUTATE ->
                AuthorizationDecision.Denied(AuthorizationDenialReason.PLATFORM_GOVERNED)

            AuthorizationOperation.SYSTEM_RULE_MUTATE ->
                AuthorizationDecision.Denied(AuthorizationDenialReason.SYSTEM_CONTROLLED)

            AuthorizationOperation.NOTIFICATION_READ,
            AuthorizationOperation.SYNC_STATUS_VIEW ->
                AuthorizationDecision.Allowed
        }
    }

    private fun baseDenial(context: AuthorizationContext): AuthorizationDenialReason? = when {
        context.userStatus != UserStatus.ACTIVE -> AuthorizationDenialReason.USER_DISABLED
        context.membershipStatus != MembershipStatus.ACTIVE -> AuthorizationDenialReason.MEMBERSHIP_DISABLED
        context.membershipOrganizationId != context.resourceOrganizationId -> AuthorizationDenialReason.CROSS_ORGANIZATION
        else -> null
    }

    private fun requireAdmin(context: AuthorizationContext): AuthorizationDecision =
        if (context.role == OrganizationRole.ADMIN) {
            AuthorizationDecision.Allowed
        } else {
            AuthorizationDecision.Denied(AuthorizationDenialReason.ADMIN_REQUIRED)
        }

    private fun requireAdminOnline(context: AuthorizationContext): AuthorizationDecision {
        val adminDecision = requireAdmin(context)
        if (adminDecision is AuthorizationDecision.Denied) return adminDecision
        return if (context.connectivity == ConnectivityState.ONLINE) {
            AuthorizationDecision.Allowed
        } else {
            AuthorizationDecision.Denied(AuthorizationDenialReason.ONLINE_REQUIRED)
        }
    }

    private fun requireCampaignMutationContext(
        context: AuthorizationContext,
        requireTeamMembership: Boolean
    ): AuthorizationDecision {
        campaignContextDenial(context)?.let { return AuthorizationDecision.Denied(it) }
        if (requireTeamMembership && !context.campaignTeamMember) {
            return AuthorizationDecision.Denied(
                AuthorizationDenialReason.CAMPAIGN_TEAM_MEMBERSHIP_REQUIRED
            )
        }
        if (context.connectivity == ConnectivityState.OFFLINE && !context.offlineReady) {
            return AuthorizationDecision.Denied(
                AuthorizationDenialReason.OFFLINE_WORKING_SET_REQUIRED
            )
        }
        return AuthorizationDecision.Allowed
    }

    private fun requireAdminCampaignOnline(
        context: AuthorizationContext,
        requireCloseGuards: Boolean
    ): AuthorizationDecision {
        val adminDecision = requireAdmin(context)
        if (adminDecision is AuthorizationDecision.Denied) return adminDecision
        campaignContextDenial(context)?.let { return AuthorizationDecision.Denied(it) }
        if (context.connectivity != ConnectivityState.ONLINE) {
            return AuthorizationDecision.Denied(AuthorizationDenialReason.ONLINE_REQUIRED)
        }
        if (requireCloseGuards && !context.closeGuardsSatisfied) {
            return AuthorizationDecision.Denied(AuthorizationDenialReason.CLOSE_GUARDS_FAILED)
        }
        return AuthorizationDecision.Allowed
    }

    private fun campaignContextDenial(context: AuthorizationContext): AuthorizationDenialReason? = when {
        context.campaignId == null || context.campaignStatus == null ->
            AuthorizationDenialReason.CAMPAIGN_CONTEXT_REQUIRED

        !context.campaignAccessAuthorized ->
            AuthorizationDenialReason.CAMPAIGN_ACCESS_REQUIRED

        context.campaignStatus != CampaignStatus.ACTIVE ->
            AuthorizationDenialReason.ACTIVE_CAMPAIGN_REQUIRED

        else -> null
    }

    private fun requireAuthorizedCampaignWhenSpecified(
        context: AuthorizationContext
    ): AuthorizationDecision {
        if (context.campaignId != null && !context.campaignAccessAuthorized) {
            return AuthorizationDecision.Denied(
                AuthorizationDenialReason.CAMPAIGN_ACCESS_REQUIRED
            )
        }
        return AuthorizationDecision.Allowed
    }
}
