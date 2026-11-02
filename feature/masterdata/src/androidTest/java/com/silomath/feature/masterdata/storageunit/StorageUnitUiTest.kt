package com.silomath.feature.masterdata.storageunit

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.silomath.core.designsystem.SiloMathTheme
import com.silomath.core.domain.model.*
import com.silomath.core.model.*
import com.silomath.feature.masterdata.location.*
import java.time.Instant
import org.junit.*
import org.junit.Assert.*

/** Controlled canonical UI fixtures only; real rendering/semantics, no Production fake source. */
class StorageUnitUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun id(n:Int)="10000000-0000-0000-0000-"+n.toString().padStart(12,'0')
    private val org=OrganizationId(id(1));private val loc=LocationId(id(2))
    private val parent=Location(loc,org,"North Location","NORTH",null,null,null,null,MasterStatus.ACTIVE,Instant.EPOCH,id(9),Instant.EPOCH,id(9),1)
    private val active=StorageUnit(StorageUnitId(id(3)),loc,"Alpha","A",null,MasterStatus.ACTIVE,Instant.EPOCH,id(9),Instant.EPOCH,id(9),1)
    private val inactive=active.copy(storageUnitId=StorageUnitId(id(4)),name="Beta",code="B",lifecycleStatus=MasterStatus.INACTIVE)
    private val child=StorageSpace(StorageSpaceId(id(5)),active.storageUnitId,"S01",null,GeometryProfileId(id(6)),MasterStatus.INACTIVE,Instant.EPOCH,id(9),Instant.EPOCH,id(9),1)
    private fun listState(admin:Boolean=false)=LocationUnitsState(parent,listOf(active,inactive),loading=false,admin=admin)
    private fun capture(name:String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.filesDir,"md004-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun loc02SearchLifecycleStableSelectionAndAdminActions() {
        var query by mutableStateOf("");var filter by mutableStateOf(LifecycleFilter.Active)
        var opened:StorageUnitId?=null;var added=false;var edited:StorageUnitId?=null
        compose.setContent {SiloMathTheme {LocationUnitsContent(listState(true),query,{query=it},filter,{filter=it},{opened=it.storageUnitId},{added=true},{edited=it.storageUnitId})}}
        compose.onNodeWithText("North Location").assertExists();capture("loc02")
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("Alpha"))
        compose.onNodeWithText("Alpha").performClick();compose.runOnIdle{assertEquals(active.storageUnitId,opened)}
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("Inactive",substring=false))
        compose.onNodeWithText("Inactive").performClick()
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("Beta"));compose.onNodeWithText("Beta").assertExists()
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("Search Storage Units"));compose.onNodeWithText("Search Storage Units").performTextInput("no-match")
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("No Storage Units match these filters."))
        compose.onNodeWithText("No Storage Units match these filters.").assertExists()
        compose.runOnIdle{query="";filter=LifecycleFilter.All}
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("Edit Storage Unit B"));compose.onNodeWithText("Edit Storage Unit B").performClick()
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("Add Storage Unit"));compose.onNodeWithText("Add Storage Unit").performClick()
        compose.runOnIdle{assertTrue(added);assertEquals(inactive.storageUnitId,edited)}
    }
    @Test fun loc02StandardHasNoMutationControlsAndWrongParentNeverAppears() {
        compose.setContent {SiloMathTheme {LocationUnitsContent(listState().copy(units=listOf(active,active.copy(name="Foreign",locationId=LocationId(id(99))))),"",{},LifecycleFilter.All,{},{},{},{})}}
        compose.onNodeWithText("Add Storage Unit").assertDoesNotExist();compose.onNodeWithText("Edit Storage Unit A").assertDoesNotExist()
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("Alpha"));compose.onNodeWithText("Alpha").assertExists();compose.onNodeWithText("Foreign").assertDoesNotExist()
    }
    @Test fun loc02LoadingErrorAndVerifiedEmptyAreDistinct() {
        var state by mutableStateOf(LocationUnitsState())
        compose.setContent {SiloMathTheme {LocationUnitsContent(state,"",{},LifecycleFilter.Active,{},{},{},{})}}
        compose.onNodeWithContentDescription("Loading Storage Units").assertExists()
        compose.runOnIdle{state=LocationUnitsState(loading=false,error="Storage Units could not be loaded.")}
        compose.onNodeWithText("Storage Units could not be loaded.").assertExists()
        compose.runOnIdle{state=LocationUnitsState(parent,emptyList(),loading=false)}
        compose.onNodeWithTag("storage-unit-list").performScrollToNode(hasText("No Storage Units in this Location."))
        compose.onNodeWithText("No Storage Units in this Location.").assertExists()
    }
    @Test fun stu02ShowsCanonicalChildrenStatusAndReadOnlyLaterTaskBoundary() {
        var state by mutableStateOf(StorageUnitDetailsState(parent,active,listOf(child),loading=false,admin=true))
        var opened:StorageSpaceId?=null;var edit=false
        compose.setContent {SiloMathTheme {StorageUnitDetailsContent(state,{edit=true},{opened=it})}}
        compose.onNodeWithText("Location: North Location (NORTH)").assertExists()
        compose.onNodeWithTag("unit-details-list").performScrollToNode(hasText("Edit Storage Unit"));compose.onNodeWithText("Edit Storage Unit").performClick()
        compose.onNodeWithTag("unit-details-list").performScrollToNode(hasText("S01"));compose.onNodeWithText("S01").performClick()
        compose.runOnIdle{assertEquals(child.storageSpaceId,opened);assertTrue(edit)}
        compose.onNodeWithText("Add Storage Space").assertIsNotEnabled()
        compose.runOnIdle{state=state.copy(admin=false)}
        compose.onNodeWithText("Edit Storage Unit").assertDoesNotExist();compose.onNodeWithText("Add Storage Space").assertDoesNotExist()
    }
    @Test fun stu02ReadFailureNeverBecomesFalseEmptyAndLaterErrorIsExplicit() {
        var state by mutableStateOf(StorageUnitDetailsState(parent,active,loading=false))
        compose.setContent {SiloMathTheme {StorageUnitDetailsContent(state,{},{},navigationError="Storage Space details are not available.")}}
        compose.onNodeWithTag("unit-details-list").performScrollToNode(hasText("No Storage Spaces in this Storage Unit."));compose.onNodeWithText("No Storage Spaces in this Storage Unit.").assertExists()
        compose.runOnIdle{state=state.copy(spacesError="Storage Spaces could not be loaded.")}
        compose.onNodeWithTag("unit-details-list").performScrollToNode(hasText("Storage Spaces could not be loaded."));compose.onNodeWithText("No Storage Spaces in this Storage Unit.").assertDoesNotExist()
        compose.onNodeWithTag("unit-details-list").performScrollToNode(hasText("Storage Space details are not available."));compose.onNodeWithText("Storage Space details are not available.").assertExists()
    }
    @Test fun sheetEditsOnlyApprovedFieldsFixedParentLifecycleSaveAndCancel() {
        var state by mutableStateOf(StorageUnitEditorState(parent,loading=false,allowed=true));var saves=0;var cancels=0
        compose.setContent {SiloMathTheme {StorageUnitEditorContent(state,false,{state=state.copy(draft=it)},{saves++},{cancels++})}}
        compose.onNodeWithText("Location: North Location (NORTH)").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Read only"))
        compose.onNodeWithText("Storage Unit Name").performTextInput("New")
        compose.onNodeWithText("Storage Unit Code").performTextInput(" n ");capture("editor")
        compose.onNodeWithText("Description").performTextInput("Description")
        compose.onNodeWithText("Inactive").performScrollTo().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.runOnIdle{assertEquals("New",state.draft.name);assertEquals(MasterStatus.INACTIVE,state.draft.status);assertEquals(1,saves)}
        compose.onNodeWithText("Cancel").performScrollTo().performClick();compose.runOnIdle{assertEquals(1,cancels);assertEquals(1,saves)}
    }
    @Test fun sheetDeniedHistoricalCodeAndFailureDraftHaveAccessibleSemantics() {
        var state by mutableStateOf(StorageUnitEditorState(parent,StorageUnitDraft("Draft","A"),loading=false,allowed=true,codeLocked=true,error="Storage Unit could not be saved."))
        compose.setContent {SiloMathTheme {StorageUnitEditorContent(state,true,{},{},{})}}
        compose.onNodeWithText("Storage Unit Code").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Read only"))
        compose.onNodeWithText("Draft").assertExists();compose.onNodeWithText("Storage Unit could not be saved.").assertExists()
        compose.runOnIdle{state=StorageUnitEditorState(loading=false,error="Admin required")}
        compose.onNodeWithText("Storage Unit Name").assertDoesNotExist();compose.onNodeWithText("Save").assertDoesNotExist();compose.onNodeWithText("Cancel").assertIsEnabled()
    }
    @Test fun shortCompactAt200PercentFontRetainsScrollableFormAndTouchTargets() {
        var cancelled=false
        compose.setContent {SiloMathTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
                Box(Modifier.requiredSize(360.dp,320.dp)) {StorageUnitEditorContent(StorageUnitEditorState(parent,StorageUnitDraft("Long Storage Unit Name","U"),loading=false,allowed=true),true,{},{},{cancelled=true})}
            }
        }}
        compose.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cancel").performScrollTo().assertHeightIsAtLeast(48.dp).performClick();compose.runOnIdle{assertTrue(cancelled)}
    }
    @Test fun expandedDetailsUsesTwoPanesWithoutLosingUnitOrSpaces() {
        compose.setContent {SiloMathTheme {Box(Modifier.requiredSize(900.dp,600.dp)) {
            StorageUnitDetailsContent(StorageUnitDetailsState(parent,active,listOf(child),loading=false),{},{})
        }}}
        compose.onNodeWithTag("unit-details-list").assertExists();compose.onNodeWithTag("unit-spaces-list").assertExists()
        compose.onNodeWithText("Alpha").assertExists();compose.onNodeWithText("S01").assertExists();capture("expanded")
    }
}
