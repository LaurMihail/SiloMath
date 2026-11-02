package com.silomath.core.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.silomath.core.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class NavigationContractTest {
    private val org = OrganizationId("00000000-0000-0000-0000-000000000001")
    private val other = OrganizationId("00000000-0000-0000-0000-000000000002")
    private val user = UserId("00000000-0000-0000-0000-000000000003")
    private val campaign = CampaignId("00000000-0000-0000-0000-000000000004")
    private val location = LocationId("00000000-0000-0000-0000-000000000005")
    private val unit = StorageUnitId("00000000-0000-0000-0000-000000000006")
    private val space = StorageSpaceId("00000000-0000-0000-0000-000000000007")
    private val scope = NavigationScope(user,org,setOf(org),emptySet())
    private val workspace = StocktakingSpace(org,campaign,location,unit,space)
    private fun state() = OperationalStacks(org,mutableStateOf(ShellSection.Dashboard),
        ShellSection.entries.associateWith { NavBackStack<NavKey>(ShellDestination(org,it)) })

    @Test fun canonicalFiveDestinationsHaveExactOrderAndIdentity() {
        assertEquals(listOf("DSH-01","CAM-01","STK-01","LOC-01","MOR-01"),
            ShellSection.entries.map { it.componentId })
        assertEquals(listOf("Dashboard","Inventory Campaigns","Stocktaking","Locations","More"),
            ShellSection.entries.map { it.title })
    }
    @Test fun serializableWorkspacePreservesEveryStableContextId() {
        val encoded = DestinationCodec.encode(workspace)
        assertEquals(workspace,DestinationCodec.decode(encoded))
        assertFalse(encoded.contains("password"));assertFalse(encoded.contains("token"))
        assertEquals(listOf(ShellDestination(org,ShellSection.Stocktaking),
            StocktakingLocation(org,campaign,location),StocktakingUnit(org,campaign,location,unit),
            workspace),workspace.parentChain())
    }
    @Test fun unknownAndTamperedReferencesFailClosed() {
        assertNull(DestinationCodec.decode("not json"))
        assertNull(DestinationCodec.decode("{\"type\":\"Issues\"}"))
        assertNull(DestinationCodec.decode("x".repeat(8193)))
        val valid = DestinationCodec.encode(LocationUnits(org,location))
        assertNull(DestinationCodec.decode(valid.replace(org.value,"invalid-uuid")))
        assertNull(DestinationCodec.decode(valid.dropLast(1)+",\"isAdmin\":true}"))
    }
    @Test fun unauthenticatedAndCrossOrganizationTargetsNeverReachAuthorizer() = runTest {
        var calls = 0
        val auth = DestinationAuthorizer { calls++; true }
        val target = listOf(LocationUnits(org,location))
        assertFalse(authorizeNavigation(target,{scope.copy(userId=null)},auth))
        assertFalse(authorizeNavigation(target,{scope.copy(activeOrganizationId=other)},auth))
        assertFalse(authorizeNavigation(target,{scope.copy(authorizedOrganizations=emptySet())},auth))
        assertEquals(0,calls)
    }
    @Test fun adminVisibilityDoesNotSubstituteForDomainAuthorization() = runTest {
        val target = LocationEditor(org)
        assertFalse(scopeAllows(target,scope))
        val admin = scope.copy(adminOrganizations=setOf(org))
        assertTrue(scopeAllows(target,admin))
        assertFalse(authorizeNavigation(listOf(target),{admin},DestinationAuthorizer { false }))
    }
    @Test fun unknownCampaignOrEntityStateIsDeniedWithoutChangingCurrentStack() = runTest {
        val state = state()
        assertFalse(state.open(workspace,{scope},DestinationAuthorizer { false },true))
        assertEquals(ShellSection.Dashboard,state.activeSection)
        assertEquals(1,state.currentStack.size)
    }
    @Test fun changedPrincipalDuringAsyncCheckCannotCommitNavigation() = runTest {
        var current = scope
        val state=state()
        assertFalse(state.open(LocationUnits(org,location),{current},DestinationAuthorizer {
            current=scope.copy(userId=null);true
        }))
        assertEquals(ShellSection.Dashboard,state.activeSection)
    }
    @Test fun dependencyFailureIsDeniedWithoutInterpretingItAsAuthorization() = runTest {
        assertFalse(authorizeNavigation(listOf(LocationUnits(org,location)),{scope},
            DestinationAuthorizer { throw IllegalStateException("dependency unavailable") }))
    }
    @Test fun cancellationIsNotSwallowedAsAnAuthorizationDecision() = runTest {
        try {
            authorizeNavigation(listOf(LocationUnits(org,location)),{scope},
                DestinationAuthorizer { throw CancellationException("cancelled") })
            fail("Cancellation must propagate")
        } catch (_:CancellationException) { }
    }
    @Test fun deepLinkAuthorizesEveryParentAndBackRestoresUnitContext() = runTest {
        val state=state();val seen=mutableListOf<OrganizationDestination>()
        assertTrue(state.open(workspace,{scope},DestinationAuthorizer { seen+=it;true },true))
        assertEquals(workspace.parentChain(),seen)
        assertEquals(workspace,state.current)
        state.back()
        assertEquals(StocktakingUnit(org,campaign,location,unit),state.current)
    }
    @Test fun tabSwitchRetainsInactiveHierarchyAndSameTabDoesNotDuplicateRoot() = runTest {
        val state=state();val auth=DestinationAuthorizer {true}
        state.open(workspace,{scope},auth,true)
        state.open(ShellDestination(org,ShellSection.Locations),{scope},auth)
        state.open(ShellDestination(org,ShellSection.Locations),{scope},auth)
        assertEquals(1,state.currentStack.size)
        state.open(ShellDestination(org,ShellSection.Stocktaking),{scope},auth)
        assertEquals(workspace,state.current)
        state.back();assertEquals(unit,(state.current as StocktakingUnit).storageUnitId)
    }
    @Test fun wrongOrganizationCannotBePushedIntoAnotherOrganizationsOwnedStacks() = runTest {
        val state=state()
        assertFalse(state.open(LocationUnits(other,location),
            {scope.copy(activeOrganizationId=other,authorizedOrganizations=setOf(org,other))},
            DestinationAuthorizer {true},true))
    }
    @Test fun storageUnitEditorKeyRequiresAdminAndPreservesCanonicalBackContext()=runTest {
        val editor=StorageUnitEditor(org,location,unit)
        assertEquals(editor,DestinationCodec.decode(DestinationCodec.encode(editor)))
        assertFalse(scopeAllows(editor,scope));assertTrue(scopeAllows(editor,scope.copy(adminOrganizations=setOf(org))))
        assertEquals(StorageUnitDetails(org,location,unit),editor.parent())
        assertEquals(LocationUnits(org,location),StorageUnitEditor(org,location).parent())
        val stacks=state();val admin=scope.copy(adminOrganizations=setOf(org));val allow=DestinationAuthorizer{true}
        assertTrue(stacks.open(LocationUnits(org,location),{admin},allow))
        assertTrue(stacks.open(editor,{admin},allow));stacks.back();assertEquals(LocationUnits(org,location),stacks.current)
    }

}
