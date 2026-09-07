package com.silomath.core.domain.model

data class SessionState(
    val userId: String? = null,
    val token: String? = null,
    val activeOrganizationId: Long? = null,
    val isOfflineCapable: Boolean = false,
    val isRememberMeEnabled: Boolean = false
) {
    val isLoggedIn: Boolean get() = userId != null && token != null
    val isOfflineEligible: Boolean get() = isLoggedIn && isOfflineCapable
}
