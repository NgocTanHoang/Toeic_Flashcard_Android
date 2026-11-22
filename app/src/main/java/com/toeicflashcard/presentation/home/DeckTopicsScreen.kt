package com.toeicflashcard.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.AmbientMeshBackground
import com.toeicflashcard.core.ui.glass.CollapsibleHeaderSpring
import com.toeicflashcard.core.ui.glass.DockScrollController
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LiquidGlassSearchCapsule
import com.toeicflashcard.core.ui.glass.MainContentMaxWidth
import com.toeicflashcard.core.ui.glass.adaptiveHorizontalPadding
import com.toeicflashcard.core.ui.glass.gridItemSpacing
import com.toeicflashcard.core.ui.glass.rememberCollapsibleHeaderState
import com.toeicflashcard.core.ui.glass.screenHorizontalPadding
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.TopicProgress
import com.toeicflashcard.domain.model.VocabularyDecks
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@Composable
fun DeckTopicsScreen(
    selectedDeckId: String,
    topics: List<TopicProgress>,
    learnedCount: Int,
    totalCount: Int,
    onSelectTopic: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var topicQuery by rememberSaveable(selectedDeckId) { mutableStateOf("") }
    val contentHazeState = remember { HazeState() }
    val dockScrollController = remember { DockScrollController() }
    val collapseThresholdPx = remember(density) { with(density) { 10.dp.toPx() } }
    val expandThresholdPx = remember(density) { with(density) { 52.dp.toPx() } }
    val collapsibleHeaderState = rememberCollapsibleHeaderState(
        dockScrollController = dockScrollController,
        collapseThresholdPx = collapseThresholdPx,
        expandThresholdPx = expandThresholdPx,
        expandOnBottomOverscroll = false
    )
    val gridState = rememberLazyGridState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeightDp = with(density) {
        if (headerHeightPx > 0) headerHeightPx.toDp() else 140.dp
    }

    LaunchedEffect(gridState) {
        snapshotFlow { !gridState.canScrollBackward }
            .distinctUntilChanged()
            .collect { atTop ->
                if (atTop) {
                    collapsibleHeaderState.expand()
                }
            }
    }

    val filteredTopics = remember(topics, topicQuery) {
        val q = topicQuery.trim()
        if (q.isEmpty()) {
            topics
        } else {
            topics.filter { summary ->
                summary.topic.contains(q, ignoreCase = true) ||
                    topicSubtitleViFor(summary.topic).contains(q, ignoreCase = true)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Haze source containing AmbientMeshBackground + scrollable topic grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = contentHazeState)
        ) {
            AmbientMeshBackground(modifier = Modifier.fillMaxSize())

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sidePad = adaptiveHorizontalPadding(
                    availableWidth = maxWidth,
                    maxContentWidth = MainContentMaxWidth
                )
                val gridGap = gridItemSpacing(maxWidth)
                val columns = when {
                    maxWidth >= 900.dp -> 3
                    maxWidth >= 600.dp -> 2
                    else -> 1
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(collapsibleHeaderState.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = sidePad,
                        end = sidePad,
                        top = headerHeightDp + 10.dp,
                        bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(gridGap),
                    verticalArrangement = Arrangement.spacedBy(gridGap)
                ) {
                    if (filteredTopics.isEmpty()) {
                        item(
                            key = "__topics_empty__",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "topics_empty"
                        ) {
                            FlatSurfaceCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Không tìm thấy chủ đề phù hợp",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Thử tìm với từ khóa khác hoặc xóa bộ lọc.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = filteredTopics,
                            key = { it.topic },
                            contentType = { "topic_card" }
                        ) { topicSummary ->
                            val onTopicClick = remember(topicSummary.topic, onSelectTopic) {
                                { onSelectTopic(topicSummary.topic) }
                            }
                            TopicFlatCard(
                                topicProgress = topicSummary,
                                onClick = onTopicClick
                            )
                        }
                    }

                    item(
                        key = "__topics_nav_spacer__",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "topics_nav_spacer"
                    ) {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }

        // Layer 2: Floating Collapsible Liquid Glass Header rendered OUTSIDE Modifier.haze(contentHazeState)
        FloatingDeckTopicsHeader(
            selectedDeckId = selectedDeckId,
            learnedCount = learnedCount,
            totalCount = totalCount,
            topicCount = topics.size,
            topicQuery = topicQuery,
            isCollapsed = collapsibleHeaderState.isCollapsed,
            contentHazeState = contentHazeState,
            onTopicQueryChange = { topicQuery = it },
            onNavigateBack = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged {
                    if (headerHeightPx == 0 && it.height > 0) {
                        headerHeightPx = it.height
                    }
                }
        )
    }
}

@Composable
private fun FloatingDeckTopicsHeader(
    selectedDeckId: String,
    learnedCount: Int,
    totalCount: Int,
    topicCount: Int,
    topicQuery: String,
    isCollapsed: Boolean,
    contentHazeState: HazeState,
    onTopicQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val horizPad = screenHorizontalPadding(LocalConfiguration.current.screenWidthDp.dp)
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    var topRowHeightPx by remember { mutableIntStateOf(0) }
    val headerAnimSpec = remember(isCollapsed) {
        if (isCollapsed) {
            CollapsibleHeaderSpring
        } else {
            spring<Float>(dampingRatio = 0.92f, stiffness = 220f)
        }
    }
    val collapseProgressState = animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = headerAnimSpec,
        label = "deck_topics_header_collapse"
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
        // Frosted header backdrop: skip composition once fully collapsed to avoid redundant full-width GPU blur during scroll
        if (collapseProgressState.value < 0.99f) {
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
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = MainContentMaxWidth)
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
                        .padding(horizontal = horizPad, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiquidGlassIconButton(
                        onClick = onNavigateBack,
                        size = 42.dp,
                        enabled = !isCollapsed
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = VocabularyDecks.dashboardTitleFor(selectedDeckId),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$learnedCount/$totalCount từ đã học • $topicCount chủ đề",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            LiquidGlassSearchCapsule(
                query = topicQuery,
                placeholder = "Tìm chủ đề...",
                onQueryChange = onTopicQueryChange,
                hazeState = contentHazeState,
                floatingBlurRadius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizPad, end = horizPad, bottom = 12.dp)
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
