package com.silomath.core.designsystem

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object SiloMathSpacing {
    val xs = 4.dp; val sm = 8.dp; val md = 16.dp
    val lg = 24.dp; val xl = 32.dp; val xxl = 40.dp
}
object SiloMathShapes {
    val small = RoundedCornerShape(4.dp)
    val medium = RoundedCornerShape(8.dp)
    val large = RoundedCornerShape(12.dp)
    val full = RoundedCornerShape(50)
    val material = Shapes(extraSmall = small, small = small, medium = medium,
        large = large, extraLarge = large)
}
object SiloMathDimensions {
    val minimumTouchTarget = 48.dp
    val fieldHeight = 56.dp
    val topBarHeight = 64.dp
    val border = 1.dp
    val focusedBorder = 2.dp
}
object SiloMathElevation {
    val canvas = 0.dp; val card = 0.dp; val overlay = 4.dp
}
enum class SiloMathWindowClass(val horizontalMargin: Dp) {
    Compact(16.dp), Medium(24.dp), Expanded(32.dp)
}
fun siloMathWindowClass(widthDp: Float): SiloMathWindowClass {
    require(widthDp.isFinite() && widthDp >= 0)
    return when { widthDp < 600 -> SiloMathWindowClass.Compact
        widthDp < 840 -> SiloMathWindowClass.Medium
        else -> SiloMathWindowClass.Expanded }
}

/** Re-evaluated from available bounds on resize, not a device/tablet identity. */
@Composable
fun SiloMathAdaptiveContent(modifier: Modifier = Modifier,
    content: @Composable BoxScope.(SiloMathWindowClass) -> Unit) {
    BoxWithConstraints(modifier) {
        val window = siloMathWindowClass(maxWidth.value)
        Box(Modifier.fillMaxWidth().padding(horizontal = window.horizontalMargin)) {
            content(window)
        }
    }
}

/** Use once at the screen/form owner, beneath shell-owned insets. */
fun Modifier.siloMathFormInsets() = this.imePadding()
@Composable
fun Modifier.siloMathSafeContent() = this.windowInsetsPadding(WindowInsets.safeDrawing)
fun Modifier.siloMathTouchTarget() = this.sizeIn(minWidth = 48.dp, minHeight = 48.dp)

/** Call when starting optional motion; no custom animation overrides platform scale. */
fun siloMathMotionEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
