package com.silomath.core.navigation

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/** Explicit appearance follows the rendered surface, not the device night setting.
 * The composition root supplies the canonical design-system surface appearance. */
fun ComponentActivity.configureNavigationWindow(lightSurface: Boolean) {
    val style = if (lightSurface) SystemBarStyle.light(Color.TRANSPARENT,Color.TRANSPARENT)
        else SystemBarStyle.dark(Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle=style,navigationBarStyle=style)
}
