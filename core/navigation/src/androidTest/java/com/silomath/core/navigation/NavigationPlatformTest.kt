package com.silomath.core.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInput
import androidx.test.espresso.Espresso
import androidx.core.view.WindowCompat
import com.silomath.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

private val fixtureOrg = OrganizationId("00000000-0000-0000-0000-000000000001")
private val fixtureCampaign = CampaignId("00000000-0000-0000-0000-000000000002")
private val fixtureLocation = LocationId("00000000-0000-0000-0000-000000000003")
private val fixtureUnit = StorageUnitId("00000000-0000-0000-0000-000000000004")
private val fixtureSpace = StorageSpaceId("00000000-0000-0000-0000-000000000005")
private val unitTarget = StocktakingUnit(fixtureOrg,fixtureCampaign,fixtureLocation,fixtureUnit)
private val spaceTarget = StocktakingSpace(fixtureOrg,fixtureCampaign,fixtureLocation,fixtureUnit,fixtureSpace)

class NavigationFixtureActivity: ComponentActivity() {
    lateinit var stack: NavBackStack<NavKey>
    var unitScrollIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);configureNavigationWindow(lightSurface = true)
        setContent {
            val nav=rememberNavBackStack(ShellDestination(fixtureOrg,ShellSection.Stocktaking),
                StocktakingLocation(fixtureOrg,fixtureCampaign,fixtureLocation),unitTarget)
            SideEffect { stack=nav }
            MaterialTheme {
                NavDisplay(backStack=nav,onBack={nav.removeLastOrNull()},
                    modifier=Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
                    entryDecorators=listOf(rememberSaveableStateHolderNavEntryDecorator()),
                    entryProvider={ key -> NavEntry(key) {
                        when(key) {
                            is StocktakingUnit -> {
                                var query by rememberSaveable { mutableStateOf("") }
                                val list=rememberLazyListState()
                                val index=list.firstVisibleItemIndex
                                SideEffect { unitScrollIndex=index }
                                Column(Modifier.fillMaxSize()) {
                                    Text("Unit "+key.storageUnitId.value)
                                    TextField(query,{query=it},label={Text("Search Storage Spaces")})
                                    Button({nav.add(spaceTarget)}) {Text("Open workspace")}
                                    LazyColumn(state=list,modifier=Modifier.weight(1f)) {
                                        items(100) { Text("Fixture row $it",Modifier.height(48.dp)) }
                                    }
                                }
                            }
                            is StocktakingSpace -> Text("Workspace "+key.storageSpaceId.value)
                            else -> Text("Parent")
                        }
                    }})
            }
        }
    }
}

private class PredictiveInput:NavigationEventInput() {
    fun start()=dispatchOnBackStarted(NavigationEvent(touchX=0f,touchY=100f,progress=0f,swipeEdge=NavigationEvent.EDGE_LEFT))
    fun progress(value:Float)=dispatchOnBackProgressed(NavigationEvent(touchX=0f,touchY=100f,progress=value,swipeEdge=NavigationEvent.EDGE_LEFT))
    fun cancel()=dispatchOnBackCancelled()
    fun complete()=dispatchOnBackCompleted()
}

class NavigationPlatformTest {
    @get:Rule val compose=createAndroidComposeRule<NavigationFixtureActivity>()
    @Test fun systemBarsFollowRenderedLightSurfaceRatherThanWallpaperNightMode() {
        compose.runOnIdle {
            val controller=WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView)
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
        }
    }
    @Test fun actualSystemBackRestoresUnitSearchScrollAndStableContext() {
        compose.onNode(hasSetTextAction()).performTextInput("Bay search")
        Espresso.closeSoftKeyboard()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(50)
        compose.onNodeWithText("Open workspace").performClick()
        compose.onNodeWithText("Workspace "+fixtureSpace.value).assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("Unit "+fixtureUnit.value).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("Bay search")
        compose.runOnIdle {assertTrue(compose.activity.unitScrollIndex>=50)}
    }
    @Test fun activityRecreationRestoresSerializableStackAndEntryState() {
        compose.onNode(hasSetTextAction()).performTextInput("Restored query")
        Espresso.closeSoftKeyboard()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(30)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Unit "+fixtureUnit.value).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("Restored query")
        compose.runOnIdle {
            assertEquals(unitTarget,compose.activity.stack.last())
            assertTrue(compose.activity.unitScrollIndex>=30)
        }
    }
    @Test fun predictiveProgressCancellationAndCommitChangeOnlyNavigationOnCommit() {
        compose.onNodeWithText("Open workspace").performClick()
        val input=PredictiveInput()
        compose.runOnIdle {
            compose.activity.navigationEventDispatcher.addInput(input)
            input.start();input.progress(0.5f)
            assertEquals(spaceTarget,compose.activity.stack.last())
            input.cancel()
        }
        compose.runOnIdle {assertEquals(spaceTarget,compose.activity.stack.last())}
        compose.runOnIdle {input.start();input.progress(0.8f);input.complete()}
        compose.onNodeWithText("Unit "+fixtureUnit.value).assertIsDisplayed()
        compose.runOnIdle {assertEquals(unitTarget,compose.activity.stack.last())}
    }
}
