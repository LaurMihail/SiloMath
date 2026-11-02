package com.silomath.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SiloMathFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun type(size: Int, weight: Int, line: Int) = TextStyle(
    fontFamily = SiloMathFontFamily, fontSize = size.sp, fontWeight = FontWeight(weight),
    lineHeight = line.sp, letterSpacing = 0.sp
)

/** Frozen Doc21 section4 scale. Display roles reuse the largest approved size. */
val Typography = Typography(
    displayLarge = type(32, 700, 40), displayMedium = type(32, 700, 40),
    displaySmall = type(32, 700, 40),
    headlineLarge = type(32, 700, 40),
    headlineMedium = type(24, 600, 32),
    headlineSmall = type(20, 600, 28),
    titleLarge = type(18, 600, 24),
    titleMedium = type(16, 600, 24),
    titleSmall = type(14, 600, 20),
    bodyLarge = type(16, 400, 24),
    bodyMedium = type(14, 400, 20),
    bodySmall = type(12, 400, 16),
    labelLarge = type(14, 600, 20),
    labelMedium = type(12, 600, 16),
    labelSmall = type(11, 600, 16)
)

val SiloMathNumericTextStyle = Typography.bodyMedium.copy(fontFeatureSettings = "tnum")
