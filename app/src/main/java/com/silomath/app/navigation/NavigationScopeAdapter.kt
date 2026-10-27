package com.silomath.app.navigation

import com.silomath.core.domain.model.SessionState
import com.silomath.core.model.OrganizationId
import com.silomath.core.model.UserId
import com.silomath.core.navigation.NavigationScope

fun navigationScope(session: SessionState?, restoredSessionReady: Boolean): NavigationScope {
    if (session == null || !restoredSessionReady || !session.isLoggedIn) {
        return NavigationScope(null,null,emptySet(),emptySet())
    }
    fun organization(value: String?) = value?.let { runCatching { OrganizationId(it) }.getOrNull() }
    return NavigationScope(session.userId?.let { runCatching { UserId(it) }.getOrNull() },
        organization(session.activeOrganizationId),
        session.authorizedOrganizations.mapNotNull { organization(it.organizationId) }.toSet(),
        session.authorizedOrganizations.filter { it.role == "ADMIN" }
            .mapNotNull { organization(it.organizationId) }.toSet())
}
