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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors

private val HeroCardShape = RoundedCornerShape(28.dp)
private val GoalBadgeShape = RoundedCornerShape(20.dp)
private val DailyGoalOptions = listOf(5, 10, 20)

@Composable
fun HeroGlassDashboard(
    dueTodayCount: Int,
    learnedCount: Int,
    totalCount: Int,
    currentStreak: Int,
    hasStudiedToday: Boolean,
    dailyReviewedCount: Int,
    dailyGoal: Int,
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
    val goalReached = dailyReviewedCount >= safeGoal
    val masteryPct = if (totalCount > 0) ((learnedCount.toFloat() / totalCount.toFloat()) * 100f).toInt() else 0

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = HeroCardShape,
        containerColor = glassColors.elevatedGlassFill
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CẦN ÔN HÔM NAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = dueTodayCount.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "từ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = "Đã thuộc $learnedCount/$totalCount từ ($masteryPct%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(GoalBadgeShape)
                        .clickable(
                            role = Role.Button,
                            onClick = { showGoalDialog = true }
                        )
                        .semantics {
                            contentDescription =
                                "Mục tiêu ngày $dailyReviewedCount trên $safeGoal từ, chuỗi $currentStreak ngày. Chạm để đổi mục tiêu"
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.size(84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedGoalProgress.value },
                            modifier = Modifier.size(84.dp),
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round,
                            color = if (goalReached) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = glassColors.hairlineTrack
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$dailyReviewedCount/$safeGoal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "hôm nay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val badgeFill = if (glassColors.isDark) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.White.copy(alpha = 0.56f)
                    }
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeFill)
                            .border(
                                width = if (glassColors.isDark) 0.75.dp else 0.8.dp,
                                brush = glassColors.defaultBorderBrush,
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (currentStreak > 0 || hasStudiedToday) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$currentStreak ngày",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Chỉnh mục tiêu ngày",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

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
                onClick = onStartDueReview,
                enabled = dueTodayCount > 0,
                tintColor = reviewTint,
                contentColor = reviewContentColor,
                borderColor = if (glassColors.isDark) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (dueTodayCount > 0) {
                        Icons.Rounded.PlayArrow
                    } else {
                        Icons.Rounded.CheckCircle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (dueTodayCount > 0) "Ôn tập ngay ($dueTodayCount từ)" else "Đã hoàn thành mục tiêu hôm nay",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
            modifier = Modifier.fillMaxWidth(0.88f)
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
