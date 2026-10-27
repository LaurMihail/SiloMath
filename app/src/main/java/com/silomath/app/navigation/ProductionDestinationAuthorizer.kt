package com.silomath.app.navigation

import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.domain.repository.LocationRepository
import com.silomath.core.navigation.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Re-read actual session scope. Unimplemented feature contexts remain fail-closed;
 * their domain authorizer is enabled only together with the real destination renderer.
 * A serialized key or an Intent never grants entity/campaign/state access. */
class ProductionDestinationAuthorizer @Inject constructor(
    private val auth: AuthRepository, private val locations: LocationRepository
) : DestinationAuthorizer {
    override suspend fun isAuthorized(target: OrganizationDestination): Boolean {
        val session = auth.sessionState.first()
        if (!session.isLoggedIn || session.activeOrganizationId != target.organizationId.value ||
            session.authorizedOrganizations.none { it.organizationId == target.organizationId.value }) return false
        return when(target) {
            is ShellDestination -> true
            is LocationEditor -> {
                if(session.authorizedOrganizations.none{it.organizationId==target.organizationId.value && it.role=="ADMIN"})return false
                val found=target.locationId==null || locations.observeLocations(target.organizationId).first().any {
                    it.organizationId==target.organizationId && it.locationId==target.locationId
                }
                found && auth.sessionState.first()==session
            }
            is LocationUnits -> {
                if(session.authorizedOrganizations.none {it.organizationId==target.organizationId.value &&
                    it.role in setOf("ADMIN","STANDARD")}) return false
                val found=locations.observeLocations(target.organizationId).first().any {
                    it.organizationId==target.organizationId && it.locationId==target.locationId
                }
                // Domain lookup may suspend; membership and active organization must still match.
                found && auth.sessionState.first()==session
            }
            else -> false // Unimplemented destination renderers remain fail-closed.
        }
    }
}
