package com.toeicflashcard.core.ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 2-axis window classification derived from the root BoxWithConstraints viewport.
 * - [isNarrowCover]: Z Fold 3/4/5/6 ultra-narrow cover screens (`widthDp < 385.dp`).
 * - [isShortCover]: Z Fold 8 5.5" 10:16 cover screen (`widthDp < 600.dp && heightDp < 780.dp`).
 * - [isExpandedWidth]: tablet / unfolded Z Fold width with sufficient height (`widthDp >= 600.dp && heightDp >= 580.dp`).
 * - [isFoldInnerExpanded]: unfolded Z Fold inner display (`600.dp <= widthDp < 900.dp && heightDp >= 580.dp`).
 * - [isLandscapeShort]: wide yet vertically constrained viewport (landscape phone/tablet or split-screen) that uses 2-pane study/quiz layouts.
 */
@Immutable
data class AppWindowMetrics(
    val widthDp: Dp = 411.dp,
    val heightDp: Dp = 891.dp
) {
    val isNarrowCover: Boolean
        get() = widthDp < 385.dp

    val isShortCover: Boolean
        get() = widthDp < 600.dp && heightDp < 780.dp

    val isExpandedWidth: Boolean
        get() = widthDp >= 600.dp && heightDp >= 580.dp

    val isFoldInnerExpanded: Boolean
        get() = widthDp >= 600.dp && widthDp < 900.dp && heightDp >= 580.dp

    val foldInnerScale: Float
        get() = if (isFoldInnerExpanded) 1.08f else 1.0f

    val isLandscapeShort: Boolean
        get() = widthDp >= 680.dp && heightDp < 650.dp
}

val LocalAppWindowMetrics = staticCompositionLocalOf { AppWindowMetrics() }
