package com.silomath.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.silomath.core.designsystem.*

/** Presentation-safe keys supplied by core:navigation through the composition root.
 * The design system defines no route grammar, destination IDs or authorization. */
data class SiloMathNavigationItem<K>(val key: K, val label: String, val icon: ImageVector)

@Composable
fun <K> SiloMathAdaptiveNavigation(items: List<SiloMathNavigationItem<K>>, selected: K,
    onSelect: (K) -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val window = siloMathWindowClass(maxWidth.value)
        if (window == SiloMathWindowClass.Compact) {
            Scaffold(bottomBar = {
                NavigationBar(containerColor = SiloMathColors.surface) {
                    items.forEach { item -> NavigationBarItem(
                        selected == item.key, { onSelect(item.key) },
                        icon = { Icon(item.icon, null) }, label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SiloMathColors.primary,
                            selectedTextColor = SiloMathColors.primary,
                            indicatorColor = SiloMathColors.primarySoft)) }
                }
            }) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { content() } }
        } else {
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                NavigationRail(containerColor = SiloMathColors.surface) {
                    items.forEach { item -> NavigationRailItem(
                        selected == item.key, { onSelect(item.key) },
                        icon = { Icon(item.icon, null) }, label = { Text(item.label) },
                        modifier = Modifier.siloMathTouchTarget(),
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = SiloMathColors.primary,
                            selectedTextColor = SiloMathColors.primary,
                            indicatorColor = SiloMathColors.primarySoft)) }
                }
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiloMathTopAppBar(title: String, modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Column(modifier) {
        TopAppBar(title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
            navigationIcon = {
                if (onBack != null) IconButton(onBack, Modifier.siloMathTouchTarget()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }, actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SiloMathColors.surface))
        HorizontalDivider(color = SiloMathColors.border, thickness = 1.dp)
    }
}
