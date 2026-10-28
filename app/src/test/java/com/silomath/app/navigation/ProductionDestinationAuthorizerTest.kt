package com.silomath.app.navigation

import com.silomath.core.domain.model.AuthorizedOrganizationContext
import com.silomath.core.domain.model.SessionState
import com.silomath.core.domain.repository.AuthRepository
import com.silomath.core.domain.repository.LocationRepository
import kotlinx.coroutines.flow.flowOf
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
    private val locations=mockk<LocationRepository>().also {
        every {it.observeLocations(org)} returns flowOf(emptyList())
    }
    private val units=mockk<com.silomath.core.domain.repository.StorageUnitRepository>()
    private val authorizer=ProductionDestinationAuthorizer(auth,locations,units)
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
        assertTrue(authorizer.isAuthorized(LocationEditor(org)))
        assertFalse(authorizer.isAuthorized(LocationUnits(org,
            LocationId("00000000-0000-0000-0000-000000000004"))))
    }
    @Test fun locationUnitsRequiresCanonicalParentAndSupportedMembership()=runTest {
        val id=LocationId("00000000-0000-0000-0000-000000000004")
        val location=mockk<com.silomath.core.domain.model.Location>()
        every{location.locationId} returns id
        every{location.organizationId} returns org
        every{locations.observeLocations(org)} returns flowOf(listOf(location))
        assertTrue(authorizer.isAuthorized(LocationUnits(org,id)))
        every{location.organizationId} returns other
        assertFalse(authorizer.isAuthorized(LocationUnits(org,id)))
        every{location.organizationId} returns org
        state.value=state.value.copy(authorizedOrganizations=state.value.authorizedOrganizations.map{it.copy(role="UNSUPPORTED")})
        assertFalse(authorizer.isAuthorized(LocationUnits(org,id)))
    }
    @Test fun locationLookupCannotGrantAccessAfterSessionChanges()=runTest {
        val id=LocationId("00000000-0000-0000-0000-000000000004")
        val location=mockk<com.silomath.core.domain.model.Location>()
        every{location.locationId} returns id;every{location.organizationId} returns org
        every{locations.observeLocations(org)} returns kotlinx.coroutines.flow.flow {
            state.value=state.value.copy(token=null)
            emit(listOf(location))
        }
        assertFalse(authorizer.isAuthorized(LocationUnits(org,id)))
    }
    @Test fun editorRequiresAdminForCreateAndCanonicalScopeForEdit()=runTest {
        assertTrue(authorizer.isAuthorized(LocationEditor(org)))
        state.value=state.value.copy(authorizedOrganizations=state.value.authorizedOrganizations.map{it.copy(role="STANDARD")})
        assertFalse(authorizer.isAuthorized(LocationEditor(org)))
        assertFalse(authorizer.isAuthorized(LocationEditor(org,LocationId("00000000-0000-0000-0000-000000000004"))))
    }
    @Test fun storageUnitDetailsAndEditorRequireMatchingParentAndCurrentRole()=runTest {
        val parent=LocationId("00000000-0000-0000-0000-000000000004")
        val id=StorageUnitId("00000000-0000-0000-0000-000000000005")
        val location=mockk<com.silomath.core.domain.model.Location>()
        every{location.locationId} returns parent;every{location.organizationId} returns org
        every{locations.observeLocations(org)} returns flowOf(listOf(location))
        val unit=mockk<com.silomath.core.domain.model.StorageUnit>()
        every{unit.storageUnitId} returns id;every{unit.locationId} returns parent
        every{units.observeStorageUnits(parent)} returns flowOf(listOf(unit))
        assertTrue(authorizer.isAuthorized(StorageUnitDetails(org,parent,id)))
        assertTrue(authorizer.isAuthorized(StorageUnitEditor(org,parent,id)))
        state.value=state.value.copy(authorizedOrganizations=state.value.authorizedOrganizations.map{it.copy(role="STANDARD")})
        assertTrue(authorizer.isAuthorized(StorageUnitDetails(org,parent,id)))
        assertFalse(authorizer.isAuthorized(StorageUnitEditor(org,parent)))
        assertFalse(authorizer.isAuthorized(StorageUnitEditor(org,parent,id)))
        every{unit.locationId} returns LocationId("00000000-0000-0000-0000-000000000006")
        assertFalse(authorizer.isAuthorized(StorageUnitDetails(org,parent,id)))
    }

}
