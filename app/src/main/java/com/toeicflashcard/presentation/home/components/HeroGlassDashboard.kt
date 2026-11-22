package com.toeicflashcard.presentation.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassDialogSurface
import com.toeicflashcard.core.ui.glass.LiquidGlassHairlineProgress
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.usecase.GetStudyQueueUseCase
import java.text.NumberFormat
import java.util.Locale

private val HeroCardShape = RoundedCornerShape(24.dp)
private val MetricTileShape = RoundedCornerShape(14.dp)
private val DailyGoalOptions = listOf(10, 15, 20, 30)

@Composable
fun HeroGlassDashboard(
    reviewDueCount: Int,
    newWordsCount: Int,
    dailyReviewedCount: Int,
    dailyGoal: Int,
    isWide: Boolean,
    onSelectDailyGoal: (Int) -> Unit,
    onStartDueReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    val glassColors = LocalLiquidGlassColors.current
    val safeGoal = dailyGoal.coerceAtLeast(1)
    val goalProgress = (dailyReviewedCount.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val animatedGoalProgress = animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "hero_goal_progress"
    )
    val goalPercent = (goalProgress * 100f).toInt()
    val viFormatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")) }

    val sessionLimit = GetStudyQueueUseCase.DEFAULT_DAILY_SESSION_LIMIT
    val ctaEnabled = reviewDueCount > 0 || newWordsCount > 0
    val ctaText = when {
        reviewDueCount > 0 -> "Bắt đầu ôn tập (${minOf(reviewDueCount, sessionLimit)} từ)"
        newWordsCount > 0 -> "Học ${minOf(newWordsCount, sessionLimit)} từ mới"
        else -> "Đã hoàn thành hôm nay"
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = HeroCardShape,
        containerColor = glassColors.elevatedGlassFill
    ) {
        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DailyOverviewHeader(
                        goalPercent = goalPercent,
                        goalReached = dailyReviewedCount >= safeGoal
                    )

                    DailyThreeMetricsRow(
                        reviewDueCount = reviewDueCount,
                        dailyReviewedCount = dailyReviewedCount,
                        dailyGoal = safeGoal,
                        viFormatter = viFormatter,
                        onGoalClick = { showGoalDialog = true }
                    )

                    DailyProgressBar(
                        progress = animatedGoalProgress.value,
                        goalPercent = goalPercent
                    )
                }

                Column(
                    modifier = Modifier.weight(0.95f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    HeroPrimaryCtaButton(
                        ctaText = ctaText,
                        enabled = ctaEnabled,
                        onClick = onStartDueReview,
                        modifier = Modifier.widthIn(min = 220.dp, max = 300.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DailyOverviewHeader(
                    goalPercent = goalPercent,
                    goalReached = dailyReviewedCount >= safeGoal
                )

                DailyThreeMetricsRow(
                    reviewDueCount = reviewDueCount,
                    dailyReviewedCount = dailyReviewedCount,
                    dailyGoal = safeGoal,
                    viFormatter = viFormatter,
                    onGoalClick = { showGoalDialog = true }
                )

                DailyProgressBar(
                    progress = animatedGoalProgress.value,
                    goalPercent = goalPercent
                )

                HeroPrimaryCtaButton(
                    ctaText = ctaText,
                    enabled = ctaEnabled,
                    onClick = onStartDueReview
                )
            }
        }
    }

    if (showGoalDialog) {
        DailyGoalPickerDialog(
            currentGoal = dailyGoal,
            onSelectGoal = { selected ->
                onSelectDailyGoal(selected)
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
        )
    }
}

@Composable
private fun DailyOverviewHeader(
    goalPercent: Int,
    goalReached: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "TIẾN ĐỘ HÔM NAY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )
        Text(
            text = if (goalReached) "Đạt mục tiêu • $goalPercent%" else "$goalPercent% mục tiêu",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (goalReached) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
private fun DailyThreeMetricsRow(
    reviewDueCount: Int,
    dailyReviewedCount: Int,
    dailyGoal: Int,
    viFormatter: NumberFormat,
    onGoalClick: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val tileBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.56f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewMetricTile(
            label = "Cần ôn",
            value = viFormatter.format(reviewDueCount),
            valueColor = if (reviewDueCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            backgroundColor = tileBg,
            modifier = Modifier.weight(1f)
        )
        OverviewMetricTile(
            label = "Đã học",
            value = viFormatter.format(dailyReviewedCount),
            valueColor = MaterialTheme.colorScheme.secondary,
            backgroundColor = tileBg,
            modifier = Modifier.weight(1f)
        )
        OverviewMetricTile(
            label = "Mục tiêu",
            value = viFormatter.format(dailyGoal),
            valueColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = tileBg,
            editable = true,
            onClick = onGoalClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverviewMetricTile(
    label: String,
    value: String,
    valueColor: Color,
    backgroundColor: Color,
    editable: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val clickModifier = if (onClick != null) {
        Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "$label $value. Chạm để chỉnh mục tiêu"
            }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .clip(MetricTileShape)
            .background(backgroundColor)
            .border(
                width = 0.75.dp,
                brush = glassColors.defaultBorderBrush,
                shape = MetricTileShape
            )
            .then(clickModifier)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (editable) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyProgressBar(
    progress: Float,
    goalPercent: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LiquidGlassHairlineProgress(
            progress = progress,
            height = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Tiến độ mục tiêu hôm nay $goalPercent phần trăm"
                }
        )
    }
}

@Composable
private fun HeroPrimaryCtaButton(
    ctaText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val reviewTint = if (glassColors.isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }
    val reviewContentColor = if (glassColors.isDark) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    LiquidGlassButton(
        onClick = onClick,
        enabled = enabled,
        tintColor = reviewTint,
        contentColor = reviewContentColor,
        borderColor = if (glassColors.isDark) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.primary
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (enabled) {
                Icons.Rounded.PlayArrow
            } else {
                Icons.Rounded.CheckCircle
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = ctaText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyGoalPickerDialog(
    currentGoal: Int,
    onSelectGoal: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassDialogSurface(
            blurRadius = 18.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Mục tiêu học mỗi ngày",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Chọn số từ vựng bạn muốn ôn tập mỗi ngày:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DailyGoalOptions.forEach { goalOption ->
                        val isSelected = currentGoal == goalOption
                        val tint = when {
                            isSelected && glassColors.isDark -> MaterialTheme.colorScheme.primary
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                            glassColors.isDark -> Color.White.copy(alpha = 0.08f)
                            else -> Color.White.copy(alpha = 0.50f)
                        }
                        val fg = when {
                            isSelected && glassColors.isDark -> MaterialTheme.colorScheme.onPrimary
                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        LiquidGlassButton(
                            onClick = { onSelectGoal(goalOption) },
                            tintColor = tint,
                            contentColor = fg,
                            borderColor = if (isSelected && !glassColors.isDark) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                null
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$goalOption từ",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Đóng")
                    }
                }
            }
        }
    }
}
