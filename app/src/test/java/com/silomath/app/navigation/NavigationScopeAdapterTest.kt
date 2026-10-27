package com.silomath.app.navigation

import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import org.junit.Assert.*
import org.junit.Test

class NavigationScopeAdapterTest {
    private val id="00000000-0000-0000-0000-000000000001"
    private val org="00000000-0000-0000-0000-000000000002"
    private fun session(role:String="STANDARD")=SessionState(userId=id,token="test-only-token",
        activeOrganizationId=org,authorizedOrganizations=listOf(
            AuthorizedOrganizationContext("test-membership",org,"Fixture","FIX",role)))
    @Test fun failedRestorationNeverExposesSavedCredentialScope() {
        val scope=navigationScope(session(),false)
        assertNull(scope.userId);assertNull(scope.activeOrganizationId)
        assertTrue(scope.authorizedOrganizations.isEmpty())
    }
    @Test fun roleComesOnlyFromAuthorizedMembership() {
        assertTrue(navigationScope(session(),true).adminOrganizations.isEmpty())
        assertEquals(setOf(org),navigationScope(session("ADMIN"),true).adminOrganizations.map{it.value}.toSet())
    }
    @Test fun malformedLegacyIdsCannotBecomeCanonicalDestinationContext() {
        val scope=navigationScope(session().copy(userId="legacy-user",activeOrganizationId="legacy-org"),true)
        assertNull(scope.userId);assertNull(scope.activeOrganizationId)
    }
}
