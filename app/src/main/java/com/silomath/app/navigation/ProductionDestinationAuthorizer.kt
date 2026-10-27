package com.silomath.app.navigation

import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.navigation.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Re-read actual session scope. Unimplemented feature contexts remain fail-closed;
 * their domain authorizer is enabled only together with the real destination renderer.
 * A serialized key or an Intent never grants entity/campaign/state access. */
class ProductionDestinationAuthorizer @Inject constructor(
    private val auth: AuthRepository
) : DestinationAuthorizer {
    override suspend fun isAuthorized(target: OrganizationDestination): Boolean {
        val session = auth.sessionState.first()
        if (!session.isLoggedIn || session.activeOrganizationId != target.organizationId.value ||
            session.authorizedOrganizations.none { it.organizationId == target.organizationId.value }) return false
        return target is ShellDestination
    }
}
