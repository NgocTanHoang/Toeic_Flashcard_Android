package com.toeicflashcard.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.CollapsibleHeaderSpring
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassHairlineProgress
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LiquidGlassSearchCapsule
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.TopicProgress
import com.toeicflashcard.presentation.home.components.HeroGlassDashboard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlin.math.roundToInt

private val TopicCardShape = RoundedCornerShape(20.dp)

@Composable
fun HomeScreen(
    state: HomeUiState,
    topicQuery: String,
    headerHeightDp: Dp,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    onSelectDailyGoal: (Int) -> Unit,
    onStartDueReview: () -> Unit,
    onSelectTopic: (String) -> Unit
) {
    val filteredTopics = if (topicQuery.isBlank()) {
        state.topics
    } else {
        state.filteredTopics
    }

    AnimatedContent(
        targetState = state.isLoading,
        transitionSpec = {
            fadeIn(tween(durationMillis = 220)) togetherWith fadeOut(tween(durationMillis = 160))
        },
        label = "home_loading_transition",
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
                    top = headerHeightDp + 10.dp,
                    bottom = 108.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(
                    key = "__hero__",
                    contentType = "hero"
                ) {
                    HeroGlassDashboard(
                        dueTodayCount = state.dueTodayCount,
                        learnedCount = state.learnedCount,
                        totalCount = state.totalCount,
                        currentStreak = state.currentStreak,
                        hasStudiedToday = state.hasStudiedToday,
                        dailyReviewedCount = state.dailyReviewedCount,
                        dailyGoal = state.dailyGoal,
                        onSelectDailyGoal = onSelectDailyGoal,
                        onStartDueReview = onStartDueReview,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item(
                    key = "__section_header__",
                    contentType = "section_header"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chủ đề TOEIC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (topicQuery.isBlank()) {
                                "${state.topics.size} chủ đề"
                            } else {
                                "${filteredTopics.size}/${state.topics.size}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(
                    items = filteredTopics,
                    key = { it.topic },
                    contentType = { "topic_card" }
                ) { topicProgress ->
                    val onTopicClick = remember(topicProgress.topic, onSelectTopic) {
                        { onSelectTopic(topicProgress.topic) }
                    }
                    TopicGlassCard(
                        topicProgress = topicProgress,
                        onClick = onTopicClick,
                        modifier = Modifier.animateItem()
                    )
                }

                if (filteredTopics.isEmpty()) {
                    item(
                        key = "__empty_search__",
                        contentType = "empty_search"
                    ) {
                        LiquidGlassCard(
                            shape = TopicCardShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (topicQuery.isBlank() && state.totalCount == 0) {
                                    "Bộ từ \"${state.selectedDeckName}\" chưa có từ vựng nào. Hãy sang tab Kho từ bấm nút + để thêm từ mới hoặc chọn bộ từ khác."
                                } else {
                                    "Không tìm thấy chủ đề phù hợp với \"$topicQuery\"."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                }

                item(
                    key = "__nav_spacer__",
                    contentType = "nav_spacer"
                ) {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
fun FloatingGlassHomeHeader(
    selectedDeckName: String,
    totalCount: Int,
    topicCount: Int,
    topicQuery: String,
    isCollapsed: Boolean = false,
    onTopicQueryChange: (String) -> Unit,
    onOpenDeckSwitcher: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    onToggleLightDark: (currentlyDark: Boolean) -> Unit,
    contentHazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    var topRowHeightPx by remember { mutableIntStateOf(0) }
    val collapseProgressState = animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = CollapsibleHeaderSpring,
        label = "home_header_collapse"
    )
    val extraTopGapPx = remember(density) { with(density) { 8.dp.toPx() } }
    val extraSideMarginPx = remember(density) { with(density) { 16.dp.toPx() } }
    val floatingShadowPx = remember(density, glassColors.isDark) {
        with(density) { (if (glassColors.isDark) 8.dp else 10.dp).toPx() }
    }
    val ambientShadowTint = Color(0xFF0F172A).copy(alpha = if (glassColors.isDark) 0.45f else 0.16f)
    val spotShadowTint = Color(0xFF0F172A).copy(alpha = if (glassColors.isDark) 0.55f else 0.24f)

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

    Box(modifier = modifier) {
        // Layer 1: Full-bleed frosted header backdrop fading cleanly when collapsed
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = collapseProgressState.value
                    alpha = (1f - p).coerceIn(0f, 1f)
                    translationY = -topRowHeightPx * 0.5f * p
                }
                .hazeChild(
                    state = contentHazeState,
                    style = headerHazeStyle
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(glassColors.divider)
            )
        }

        // Layer 2: Collapsing top bar + Sticky floating frosted search pill
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged {
                        if (it.height > 0) topRowHeightPx = it.height
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = collapseProgressState.value
                            translationY = -topRowHeightPx * p
                            alpha = (1f - p * 1.35f).coerceIn(0f, 1f)
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeckSwitcherPill(
                        selectedDeckName = selectedDeckName,
                        totalCount = totalCount,
                        topicCount = topicCount,
                        enabled = !isCollapsed,
                        onClick = onOpenDeckSwitcher,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassIconButton(
                            onClick = onOpenReminderSettings,
                            size = 40.dp,
                            enabled = !isCollapsed
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Cài đặt nhắc nhở",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        ThemeQuickToggleGlassButton(
                            isDark = glassColors.isDark,
                            enabled = !isCollapsed,
                            onClick = { onToggleLightDark(glassColors.isDark) }
                        )
                    }
                }
            }

            LiquidGlassSearchCapsule(
                query = topicQuery,
                placeholder = "Tìm trong $topicCount chủ đề (Marketing, Thuế...)",
                onQueryChange = onTopicQueryChange,
                hazeState = contentHazeState,
                floatingBlurRadius = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 12.dp)
                    .layout { measurable, constraints ->
                        val inset = (extraSideMarginPx * collapseProgressState.value).roundToInt()
                        val narrowedMaxWidth = (constraints.maxWidth - inset * 2).coerceAtLeast(0)
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = constraints.minWidth.coerceAtMost(narrowedMaxWidth),
                                maxWidth = narrowedMaxWidth
                            )
                        )
                        val outerWidth = constraints.maxWidth.coerceAtLeast(placeable.width)
                        layout(outerWidth, placeable.height) {
                            placeable.placeRelative(
                                x = (outerWidth - placeable.width) / 2,
                                y = 0
                            )
                        }
                    }
                    .graphicsLayer {
                        val p = collapseProgressState.value
                        val travel = (topRowHeightPx - extraTopGapPx).coerceAtLeast(0f)
                        translationY = -travel * p
                        shadowElevation = floatingShadowPx * p
                        shape = CircleShape
                        clip = false
                        ambientShadowColor = ambientShadowTint
                        spotShadowColor = spotShadowTint
                    }
            )
        }
    }
}

@Composable
private fun DeckSwitcherPill(
    selectedDeckName: String,
    totalCount: Int,
    topicCount: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val pillFill = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }
    val clickModifier = if (enabled) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(pillFill)
            .border(
                width = if (glassColors.isDark) 0.75.dp else 1.dp,
                brush = glassColors.defaultBorderBrush,
                shape = RoundedCornerShape(16.dp)
            )
            .then(clickModifier)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = selectedDeckName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Chuyển bộ từ vựng",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = "SM-2 • $totalCount từ • $topicCount chủ đề",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ThemeQuickToggleGlassButton(
    isDark: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    LiquidGlassIconButton(
        onClick = onClick,
        size = 40.dp,
        enabled = enabled
    ) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 180)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(durationMillis = 180)
                )) togetherWith (fadeOut(tween(durationMillis = 120)) + scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(durationMillis = 120)
                ))
            },
            label = "quick_theme_icon_toggle"
        ) { darkActive ->
            Icon(
                imageVector = if (darkActive) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                contentDescription = if (darkActive) {
                    "Chế độ giao diện: Tối"
                } else {
                    "Chế độ giao diện: Sáng"
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TopicGlassCard(
    topicProgress: TopicProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val progress = if (topicProgress.totalCount > 0) {
        topicProgress.learnedCount.toFloat() / topicProgress.totalCount.toFloat()
    } else {
        0f
    }
    val visual = remember(topicProgress.topic) { topicVisualFor(topicProgress.topic) }
    val iconCircleBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val iconTint = MaterialTheme.colorScheme.primary

    LiquidGlassCard(
        onClick = onClick,
        pressScaleEnabled = false,
        shape = TopicCardShape,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconCircleBg)
                        .border(
                            width = if (glassColors.isDark) 0.75.dp else 1.dp,
                            brush = glassColors.defaultBorderBrush,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = topicProgress.topic,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = visual.subtitleVi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${topicProgress.learnedCount}/${topicProgress.totalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (topicProgress.learnedCount >= topicProgress.totalCount && topicProgress.totalCount > 0) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Học chủ đề ${topicProgress.topic}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            LiquidGlassHairlineProgress(
                progress = progress,
                height = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            )
        }
    }
}
