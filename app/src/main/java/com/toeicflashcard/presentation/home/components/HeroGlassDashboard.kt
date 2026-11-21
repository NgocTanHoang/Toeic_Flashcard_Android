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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
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
import com.toeicflashcard.domain.model.TopicProgress
import com.toeicflashcard.domain.usecase.GetStudyQueueUseCase
import com.toeicflashcard.presentation.home.topicVisualFor
import java.text.NumberFormat
import java.util.Locale

private val HeroCardShape = RoundedCornerShape(26.dp)
private val ShortcutRowShape = RoundedCornerShape(14.dp)
private val DailyGoalOptions = listOf(5, 10, 20)

@OptIn(ExperimentalLayoutApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun HeroGlassDashboard(
    reviewDueCount: Int,
    newWordsCount: Int,
    learnedCount: Int,
    totalCount: Int,
    currentStreak: Int,
    hasStudiedToday: Boolean,
    dailyReviewedCount: Int,
    dailyGoal: Int,
    continueTopic: TopicProgress?,
    isWide: Boolean,
    onSelectDailyGoal: (Int) -> Unit,
    onStartDueReview: () -> Unit,
    onSelectTopic: (String) -> Unit,
    onOpenStandardQuiz: () -> Unit = {},
    onOpenListeningQuiz: () -> Unit = {},
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
    val goalPercent = (goalProgress * 100f).toInt()
    val extraWords = (dailyReviewedCount - safeGoal).coerceAtLeast(0)

    val sessionLimit = GetStudyQueueUseCase.DEFAULT_DAILY_SESSION_LIMIT
    val ctaEnabled = reviewDueCount > 0 || newWordsCount > 0
    val ctaText = when {
        reviewDueCount > 0 -> "Ôn tập đến hạn (${minOf(reviewDueCount, sessionLimit)} từ)"
        newWordsCount > 0 -> "Học từ mới (${minOf(newWordsCount, sessionLimit)} từ)"
        else -> "Đã hoàn thành mục tiêu hôm nay"
    }
    val viFormatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")) }
    val summaryLine =
        "Đến hạn: ${viFormatter.format(reviewDueCount)} • Mới: ${viFormatter.format(newWordsCount)} • Thuộc: ${viFormatter.format(learnedCount)}/${viFormatter.format(totalCount)}"

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = HeroCardShape,
        containerColor = glassColors.elevatedGlassFill
    ) {
        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroHeaderLabel()

                    HeroProgressRow(
                        isWide = true,
                        animatedProgress = animatedGoalProgress.value,
                        goalReached = goalReached,
                        goalPercent = goalPercent,
                        dailyReviewedCount = dailyReviewedCount,
                        safeGoal = safeGoal,
                        extraWords = extraWords
                    )

                    Text(
                        text = summaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroStreakGoalPill(
                        currentStreak = currentStreak,
                        hasStudiedToday = hasStudiedToday,
                        dailyReviewedCount = dailyReviewedCount,
                        safeGoal = safeGoal,
                        onClick = { showGoalDialog = true }
                    )

                    HeroPrimaryCtaButton(
                        ctaText = ctaText,
                        enabled = ctaEnabled,
                        onClick = onStartDueReview,
                        modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)
                    )

                    if (continueTopic != null) {
                        ContinueTopicShortcutRow(
                            continueTopic = continueTopic,
                            onSelectTopic = onSelectTopic,
                            modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroHeaderLabel(modifier = Modifier.weight(1f))
                    HeroStreakGoalPill(
                        currentStreak = currentStreak,
                        hasStudiedToday = hasStudiedToday,
                        dailyReviewedCount = dailyReviewedCount,
                        safeGoal = safeGoal,
                        onClick = { showGoalDialog = true }
                    )
                }

                HeroProgressRow(
                    isWide = false,
                    animatedProgress = animatedGoalProgress.value,
                    goalReached = goalReached,
                    goalPercent = goalPercent,
                    dailyReviewedCount = dailyReviewedCount,
                    safeGoal = safeGoal,
                    extraWords = extraWords
                )

                Text(
                    text = summaryLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                HeroPrimaryCtaButton(
                    ctaText = ctaText,
                    enabled = ctaEnabled,
                    onClick = onStartDueReview
                )

                if (continueTopic != null) {
                    ContinueTopicShortcutRow(
                        continueTopic = continueTopic,
                        onSelectTopic = onSelectTopic
                    )
                }
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
private fun HeroHeaderLabel(modifier: Modifier = Modifier) {
    Text(
        text = "MỤC TIÊU HÔM NAY",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun HeroStreakGoalPill(
    currentStreak: Int,
    hasStudiedToday: Boolean,
    dailyReviewedCount: Int,
    safeGoal: Int,
    onClick: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val badgeFill = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.62f)
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
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription =
                    "Chuỗi $currentStreak ngày, mục tiêu $dailyReviewedCount trên $safeGoal từ. Chạm để đổi mục tiêu"
            }
            .padding(horizontal = 10.dp, vertical = 4.dp),
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
            modifier = Modifier.size(14.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroProgressRow(
    isWide: Boolean,
    animatedProgress: Float,
    goalReached: Boolean,
    goalPercent: Int,
    dailyReviewedCount: Int,
    safeGoal: Int,
    extraWords: Int
) {
    val glassColors = LocalLiquidGlassColors.current
    val ringSize = if (isWide) 72.dp else 64.dp
    val ringStroke = if (isWide) 5.5.dp else 5.dp
    val countStyle = if (isWide) {
        MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp)
    } else {
        MaterialTheme.typography.headlineMedium
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(ringSize),
                strokeWidth = ringStroke,
                strokeCap = StrokeCap.Round,
                color = if (goalReached) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = glassColors.hairlineTrack
            )
            if (goalReached) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Đạt mục tiêu hôm nay",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(if (isWide) 28.dp else 26.dp)
                )
            } else {
                Text(
                    text = "$goalPercent%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "$dailyReviewedCount / $safeGoal",
                        style = countStyle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "từ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                if (goalReached) {
                    val badgeText = if (extraWords > 0) {
                        "Vượt chỉ tiêu +$extraWords"
                    } else {
                        "Hoàn thành"
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(
                                    alpha = if (glassColors.isDark) 0.18f else 0.14f
                                )
                            )
                            .border(
                                width = 0.75.dp,
                                color = MaterialTheme.colorScheme.secondary.copy(
                                    alpha = if (glassColors.isDark) 0.42f else 0.50f
                                ),
                                shape = CircleShape
                            )
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
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
private fun ContinueTopicShortcutRow(
    continueTopic: TopicProgress,
    onSelectTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val visual = remember(continueTopic.topic) { topicVisualFor(continueTopic.topic) }
    val shortcutFill = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.50f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShortcutRowShape)
            .background(shortcutFill)
            .border(
                width = if (glassColors.isDark) 0.75.dp else 0.9.dp,
                brush = glassColors.defaultBorderBrush,
                shape = ShortcutRowShape
            )
            .clickable(
                role = Role.Button,
                onClick = { onSelectTopic(continueTopic.topic) }
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Tiếp tục: ${continueTopic.topic} (${continueTopic.learnedCount}/${continueTopic.totalCount})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "Học tiếp ${continueTopic.topic}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
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
