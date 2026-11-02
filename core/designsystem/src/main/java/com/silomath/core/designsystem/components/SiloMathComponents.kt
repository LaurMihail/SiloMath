package com.silomath.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.silomath.core.designsystem.*

@Composable
fun SiloMathPrimaryButton(label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Button(onClick, modifier.heightIn(min = 48.dp), enabled, shape = SiloMathShapes.medium,
        interactionSource = interactions,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) SiloMathColors.primaryPressed else SiloMathColors.primary,
            contentColor = SiloMathColors.surface,
            disabledContainerColor = SiloMathColors.surfaceSubtle,
            disabledContentColor = SiloMathColors.textSecondary)) { Text(label) }
}

@Composable
fun SiloMathSecondaryButton(label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled,
        shape = SiloMathShapes.medium, border = BorderStroke(1.dp, SiloMathColors.textMuted),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = SiloMathColors.surface,
            contentColor = SiloMathColors.primary)) { Text(label) }
}

@Composable
fun SiloMathTextField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, errorMessage: String? = null, readOnly: Boolean = false,
    singleLine: Boolean = true, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null) {
    OutlinedTextField(value, onValueChange,
        modifier.heightIn(min = 56.dp).semantics {
            if (readOnly) stateDescription = "Read only"
            errorMessage?.let { error(it) }
        }, readOnly = readOnly, label = { Text(label) },
        isError = errorMessage != null, singleLine = singleLine,
        supportingText = errorMessage?.let { message -> { Text(message) } },
        keyboardOptions = keyboardOptions, visualTransformation = visualTransformation,
        trailingIcon = trailingIcon, leadingIcon = leadingIcon,
        shape = SiloMathShapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SiloMathColors.primary,
            unfocusedBorderColor = SiloMathColors.textMuted,
            focusedContainerColor = SiloMathColors.surface,
            unfocusedContainerColor = if (readOnly) SiloMathColors.surfaceSubtle else SiloMathColors.surface))
}

@Composable
fun SiloMathSearchField(value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, label: String = "Search") {
    SiloMathTextField(value, onValueChange, label, modifier,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) })
}

@Composable
fun SiloMathCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = SiloMathShapes.medium, color = SiloMathColors.surface,
        border = BorderStroke(1.dp, SiloMathColors.border), shadowElevation = 0.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/** Presentation categories only; callers supply actual frozen domain labels. */
enum class SiloMathTone { Neutral, Positive, Warning, Error }

@Composable
fun SiloMathStatusChip(label: String, tone: SiloMathTone,
    modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val surface = when (tone) { SiloMathTone.Neutral -> SiloMathColors.surfaceSubtle
        SiloMathTone.Positive -> SiloMathColors.successSurface
        SiloMathTone.Warning -> SiloMathColors.warningSurface
        SiloMathTone.Error -> SiloMathColors.errorSurface }
    val accent = when (tone) { SiloMathTone.Neutral -> SiloMathColors.textSecondary
        SiloMathTone.Positive -> SiloMathColors.success
        SiloMathTone.Warning -> SiloMathColors.warning
        SiloMathTone.Error -> SiloMathColors.error }
    Surface(modifier.semantics(mergeDescendants = true) {},
        shape = SiloMathShapes.small, color = surface) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(label, color = SiloMathColors.textPrimary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SiloMathSyncIndicator(label: String, tone: SiloMathTone, modifier: Modifier = Modifier) =
    SiloMathStatusChip(label, tone, modifier)

@Composable
fun SiloMathFilterChip(label: String, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    FilterChip(selected, onClick, label = { Text(label) }, modifier = modifier.heightIn(min = 48.dp),
        shape = SiloMathShapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SiloMathColors.primarySoft,
            selectedLabelColor = SiloMathColors.primary,
            labelColor = SiloMathColors.textSecondary))
}

@Composable
fun SiloMathListRow(title: String, supportingText: String?, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    Surface(onClick, modifier.heightIn(min = 48.dp), color = SiloMathColors.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            supportingText?.let { Text(it, color = SiloMathColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun SiloMathEmptyState(message: String, modifier: Modifier = Modifier) {
    Text(message, modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun SiloMathErrorState(message: String, modifier: Modifier = Modifier,
    retry: (() -> Unit)? = null) {
    Column(modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = SiloMathColors.error)
        retry?.let { SiloMathSecondaryButton("Retry", it) }
    }
}

@Composable
fun SiloMathOfflineBanner(message: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = SiloMathColors.warningSurface) {
        Text(message, Modifier.padding(16.dp), color = SiloMathColors.textPrimary)
    }
}
