package com.silomath.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.silomath.core.designsystem.components.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DesignSystemAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectedMarkerAndAccessibleListSurviveBasemapFailure() {
        var markerClicks = 0; var listClicks = 0
        compose.setContent { SiloMathTheme {
            SiloMathLocationMap(false, { listClicks++ }, Modifier.size(350.dp,500.dp)) {
                SiloMathLocationMarker("Location A", true, { markerClicks++ })
            }
        } }
        compose.onNodeWithText("Map unavailable. Locations remain available in List.").assertIsDisplayed()
        compose.onNodeWithText("Location A").assertIsSelected().assertHasClickAction().performClick()
        compose.onNodeWithText("Show list").performClick()
        compose.runOnIdle { assertEquals(1,markerClicks); assertEquals(1,listClicks) }
    }

    @Test fun disabledActionAndReadOnlyFieldExposeTheirRealState() {
        var clicked = false
        compose.setContent { SiloMathTheme { Column {
            SiloMathPrimaryButton("Save", { clicked = true }, enabled = false)
            SiloMathTextField("LOC-A", {}, "Location Code", readOnly = true)
            SiloMathTextField("", {}, "Location Name", errorMessage = "Location Name is required")
        } } }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("LOC-A").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Read only"))
        compose.onNode(hasSetTextAction() and hasText("Location Name")).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error,"Location Name is required"))
        compose.runOnIdle { assertFalse(clicked) }
    }

    @Test fun largeTextActionsKeepTouchBoundsAndRemainReachable() {
        compose.setContent { val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,2f)) {
                SiloMathTheme { Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    SiloMathPrimaryButton("Continue Offline", {}, Modifier.fillMaxWidth())
                    SiloMathStatusChip("Validated", SiloMathTone.Positive)
                    SiloMathFilterChip("Active", true, {})
                } }
            }
        }
        compose.onNodeWithText("Continue Offline").assertHeightIsAtLeast(48.dp).assertIsDisplayed()
        compose.onNodeWithText("Active").assertIsSelected().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Validated").assertIsDisplayed()
    }

    @Test fun selectionAndStableCallbacksSurviveCompactToRailResize() {
        var expanded by mutableStateOf(false)
        var selected by mutableStateOf("Dashboard")
        val labels = listOf("Dashboard","Inventory Campaigns","Stocktaking","Locations","More")
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(if (expanded) 1f else density.density)) {
                SiloMathTheme { SiloMathAdaptiveNavigation(
                    labels.map { SiloMathNavigationItem(it,it,Icons.Default.Dashboard) }, selected,
                    { selected = it }) { Text("Content") } }
            }
        }
        compose.onNodeWithText("Locations").performClick()
        compose.onNodeWithText("Locations").assertIsSelected()
        compose.runOnIdle { expanded = true }
        compose.onNodeWithText("Locations").assertIsSelected()
        compose.onNodeWithText("More").performClick()
        compose.runOnIdle { assertEquals("More",selected); expanded = false }
        compose.onNodeWithText("More").assertIsSelected()
        labels.forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test fun paletteAndPackagedFontIgnoreSystemDarkMode() {
        var checked = false
        compose.setContent {
            val night = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            CompositionLocalProvider(LocalConfiguration provides night) { SiloMathTheme {
            val color = MaterialTheme.colorScheme
            val typography = MaterialTheme.typography
            SideEffect {
                assertEquals(SiloMathColors.primary,color.primary)
                assertEquals(SiloMathColors.surface,color.surface)
                assertEquals(SiloMathFontFamily,typography.bodyMedium.fontFamily)
                checked = true
            }
            Text("SiloMath")
        } } }
        compose.runOnIdle { assertTrue(checked) }
    }
}
