package com.toeicflashcard.presentation.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.FocusedContentMaxWidth
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassSheetSurface
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.ToeicTargetLevel
import dev.chrisbanes.haze.HazeState

private val OnboardingCardShape = RoundedCornerShape(20.dp)
private val OnboardingDailyGoals = listOf(10, 15, 20, 30)

@Composable
fun OnboardingSheet(
    visible: Boolean,
    hazeState: HazeState,
    initialDailyGoal: Int,
    initialTargetLevel: String,
    onStartLearning: (dailyGoal: Int, targetLevel: String) -> Unit,
    onSkip: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current

    BackHandler(enabled = visible) {
        onSkip()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(glassColors.sheetScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) { it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            OnboardingSheetContent(
                hazeState = hazeState,
                initialDailyGoal = initialDailyGoal,
                initialTargetLevel = initialTargetLevel,
                onStartLearning = onStartLearning,
                onSkip = onSkip
            )
        }
    }
}

@Composable
private fun OnboardingSheetContent(
    hazeState: HazeState,
    initialDailyGoal: Int,
    initialTargetLevel: String,
    onStartLearning: (dailyGoal: Int, targetLevel: String) -> Unit,
    onSkip: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    var selectedTarget by remember(initialTargetLevel) {
        mutableStateOf(ToeicTargetLevel.fromId(initialTargetLevel))
    }
    var selectedDailyGoal by remember(initialDailyGoal) {
        val normalized = if (initialDailyGoal in OnboardingDailyGoals) initialDailyGoal else 15
        mutableIntStateOf(normalized)
    }

    LiquidGlassSheetSurface(
        hazeState = hazeState,
        sheetMaxWidth = FocusedContentMaxWidth,
        modifier = Modifier
            .widthIn(max = FocusedContentMaxWidth)
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Chào mừng đến Lexora",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(onClick = onSkip) {
                    Text("Bỏ qua", fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                text = "Chọn mục tiêu học tập — có thể thay đổi bất cứ lúc nào.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Card 1: TOEIC Target Level
            LiquidGlassCard(
                shape = OnboardingCardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Mục tiêu TOEIC",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToeicTargetLevel.entries.forEach { level ->
                            val selected = selectedTarget == level
                            OnboardingOptionChip(
                                label = level.label,
                                selected = selected,
                                onClick = { selectedTarget = level },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = selectedTarget.recommendationVi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Card 2: Daily Study Goal
            LiquidGlassCard(
                shape = OnboardingCardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Cường độ học mỗi ngày",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OnboardingDailyGoals.forEach { goal ->
                            val selected = selectedDailyGoal == goal
                            OnboardingOptionChip(
                                label = "$goal từ",
                                selected = selected,
                                onClick = { selectedDailyGoal = goal },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            LiquidGlassButton(
                onClick = { onStartLearning(selectedDailyGoal, selectedTarget.id) },
                tintColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Bắt đầu học",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OnboardingOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val bg = when {
        selected && glassColors.isDark -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        glassColors.isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.62f)
    }
    val fg = when {
        selected && glassColors.isDark -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderBrush = rememberLiquidGlassBorderBrush(
        glassColors = glassColors,
        accentColor = if (selected && !glassColors.isDark) MaterialTheme.colorScheme.primary else null
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .border(width = if (glassColors.isDark) 0.75.dp else 1.dp, brush = borderBrush, shape = CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
