package com.silomath.core.domain.model

data class AuthorizedOrganizationContext(
    val membershipId: String,
    val organizationId: String,
    val organizationName: String,
    val organizationCode: String,
    val role: String
)

data class SessionState(
    val userId: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val activeOrganizationId: String? = null,
    val authorizedOrganizations: List<AuthorizedOrganizationContext> = emptyList(),
    val isOfflineCapable: Boolean = false
) {
    val isLoggedIn: Boolean
        get() = userId != null && token != null

    val isOfflineEligible: Boolean
        get() = isLoggedIn && isOfflineCapable

    val hasAuthorizedActiveOrganization: Boolean
        get() =
            activeOrganizationId != null &&
                authorizedOrganizations.any {
                    it.organizationId == activeOrganizationId
                }
}