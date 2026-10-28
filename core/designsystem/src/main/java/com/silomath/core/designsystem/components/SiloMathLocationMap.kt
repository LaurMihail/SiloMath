package com.silomath.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.silomath.core.designsystem.*

/** Provider-neutral visual host. The feature/provider places geographic markers;
 * this component never authorizes, filters, geocodes, tracks or persists Locations.
 * Accessible list access and required attribution remain outside provider gestures. */
@Composable
fun SiloMathLocationMap(basemapAvailable: Boolean, onShowList: () -> Unit,
    modifier: Modifier = Modifier, attribution: String? = null,
    content: @Composable BoxScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!basemapAvailable) SiloMathOfflineBanner("Map unavailable. Locations remain available in List.")
        Surface(Modifier.fillMaxWidth().weight(1f), color = SiloMathColors.surfaceSubtle,
            border = BorderStroke(1.dp, SiloMathColors.border)) {
            Box(Modifier.fillMaxSize(), content = content)
        }
        attribution?.let { Text(it, style = MaterialTheme.typography.bodySmall,
            color = SiloMathColors.textSecondary) }
        SiloMathSecondaryButton("Show list", onShowList)
    }
}

@Composable
fun SiloMathLocationMarker(label: String, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    Surface(onClick, modifier.siloMathTouchTarget().semantics {
        this.selected = selected
        role = Role.Button
        stateDescription = if (selected) "Selected" else "Not selected"
    }, shape = SiloMathShapes.medium,
        color = if (selected) SiloMathColors.primary else SiloMathColors.surface,
        contentColor = if (selected) SiloMathColors.surface else SiloMathColors.textPrimary,
        border = BorderStroke(if (selected) 2.dp else 1.dp, SiloMathColors.primary)) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge)
    }
}
