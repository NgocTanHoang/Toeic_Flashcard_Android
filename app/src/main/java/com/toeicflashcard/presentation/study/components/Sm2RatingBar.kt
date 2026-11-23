package com.toeicflashcard.presentation.study.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toeicflashcard.core.ui.glass.AdaptiveWideBreakpoint
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LocalAppWindowMetrics
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.core.ui.theme.LocalSm2RatingColors
import com.toeicflashcard.core.ui.theme.Sm2ColorPair
import com.toeicflashcard.domain.model.ReviewQuality

private val Sm2PillShape = RoundedCornerShape(20.dp)

@Composable
fun Sm2RatingBar(
    visible: Boolean,
    intervalPreviews: Map<ReviewQuality, String>,
    onSelectQuality: (ReviewQuality) -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    isLargeVertical: Boolean = false
) {
    val sm2Colors = LocalSm2RatingColors.current
    val config = LocalConfiguration.current
    val windowMetrics = LocalAppWindowMetrics.current
    val effectiveWidth = maxOf(config.screenWidthDp.dp, windowMetrics.widthDp)
    val effectiveHeight = minOf(config.screenHeightDp.dp, windowMetrics.heightDp)
    val isWide = effectiveWidth >= AdaptiveWideBreakpoint
    val isNarrowCover = windowMetrics.isNarrowCover || effectiveWidth < 385.dp
    val isShortCover = windowMetrics.isShortCover || (effectiveWidth < AdaptiveWideBreakpoint && effectiveHeight < 780.dp)
    val isFoldInner = windowMetrics.isFoldInnerExpanded ||
        (effectiveWidth >= AdaptiveWideBreakpoint && effectiveWidth < 900.dp && effectiveHeight >= 580.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (isVertical) {
            Column(
                modifier = if (isLargeVertical) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isLargeVertical) 12.dp else 10.dp)
            ) {
                ReviewQuality.entries.forEach { quality ->
                    val palette = when (quality) {
                        ReviewQuality.AGAIN -> sm2Colors.again
                        ReviewQuality.HARD -> sm2Colors.hard
                        ReviewQuality.GOOD -> sm2Colors.good
                        ReviewQuality.EASY -> sm2Colors.easy
                    }
                    Sm2GlassPillButton(
                        quality = quality,
                        label = quality.labelVi,
                        intervalHint = intervalPreviews[quality].orEmpty(),
                        colors = palette,
                        onClick = { onSelectQuality(quality) },
                        isVerticalLayout = true,
                        isLargeVertical = isLargeVertical,
                        modifier = if (isLargeVertical) {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    when {
                        isWide -> 14.dp
                        isNarrowCover -> 6.dp
                        else -> 7.dp
                    }
                )
            ) {
                ReviewQuality.entries.forEach { quality ->
                    val palette = when (quality) {
                        ReviewQuality.AGAIN -> sm2Colors.again
                        ReviewQuality.HARD -> sm2Colors.hard
                        ReviewQuality.GOOD -> sm2Colors.good
                        ReviewQuality.EASY -> sm2Colors.easy
                    }
                    Sm2GlassPillButton(
                        quality = quality,
                        label = quality.labelVi,
                        intervalHint = intervalPreviews[quality].orEmpty(),
                        colors = palette,
                        onClick = { onSelectQuality(quality) },
                        isVerticalLayout = false,
                        isShortCover = isShortCover,
                        isFoldInner = isFoldInner,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Sm2GlassPillButton(
    quality: ReviewQuality,
    label: String,
    intervalHint: String,
    colors: Sm2ColorPair,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVerticalLayout: Boolean = false,
    isLargeVertical: Boolean = false,
    isShortCover: Boolean = false,
    isFoldInner: Boolean = false
) {
    val glassColors = LocalLiquidGlassColors.current
    val isDark = glassColors.isDark

    val containerColor: Color
    val labelColor: Color
    val intervalColor: Color
    val borderBrush: Brush
    val shadowModifier: Modifier

    if (!isDark) {
        val (accent, labelTint, intervalTint) = when (quality) {
            ReviewQuality.AGAIN -> Triple(
                Color(0xFFEF4444),
                Color(0xFFDC2626),
                Color(0xFF991B1B).copy(alpha = 0.82f)
            )
            ReviewQuality.HARD -> Triple(
                Color(0xFFF59E0B),
                Color(0xFFD97706),
                Color(0xFF92400E).copy(alpha = 0.84f)
            )
            ReviewQuality.GOOD -> Triple(
                Color(0xFF2563EB),
                Color(0xFF1D4ED8),
                Color(0xFF1E3A8A).copy(alpha = 0.82f)
            )
            ReviewQuality.EASY -> Triple(
                Color(0xFF10B981),
                Color(0xFF059669),
                Color(0xFF065F46).copy(alpha = 0.84f)
            )
        }
        containerColor = Color.White.copy(alpha = 0.92f)
        labelColor = labelTint
        intervalColor = intervalTint
        borderBrush = remember(accent) {
            Brush.linearGradient(
                colors = listOf(
                    accent.copy(alpha = 0.85f),
                    accent.copy(alpha = 0.55f)
                )
            )
        }
        shadowModifier = Modifier.shadow(
            elevation = 4.dp,
            shape = Sm2PillShape,
            ambientColor = accent.copy(alpha = 0.14f),
            spotColor = accent.copy(alpha = 0.22f)
        )
    } else {
        containerColor = colors.container
        labelColor = colors.content
        intervalColor = colors.content.copy(alpha = 0.85f)
        borderBrush = remember(colors.content) {
            Brush.linearGradient(
                colors = listOf(
                    colors.content.copy(alpha = 0.60f),
                    colors.content.copy(alpha = 0.24f)
                )
            )
        }
        shadowModifier = Modifier
    }

    var labelFontSizeSp by remember(label, isFoldInner) {
        mutableFloatStateOf(if (isFoldInner) 14f else 13f)
    }

    val pillHeight = when {
        isVerticalLayout && isLargeVertical -> 76.dp
        isVerticalLayout -> 54.dp
        isFoldInner -> 68.dp
        isShortCover -> 56.dp
        else -> 64.dp
    }

    val sizeModifier = if (isVerticalLayout && isLargeVertical) {
        Modifier.heightIn(min = 68.dp).fillMaxSize()
    } else {
        Modifier.height(pillHeight)
    }

    LiquidGlassCard(
        onClick = onClick,
        shape = Sm2PillShape,
        containerColor = containerColor,
        borderWidth = if (isDark) 0.9.dp else 1.dp,
        borderBrush = borderBrush,
        modifier = modifier
            .then(sizeModifier)
            .then(shadowModifier)
    ) {
        Box(
            modifier = Modifier
                .then(if (isVerticalLayout && isLargeVertical) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(pillHeight))
                .padding(horizontal = if (isVerticalLayout) 16.dp else 4.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isVerticalLayout) {
                val hintSubtitle = when (quality) {
                    ReviewQuality.AGAIN -> "Lặp lại ngay"
                    ReviewQuality.HARD -> "Ôn sớm"
                    ReviewQuality.GOOD -> "Đúng nhịp"
                    ReviewQuality.EASY -> "Giãn cách dài"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = labelColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isLargeVertical) {
                            Text(
                                text = hintSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = intervalColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = intervalHint,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = intervalColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = labelFontSizeSp.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result ->
                            if (result.hasVisualOverflow && labelFontSizeSp > 10.5f) {
                                labelFontSizeSp = (labelFontSizeSp - 0.5f).coerceAtLeast(10.5f)
                            }
                        }
                    )
                    Text(
                        text = intervalHint,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = intervalColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
