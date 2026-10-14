package com.silomath.core.domain.model

data class SessionState(
    val userId: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val activeOrganizationId: String? = null,
    val isOfflineCapable: Boolean = false
) {
    val isLoggedIn: Boolean get() = userId != null && token != null
    val isOfflineEligible: Boolean get() = isLoggedIn && isOfflineCapable
}
