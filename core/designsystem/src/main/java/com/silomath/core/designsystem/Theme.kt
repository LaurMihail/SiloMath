package com.silomath.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Doc21 specifies one canonical palette. System night mode/wallpaper never replace it. */
val SiloMathColorScheme = lightColorScheme(
    primary = SiloMathColors.primary,
    onPrimary = SiloMathColors.surface,
    primaryContainer = SiloMathColors.primarySoft,
    onPrimaryContainer = SiloMathColors.primary,
    inversePrimary = SiloMathColors.primarySoft,
    secondary = SiloMathColors.textSecondary,
    onSecondary = SiloMathColors.surface,
    secondaryContainer = SiloMathColors.surfaceSubtle,
    onSecondaryContainer = SiloMathColors.textPrimary,
    tertiary = SiloMathColors.primary,
    onTertiary = SiloMathColors.surface,
    tertiaryContainer = SiloMathColors.primarySoft,
    onTertiaryContainer = SiloMathColors.primary,
    background = SiloMathColors.background,
    onBackground = SiloMathColors.textPrimary,
    surface = SiloMathColors.surface,
    onSurface = SiloMathColors.textPrimary,
    surfaceVariant = SiloMathColors.surfaceSubtle,
    onSurfaceVariant = SiloMathColors.textSecondary,
    surfaceTint = SiloMathColors.primary,
    inverseSurface = SiloMathColors.textPrimary,
    inverseOnSurface = SiloMathColors.surface,
    error = SiloMathColors.error,
    onError = SiloMathColors.surface,
    errorContainer = SiloMathColors.errorSurface,
    onErrorContainer = SiloMathColors.textPrimary,
    outline = SiloMathColors.textMuted,
    outlineVariant = SiloMathColors.border,
    scrim = SiloMathColors.textPrimary,
    surfaceBright = SiloMathColors.surface,
    surfaceDim = SiloMathColors.surfaceSubtle,
    surfaceContainer = SiloMathColors.surface,
    surfaceContainerLow = SiloMathColors.background,
    surfaceContainerLowest = SiloMathColors.surface,
    surfaceContainerHigh = SiloMathColors.surfaceSubtle,
    surfaceContainerHighest = SiloMathColors.surfaceSubtle
)

@Composable
fun SiloMathTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SiloMathColorScheme, typography = Typography,
        shapes = SiloMathShapes.material, content = content)
}
