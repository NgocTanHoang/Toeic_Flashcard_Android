package com.toeicflashcard.presentation.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassHairlineProgress
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.core.ui.theme.LocalSm2RatingColors
import com.toeicflashcard.domain.model.VocabularyWord
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import java.util.Locale

private val StatsCardShape = RoundedCornerShape(24.dp)
private val GoalDashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

@Composable
fun StatsScreen(
    state: StatsUiState,
    viewModel: StatsViewModel,
    headerHeightDp: Dp,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    onReviewHardWords: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel) {
        viewModel.ttsMessages.collect { message ->
            onShowMessage(message)
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.stopSpeaking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopSpeaking()
        }
    }

    AnimatedContent(
        targetState = state.isLoading,
        transitionSpec = {
            fadeIn(tween(durationMillis = 200)) togetherWith fadeOut(tween(durationMillis = 140))
        },
        label = "stats_loading",
        modifier = Modifier.fillMaxSize()
    ) { loading ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeightDp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = headerHeightDp + 12.dp,
                    bottom = 108.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "__weekly_chart__", contentType = "weekly_chart") {
                    WeeklyBarChartGlassCard(
                        bars = state.last7Days,
                        dailyGoal = state.dailyGoal,
                        totalReviews7Days = state.totalReviewsLast7Days,
                        currentStreak = state.currentStreak
                    )
                }

                if (state.deckStats.isNotEmpty()) {
                    item(key = "__deck_mastery__", contentType = "deck_mastery") {
                        DeckMasteryGlassCard(
                            decks = state.deckStats,
                            learnedWords = state.learnedWords,
                            totalWords = state.totalWords
                        )
                    }
                }

                item(key = "__category_mastery__", contentType = "category_mastery") {
                    CategoryMasteryGlassCard(
                        categories = state.categoryStats,
                        learnedWords = state.learnedWords,
                        totalWords = state.totalWords
                    )
                }

                item(key = "__hardest_words__", contentType = "hardest_words") {
                    HardestWordsGlassCard(
                        hardestWords = state.hardestWords,
                        hasHardWords = state.hasHardWords,
                        canStartReview = state.totalWords > 0,
                        onSpeakWord = viewModel::speakWord,
                        onReviewHardWords = onReviewHardWords
                    )
                }

                item(key = "__stats_nav_spacer__", contentType = "nav_spacer") {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
fun FloatingStatsHeader(
    learnedWords: Int,
    totalWords: Int,
    currentStreak: Int,
    contentHazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    val headerTint = if (glassColors.isDark) {
        glassColors.hazeBackground.copy(alpha = glassOpacity)
    } else {
        Color(0xFFF4F8FF).copy(alpha = (glassOpacity * 0.85f).coerceIn(0.45f, 0.95f))
    }
    val headerHazeStyle = remember(glassColors.hazeBackground, glassColors.isDark, headerTint) {
        HazeStyle(
            backgroundColor = glassColors.hazeBackground,
            tint = HazeTint(headerTint),
            blurRadius = 16.dp,
            noiseFactor = 0.0f,
            fallbackTint = HazeTint(
                headerTint.copy(alpha = if (glassColors.isDark) 0.92f else 0.88f)
            )
        )
    }
    val masteryPct = if (totalWords > 0) ((learnedWords.toFloat() / totalWords) * 100f).toInt() else 0

    Column(
        modifier = modifier
            .hazeChild(
                state = contentHazeState,
                style = headerHazeStyle
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Thống kê học tập",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Đã thuộc $learnedWords/$totalWords từ ($masteryPct%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (glassColors.isDark) Color.White.copy(alpha = 0.08f)
                        else Color.White.copy(alpha = 0.65f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$currentStreak ngày",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(glassColors.divider)
        )
    }
}

@Composable
private fun WeeklyBarChartGlassCard(
    bars: List<DailyBarStat>,
    dailyGoal: Int,
    totalReviews7Days: Int,
    currentStreak: Int
) {
    val glassColors = LocalLiquidGlassColors.current
    val maxScale = remember(bars, dailyGoal) {
        maxOf(dailyGoal, bars.maxOfOrNull { it.count } ?: 0, 1)
    }
    val growthState = animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "stats_bar_growth"
    )
    val trackColor = glassColors.hairlineTrack
    val todayColor = MaterialTheme.colorScheme.primary
    val goalHitColor = MaterialTheme.colorScheme.secondary
    val pastBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val goalLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)

    LiquidGlassCard(
        shape = StatsCardShape,
        containerColor = glassColors.elevatedGlassFill,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Tiến độ 7 ngày gần nhất",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tổng $totalReviews7Days lượt ôn • Mục tiêu $dailyGoal từ/ngày",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (bars.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bars.forEach { bar ->
                            Text(
                                text = bar.count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (bar.isToday) todayColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .drawBehind {
                                val slotCount = bars.size.coerceAtLeast(1)
                                val slotWidth = size.width / slotCount
                                val barWidth = (slotWidth * 0.42f).coerceAtMost(24.dp.toPx())
                                val radius = CornerRadius(barWidth * 0.4f, barWidth * 0.4f)
                                val anim = growthState.value

                                val goalFraction = (dailyGoal.toFloat() / maxScale.toFloat()).coerceIn(0f, 1f)
                                val goalY = size.height * (1f - goalFraction)
                                drawLine(
                                    color = goalLineColor,
                                    start = Offset(0f, goalY),
                                    end = Offset(size.width, goalY),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = GoalDashEffect
                                )

                                bars.forEachIndexed { index, bar ->
                                    val x = index * slotWidth + (slotWidth - barWidth) * 0.5f
                                    drawRoundRect(
                                        color = trackColor,
                                        topLeft = Offset(x, 0f),
                                        size = Size(barWidth, size.height),
                                        cornerRadius = radius
                                    )

                                    val rawFraction = (bar.count.toFloat() / maxScale.toFloat()).coerceIn(0f, 1f)
                                    val barHeight = size.height * rawFraction * anim
                                    if (barHeight > 1f) {
                                        val barColor = when {
                                            bar.isToday -> todayColor
                                            bar.count >= dailyGoal -> goalHitColor
                                            else -> pastBarColor
                                        }
                                        drawRoundRect(
                                            color = barColor,
                                            topLeft = Offset(x, size.height - barHeight),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = radius
                                        )
                                    }
                                }
                            }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bars.forEach { bar ->
                            Text(
                                text = bar.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (bar.isToday) todayColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (currentStreak > 0) {
                Text(
                    text = "Chuỗi học hiện tại: $currentStreak ngày liên tiếp",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun DeckMasteryGlassCard(
    decks: List<DeckMasteryStat>,
    learnedWords: Int,
    totalWords: Int
) {
    val glassColors = LocalLiquidGlassColors.current
    LiquidGlassCard(
        shape = StatsCardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tiến độ theo Giáo trình / Bộ từ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$learnedWords/$totalWords",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            decks.forEach { stat ->
                val ratio = if (stat.totalCount > 0) {
                    stat.learnedCount.toFloat() / stat.totalCount.toFloat()
                } else {
                    0f
                }
                val pct = (ratio * 100f).toInt()

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = stat.deckName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Cần ôn: ${stat.dueCount} từ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${stat.learnedCount}/${stat.totalCount} ($pct%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (pct >= 80) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LiquidGlassHairlineProgress(
                        progress = ratio,
                        height = 4.dp,
                        trackColor = glassColors.hairlineTrack
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryMasteryGlassCard(
    categories: List<CategoryMasteryStat>,
    learnedWords: Int,
    totalWords: Int
) {
    val glassColors = LocalLiquidGlassColors.current
    LiquidGlassCard(
        shape = StatsCardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tỷ lệ thuộc theo nhóm chủ đề",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$learnedWords/$totalWords",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            categories.forEach { stat ->
                val ratio = if (stat.totalCount > 0) {
                    stat.learnedCount.toFloat() / stat.totalCount.toFloat()
                } else {
                    0f
                }
                val pct = (ratio * 100f).toInt()

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stat.category,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "${stat.learnedCount}/${stat.totalCount} ($pct%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pct >= 80) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LiquidGlassHairlineProgress(
                        progress = ratio,
                        height = 4.dp,
                        trackColor = glassColors.hairlineTrack
                    )
                }
            }
        }
    }
}

@Composable
private fun HardestWordsGlassCard(
    hardestWords: List<VocabularyWord>,
    hasHardWords: Boolean,
    canStartReview: Boolean,
    onSpeakWord: (String) -> Unit,
    onReviewHardWords: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val sm2Colors = LocalSm2RatingColors.current

    LiquidGlassCard(
        shape = StatsCardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Nhóm từ khó nhớ nhất",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Xếp theo hệ số ghi nhớ SM-2 (easeFactor) thấp nhất",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hardestWords.isEmpty()) {
                Text(
                    text = "Bạn chưa có dữ liệu ôn tập SM-2 để xếp hạng độ khó. Hãy học một phiên thẻ hoặc bấm nút bên dưới để luyện nhóm từ đầu tiên.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    hardestWords.forEach { word ->
                        val efText = String.format(Locale.US, "EF %.2f", word.easeFactor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (glassColors.isDark) Color.White.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.50f)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = word.meaningVi,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (word.easeFactor < 2.2f) sm2Colors.again.container
                                            else sm2Colors.hard.container
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = efText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (word.easeFactor < 2.2f) sm2Colors.again.content else sm2Colors.hard.content
                                    )
                                }

                                LiquidGlassIconButton(
                                    onClick = { onSpeakWord(word.word) },
                                    size = 34.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "Phát âm ${word.word}",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LiquidGlassButton(
                onClick = onReviewHardWords,
                enabled = canStartReview,
                tintColor = if (glassColors.isDark) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                },
                contentColor = if (glassColors.isDark) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                borderColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.primary,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasHardWords) "Ôn lại nhóm từ khó" else "Luyện nhóm 20 từ đầu tiên",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
