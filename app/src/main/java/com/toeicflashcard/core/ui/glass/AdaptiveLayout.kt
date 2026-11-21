package com.toeicflashcard.core.ui.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AdaptiveWideBreakpoint = 600.dp
val AdaptiveCardMinWidth = 310.dp
val StudyContentMaxWidth = 560.dp
val WideStudyContentMaxWidth = 720.dp
val FocusedContentMaxWidth = 580.dp
val WideQuizContentMaxWidth = 700.dp
val MainContentMaxWidth = 1100.dp

fun screenHorizontalPadding(availableWidth: Dp): Dp = when {
    availableWidth >= 1000.dp -> 40.dp
    availableWidth >= AdaptiveWideBreakpoint -> 28.dp
    else -> 16.dp
}

fun sectionVerticalSpacing(availableWidth: Dp): Dp =
    if (availableWidth >= AdaptiveWideBreakpoint) 22.dp else 14.dp

fun gridItemSpacing(availableWidth: Dp): Dp =
    if (availableWidth >= AdaptiveWideBreakpoint) 16.dp else 10.dp

fun cardInnerPadding(availableWidth: Dp): Dp =
    if (availableWidth >= AdaptiveWideBreakpoint) 22.dp else 16.dp

/**
 * Computes symmetric horizontal padding for full-bleed [LazyColumn]s / [LazyVerticalGrid]s so scrolling gestures
 * remain active across the entire viewport while content aligns with centered [maxContentWidth].
 */
fun adaptiveHorizontalPadding(
    availableWidth: Dp,
    maxContentWidth: Dp = MainContentMaxWidth,
    basePadding: Dp = screenHorizontalPadding(availableWidth)
): Dp = ((availableWidth - maxContentWidth) / 2f).coerceAtLeast(0.dp) + basePadding

/**
 * Renders 1 or 2 items in an equal-width balanced row; adds a weighted spacer when the trailing row has 1 item.
 */
@Composable
fun <T> AdaptiveTwoColumnRow(
    pair: List<T>,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 12.dp,
    matchRowHeight: Boolean = false,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    itemContent: @Composable BoxScope.(item: T) -> Unit
) {
    val rowModifier = if (matchRowHeight) {
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    } else {
        modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = verticalAlignment
    ) {
        Box(modifier = Modifier.weight(1f)) {
            itemContent(pair[0])
        }
        if (pair.size > 1) {
            Box(modifier = Modifier.weight(1f)) {
                itemContent(pair[1])
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
