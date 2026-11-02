package com.silomath.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.silomath.core.designsystem.components.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** TEST-ONLY visual reference, not a Production LOC-01 implementation or data source.
 * Implements the Doc06 LOC-01 presentation inventory with controlled fixture rows;
 * does not certify authorization, navigation or business mutations. */
class DesignReferenceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun locationsReferenceCoversCanonicalControlsAndExportsImage() {
        compose.setContent { SiloMathTheme { LocationsReference() } }
        compose.onNodeWithText("Locations").assertIsDisplayed()
        compose.onNodeWithText("Search").assertExists()
        compose.onNodeWithText("Lifecycle Filter").assertExists()
        compose.onNodeWithText("Add Location").assertHasClickAction()
        compose.onNodeWithText("List").assertIsSelected()
        compose.onNodeWithText("Map").assertHasClickAction()
        compose.onNodeWithText("Fixture Location A").assertHasClickAction()
        compose.onNodeWithText("Fixture Location B").assertHasClickAction()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let { File(it) } ?: File(context.getExternalFilesDir(null),"ui007-reference")
        check(directory.isDirectory || directory.mkdirs())
        File(directory,"loc01-compact.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG,100,it))
        }
    }
}

@Composable
private fun LocationsReference() {
    Scaffold(topBar = { SiloMathTopAppBar("Locations") }) { padding ->
        SiloMathAdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SiloMathSpacing.md)) {
                SiloMathSearchField("", {}, Modifier.fillMaxWidth())
                Text("Lifecycle Filter", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Active","Inactive","All").forEach { SiloMathFilterChip(it,it=="Active",{}) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SiloMathFilterChip("List",true,{})
                    SiloMathFilterChip("Map",false,{})
                }
                SiloMathCard(Modifier.fillMaxWidth()) {
                    SiloMathListRow("Fixture Location A","LOC-A",{})
                    SiloMathStatusChip("Active",SiloMathTone.Neutral)
                    HorizontalDivider(color = SiloMathColors.border)
                    SiloMathListRow("Fixture Location B","LOC-B",{})
                    SiloMathStatusChip("Active",SiloMathTone.Neutral)
                }
                SiloMathPrimaryButton("Add Location",{},Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
