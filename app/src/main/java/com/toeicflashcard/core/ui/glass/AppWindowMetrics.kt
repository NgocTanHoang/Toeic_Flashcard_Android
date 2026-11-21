package com.toeicflashcard.core.ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 2-axis window classification derived from the root BoxWithConstraints viewport.
 * - [isExpandedWidth]: tablet / unfolded Z Fold width (`>= 700.dp`).
 * - [isLandscapeShort]: wide yet vertically constrained viewport (landscape phone/tablet or split-screen) that uses 2-pane study/quiz layouts.
 */
@Immutable
data class AppWindowMetrics(
    val widthDp: Dp = 411.dp,
    val heightDp: Dp = 891.dp
) {
    val isExpandedWidth: Boolean
        get() = widthDp >= 700.dp

    val isLandscapeShort: Boolean
        get() = widthDp >= 680.dp && heightDp < 650.dp
}

val LocalAppWindowMetrics = staticCompositionLocalOf { AppWindowMetrics() }
