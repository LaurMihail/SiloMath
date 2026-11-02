package com.silomath.core.designsystem

import androidx.compose.ui.graphics.Color

/** Exact frozen Doc21 section3 tokens. Semantic accents are not normal-text colors. */
object SiloMathColors {
    val background = Color(0xFFF8FAFC)
    val surface = Color(0xFFFFFFFF)
    val surfaceSubtle = Color(0xFFF1F5F9)
    val border = Color(0xFFE2E8F0)
    val borderStrong = Color(0xFFCBD5E1)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF475569)
    val textMuted = Color(0xFF64748B)
    val disabled = Color(0xFF94A3B8)
    val primary = Color(0xFF0F3460)
    val primaryPressed = Color(0xFF0A2342)
    val primarySoft = Color(0xFFE0E7FF)
    val success = Color(0xFF059669)
    val successSurface = Color(0xFFECFDF5)
    val warning = Color(0xFFD97706)
    val warningSurface = Color(0xFFFFFBEB)
    val error = Color(0xFFDC2626)
    val errorSurface = Color(0xFFFEF2F2)
}

// Source-compatible aliases for existing consumers; no independent legacy palette.
val EnterpriseNavy = SiloMathColors.primary
val EnterpriseSteel = SiloMathColors.textSecondary
