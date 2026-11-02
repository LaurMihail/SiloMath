package com.silomath.feature.masterdata.location

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.silomath.core.designsystem.SiloMathTheme
import com.silomath.core.designsystem.components.SiloMathLocationMarker
import com.silomath.core.domain.model.*
import com.silomath.core.model.*
import com.silomath.feature.masterdata.location.map.*
import java.math.BigDecimal
import java.time.Instant
import org.junit.*
import org.junit.Assert.*

/** Explicit test-only canonical data; never injected into Production repositories. */
class LocationsScreenTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val org=OrganizationId("00000000-0000-0000-0000-000000000001")
    private fun row(n:Int,position:Boolean)=Location(LocationId("10000000-0000-0000-0000-%012d".format(n)),org,
        "Location $n","LOC$n",null,null,null,null,MasterStatus.ACTIVE,Instant.EPOCH,"fixture",Instant.EPOCH,"fixture",1,
        if(position)BigDecimal("45") else null,if(position)BigDecimal("25") else null)
    private val rows=listOf(row(1,true),row(2,false))
    @Test fun missingBasemapKeepsListAndUnpositionedRowsUsable() {
        var opened:LocationId?=null
        compose.setContent {SiloMathTheme {
            var mode by remember{mutableStateOf(LocationView.Map)}
            LocationsScreen(LocationUiState(org,rows,2,view=mode,loading=false),{}, {},{mode=it},
                {opened=it},{},{},{}) { _,_,_ -> Text("Basemap unavailable") }
        }}
        compose.onNodeWithText("Basemap unavailable").assertIsDisplayed()
        compose.onNodeWithText("Show list").performClick()
        compose.onNodeWithText("Location 2").assertIsDisplayed().performClick()
        compose.runOnIdle{assertEquals(rows[1].locationId,opened)}
        compose.onNodeWithText("Add Location").assertDoesNotExist()
    }
    @Test fun mapSlotReceivesOnlyPositionedSubsetAndLabeledMarkerUsesStableId() {
        var opened:LocationId?=null
        compose.setContent {SiloMathTheme {
            LocationsScreen(LocationUiState(org,rows,2,view=LocationView.Map,loading=false),{}, {},{},
                {opened=it},{},{},{}) { positioned,selected,open ->
                assertEquals(listOf(rows[0]),positioned)
                SiloMathLocationMarker(positioned[0].name,selected==positioned[0].locationId,{open(positioned[0].locationId)})
            }
        }}
        compose.onNodeWithText("Location 1").performClick()
        compose.runOnIdle{assertEquals(rows[0].locationId,opened)}
        compose.onNodeWithText("Locations without map positions remain available in List.").assertExists()
    }
    @Test fun compactLargeFontShortWindowRetainsControlsAndRows() {
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,2f)) {SiloMathTheme {
                Box(Modifier.width(340.dp).height(450.dp)) {
                    LocationsScreen(LocationUiState(org,rows,2,loading=false),{}, {},{},{},{},{},{}) {_,_,_->}
                }
            }}
        }
        compose.onNodeWithTag("location-results").performScrollTo()
        compose.onNodeWithText("Location 2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Lifecycle Filter").performScrollTo().assertIsDisplayed()
    }
    @Test fun loadingErrorEmptyAndFilteredEmptyAreExplicit() {
        var state by mutableStateOf(LocationUiState())
        var retry=false
        compose.setContent {SiloMathTheme {LocationsScreen(state,{}, {},{},{},{},{},{retry=true}){_,_,_->}}}
        compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo)).assertExists()
        compose.runOnIdle{state=state.copy(loading=false,error="Locations could not be loaded.")}
        compose.onNodeWithText("Locations could not be loaded.").assertExists()
        compose.onNodeWithText("Retry").performClick();compose.runOnIdle{assertTrue(retry);state=state.copy(error=null)}
        compose.onNodeWithText("No Locations available.").assertExists()
        compose.runOnIdle{state=state.copy(unfilteredCount=2)}
        compose.onNodeWithText("No Locations match these filters.").assertExists()
    }
    @Test fun expandedBoundsCoordinateListAndMapFromSameDataset() {
        compose.setContent {SiloMathTheme {Box(Modifier.requiredSize(900.dp,700.dp)) {
            LocationsScreen(LocationUiState(org,rows,2,view=LocationView.Map,loading=false),{}, {},{},{},{},{},{}) {positioned,_,_->
                assertEquals(listOf(rows[0]),positioned)
                Text("Coordinated map pane")
            }
        }}}
        compose.onNodeWithTag("location-results").assertExists()
        compose.onNodeWithText("Coordinated map pane").assertExists()
        compose.onNodeWithText("Location 2").assertExists()
    }
    @Test fun nativeEditorMapPicksExplicitCoordinatesWithoutDeviceLocation() {
        val runtime=FacilityMapRuntime(compose.activity.applicationContext,MapProviderConfig())
        var position:Pair<BigDecimal,BigDecimal>?=null
        compose.setContent {SiloMathTheme {Box(Modifier.fillMaxSize()) {
            NativeFacilityMap(emptyList(),null,{},runtime,BigDecimal("45") to BigDecimal("25"),{lat,lon->position=lat to lon})
        }}}
        compose.waitUntil(15000){compose.onAllNodesWithContentDescription("Location map").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("Location map").performTouchInput{click(center)}
        compose.waitUntil(15000){position!=null}
        compose.runOnIdle {
            assertTrue(position!!.first>=BigDecimal("-90") && position!!.first<=BigDecimal("90"))
            assertTrue(position!!.second>=BigDecimal("-180") && position!!.second<=BigDecimal("180"))
        }
    }
    @Test fun nativeMapWithoutProviderExposesAccessibleStoredMarkerAndFallback() {
        val runtime=FacilityMapRuntime(compose.activity.applicationContext,MapProviderConfig())
        var selected:LocationId?=null
        compose.setContent {SiloMathTheme {Box(Modifier.fillMaxSize()) {
            NativeFacilityMap(listOf(rows[0]),null,{selected=it},runtime)
        }}}
        compose.onNodeWithText("Basemap unavailable or incomplete. List remains available.").assertExists()
        compose.waitUntil(15000){compose.onAllNodesWithText("Location 1").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Location 1").performClick()
        compose.runOnIdle{assertEquals(rows[0].locationId,selected)}
    }
}
