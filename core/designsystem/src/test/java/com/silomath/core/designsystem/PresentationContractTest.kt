package com.silomath.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class PresentationContractTest {
    private fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble(); val y = b.luminance().toDouble()
        return (maxOf(x,y)+0.05)/(minOf(x,y)+0.05)
    }
    @Test fun readableTextOnEveryUsedSemanticSurface() {
        val surfaces = listOf(SiloMathColors.surface, SiloMathColors.background,
            SiloMathColors.surfaceSubtle, SiloMathColors.successSurface,
            SiloMathColors.warningSurface, SiloMathColors.errorSurface)
        for (surface in surfaces) {
            assertTrue(contrast(SiloMathColors.textPrimary, surface) >= 4.5)
            assertTrue(contrast(SiloMathColors.textSecondary, surface) >= 4.5)
        }
        assertTrue(contrast(SiloMathColors.surface, SiloMathColors.primary) >= 4.5)
        assertTrue(contrast(SiloMathColors.surface, SiloMathColors.primaryPressed) >= 4.5)
        assertTrue(contrast(SiloMathColors.error, SiloMathColors.surface) >= 4.5)
    }
    @Test fun focusAndActionBoundariesHaveNonTextContrast() {
        assertTrue(contrast(SiloMathColors.primary, SiloMathColors.surface) >= 3)
        assertTrue(contrast(SiloMathColors.textMuted, SiloMathColors.surface) >= 3)
    }
    @Test fun currentWindowBoundsControlMarginsAcrossResizing() {
        val widths = listOf(320f, 599.99f, 600f, 839.99f, 840f, 1200f, 599f)
        val expected = listOf(16f,16f,24f,24f,32f,32f,16f)
        widths.zip(expected).forEach { (width,margin) ->
            assertEquals(margin,siloMathWindowClass(width).horizontalMargin.value,0f)
        }
    }
}
