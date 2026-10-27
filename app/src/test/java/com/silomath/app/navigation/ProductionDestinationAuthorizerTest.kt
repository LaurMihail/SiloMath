package com.silomath.app.navigation

import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.model.*
import com.silomath.core.navigation.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ProductionDestinationAuthorizerTest {
    private val org=OrganizationId("00000000-0000-0000-0000-000000000001")
    private val other=OrganizationId("00000000-0000-0000-0000-000000000002")
    private val state=MutableStateFlow(SessionState(userId="00000000-0000-0000-0000-000000000003",
        token="test-only-token",activeOrganizationId=org.value,authorizedOrganizations=listOf(
            AuthorizedOrganizationContext("test-membership",org.value,"Fixture","FIX","ADMIN"))))
    private val auth=mockk<AuthRepository>().also { every {it.sessionState} returns state }
    private val authorizer=ProductionDestinationAuthorizer(auth)
    @Test fun shellRequiresCurrentAuthorizedOrganization()=runTest {
        assertTrue(authorizer.isAuthorized(ShellDestination(org,ShellSection.Locations)))
        assertFalse(authorizer.isAuthorized(ShellDestination(other,ShellSection.Locations)))
        state.value=state.value.copy(authorizedOrganizations=emptyList())
        assertFalse(authorizer.isAuthorized(ShellDestination(org,ShellSection.Locations)))
    }
    @Test fun missingAuthenticationNeverAcceptsAContextualReference()=runTest {
        state.value=state.value.copy(token=null)
        assertFalse(authorizer.isAuthorized(ShellDestination(org,ShellSection.Dashboard)))
    }
    @Test fun unimplementedFeatureReferencesDoNotExposeNewBlankDestinations()=runTest {
        assertFalse(authorizer.isAuthorized(LocationEditor(org)))
        assertFalse(authorizer.isAuthorized(LocationUnits(org,
            LocationId("00000000-0000-0000-0000-000000000004"))))
    }
}
