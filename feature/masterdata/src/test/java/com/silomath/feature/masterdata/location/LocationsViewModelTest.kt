package com.silomath.feature.masterdata.location

import androidx.lifecycle.SavedStateHandle
import com.silomath.core.domain.model.*
import com.silomath.core.domain.repository.*
import com.silomath.core.model.*
import com.silomath.core.navigation.*
import io.mockk.*
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LocationsViewModelTest {
    private val org=OrganizationId("00000000-0000-0000-0000-000000000001")
    private val other=OrganizationId("00000000-0000-0000-0000-000000000002")
    private val session=MutableStateFlow(SessionState(userId="fixture-user",token="test-only",
        activeOrganizationId=org.value,authorizedOrganizations=listOf(
            AuthorizedOrganizationContext("fixture-membership",org.value,"Fixture","FIX","ADMIN"))))
    private val rows=MutableStateFlow(listOf(location(1,"North","N",true),location(2,"South","S",false),
        location(3,"Old","OLD",true,MasterStatus.INACTIVE),location(4,"Foreign","F",true,organization=other)))
    private val auth=mockk<AuthRepository>().also {every{it.sessionState} returns session}
    private val repo=mockk<LocationRepository>().also {every{it.observeLocations(org)} returns rows}
    @Before fun setup(){Dispatchers.setMain(UnconfinedTestDispatcher())}
    @After fun tearDown(){Dispatchers.resetMain()}
    private fun location(n:Int,name:String,code:String,position:Boolean,status:MasterStatus=MasterStatus.ACTIVE,
        organization:OrganizationId=org)=Location(LocationId("10000000-0000-0000-0000-%012d".format(n)),organization,
        name,code,null,null,null,null,status,Instant.EPOCH,"fixture",Instant.EPOCH,"fixture",1,
        if(position)BigDecimal("45") else null,if(position)BigDecimal("25") else null)
    private fun TestScope.observe(vm:LocationsViewModel)=backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect()}

    @Test fun oneAuthorizedDatasetFeedsBothModesAndNoPositionRowsRemainInList()=runTest {
        val vm=LocationsViewModel(auth,repo,SavedStateHandle());observe(vm)
        assertEquals(listOf("North","South"),vm.uiState.value.locations.map{it.name})
        assertEquals(listOf("North"),vm.uiState.value.positionedLocations.map{it.name})
        val before=vm.uiState.value.locations;vm.view(LocationView.Map)
        assertEquals(before,vm.uiState.value.locations)
        vm.search("  s  ");assertEquals(listOf("South"),vm.uiState.value.locations.map{it.name})
        assertTrue(vm.uiState.value.positionedLocations.isEmpty())
        vm.search(" old ");vm.filter(LifecycleFilter.All)
        assertEquals(listOf("Old"),vm.uiState.value.locations.map{it.name})
        vm.filter(LifecycleFilter.Inactive);assertEquals(1,vm.uiState.value.positionedLocations.size)
    }
    @Test fun organizationRevocationAndLogoutClearRowsAndPreventNavigation()=runTest {
        val vm=LocationsViewModel(auth,repo,SavedStateHandle());observe(vm)
        session.value=session.value.copy(authorizedOrganizations=emptyList())
        assertTrue(vm.uiState.value.locations.isEmpty());assertFalse(vm.uiState.value.isAdmin)
        val target=async{vm.navigationRequests.first()};vm.open(rows.value.first().locationId);runCurrent()
        assertFalse(target.isCompleted);target.cancel()
        session.value=session.value.copy(token=null);assertTrue(vm.uiState.value.locations.isEmpty())
    }
    @Test fun markerAndRowUseSameTypedDestinationAndRejectForeignOrFilteredIds()=runTest {
        val vm=LocationsViewModel(auth,repo,SavedStateHandle());observe(vm)
        val target=async{vm.navigationRequests.first()};vm.open(rows.value.first().locationId);runCurrent()
        assertEquals(LocationUnits(org,rows.value.first().locationId),target.await())
        val forbidden=async{vm.navigationRequests.first()}
        vm.open(rows.value.last().locationId);vm.filter(LifecycleFilter.Inactive)
        vm.open(rows.value.first().locationId);runCurrent();assertFalse(forbidden.isCompleted);forbidden.cancel()
    }
    @Test fun stateRestoresFiltersModeAndValidSelectionWithoutStoringDomainPayload()=runTest {
        val selected=rows.value[2].locationId
        val saved=SavedStateHandle(mapOf("location.query" to " old ","location.lifecycle" to "Inactive",
            "location.view" to "Map","location.selected" to selected.value))
        val vm=LocationsViewModel(auth,repo,saved);observe(vm)
        assertEquals(LocationView.Map,vm.uiState.value.view);assertEquals(selected,vm.uiState.value.selectedId)
        vm.search("no match");assertNull(vm.uiState.value.selectedId)
    }
    @Test fun standardCannotRequestMutationAndSourceErrorsDoNotLeakDetails()=runTest {
        session.value=session.value.copy(authorizedOrganizations=session.value.authorizedOrganizations.map{it.copy(role="STANDARD")})
        val vm=LocationsViewModel(auth,repo,SavedStateHandle());observe(vm)
        assertFalse(vm.uiState.value.isAdmin)
        val target=async{vm.navigationRequests.first()};vm.edit();runCurrent();assertFalse(target.isCompleted);target.cancel()
        every{repo.observeLocations(org)} returns flow{throw IllegalStateException("secret fixture exception")}
        vm.retry();assertEquals("Locations could not be loaded.",vm.uiState.value.error)
        assertTrue(vm.uiState.value.locations.isEmpty())
    }
}
