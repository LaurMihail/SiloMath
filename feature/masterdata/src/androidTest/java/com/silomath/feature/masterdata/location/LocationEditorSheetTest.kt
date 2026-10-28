package com.silomath.feature.masterdata.location

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.silomath.core.designsystem.SiloMathTheme
import com.silomath.core.domain.model.CountryCatalog
import org.junit.*
import org.junit.Assert.*

class LocationEditorSheetTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun countrySelectorUsesLocalOptionsAndEmitsCodeNotLabel() {
        var state by mutableStateOf(LocationEditorState(loading=false,allowed=true))
        compose.setContent {SiloMathTheme {
            LocationEditorContent(state,false,{state=state.copy(draft=it)},{},{}){_,_,_->Text("Map fixture")}
        }}
        compose.onNodeWithText("Select Country").performScrollTo().performClick()
        compose.onNodeWithText("Search Countries").performTextInput("Romania")
        compose.onNode(hasText("Romania") and hasText("RO")).performClick()
        compose.runOnIdle {assertEquals("RO",state.draft.country);assertEquals(249,CountryCatalog.options.size)}
        compose.onNodeWithText("Romania").assertExists()
    }
    @Test fun clearingCoordinatesIsAtomicAndCancelDoesNotSave() {
        var state by mutableStateOf(LocationEditorState(draft=LocationDraft(name="North",code="N",latitude="45",longitude="25"),loading=false,allowed=true))
        var saved=false;var cancelled=false
        compose.setContent {SiloMathTheme {
            LocationEditorContent(state,true,{state=state.copy(draft=it)},{saved=true},{cancelled=true}){_,_,_->Text("Map fixture")}
        }}
        compose.onNodeWithText("Clear Map Position").performScrollTo().performClick()
        compose.runOnIdle {assertEquals("",state.draft.latitude);assertEquals("",state.draft.longitude)}
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.runOnIdle {assertTrue(cancelled);assertFalse(saved)}
    }
    @Test fun protectedCodeCannotBeEditedAndHistoricalCountryIsNotNormalized() {
        val state=LocationEditorState(LocationDraft(name="North",code="N",country="Romania"),loading=false,allowed=true,codeLocked=true)
        compose.setContent {SiloMathTheme {LocationEditorContent(state,true,{},{},{}){_,_,_->Text("Map fixture")}}}
        compose.onNodeWithText("Location Code").assertIsNotEnabled()
        compose.onNodeWithText("Historical Country: Romania").assertExists()
        compose.onNodeWithText("Historical Country value is preserved unchanged. A separate audit is required before migration.").assertExists()
    }
    @Test fun unauthorizedStateExposesNoMutationFieldsOrSave() {
        compose.setContent {SiloMathTheme {LocationEditorContent(LocationEditorState(loading=false,error="Admin required"),false,{},{},{}){_,_,_->}}}
        compose.onNodeWithText("Location Name").assertDoesNotExist()
        compose.onNodeWithText("Save").assertDoesNotExist()
        compose.onNodeWithText("Admin required").assertExists()
        compose.onNodeWithText("Cancel").assertIsEnabled()
    }
}
