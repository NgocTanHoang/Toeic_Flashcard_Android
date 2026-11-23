package com.toeicflashcard.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toeicflashcard.core.ui.glass.AdaptiveCardMinWidth
import com.toeicflashcard.core.ui.glass.AdaptiveWideBreakpoint
import com.toeicflashcard.core.ui.glass.CollapsibleHeaderSpring
import com.toeicflashcard.core.ui.glass.LiquidGlassHairlineProgress
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LiquidGlassSearchCapsule
import com.toeicflashcard.core.ui.glass.LocalAppWindowMetrics
import com.toeicflashcard.core.ui.glass.MainContentMaxWidth
import com.toeicflashcard.core.ui.glass.adaptiveHorizontalPadding
import com.toeicflashcard.core.ui.glass.gridItemSpacing
import com.toeicflashcard.core.ui.glass.screenHorizontalPadding
import com.toeicflashcard.core.ui.glass.sectionVerticalSpacing
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.DeckProgress
import com.toeicflashcard.domain.model.TopicProgress
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.presentation.home.components.HeroGlassDashboard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import java.text.NumberFormat
import java.util.Locale

private val FlatDeckCardShape = RoundedCornerShape(14.dp)

@Composable
fun HomeScreen(
    state: HomeUiState,
    topicQuery: String,
    headerHeightDp: Dp,
    gridState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection,
    onSelectDeck: (String) -> Unit = {},
    onOpenDeckTopics: (String) -> Unit = {},
    onSelectDailyGoal: (Int) -> Unit,
    onStartDueReview: () -> Unit,
    onSelectTopic: (String) -> Unit
) {
    val filteredTopics = if (topicQuery.isBlank()) {
        state.topics
    } else {
        state.filteredTopics
    }

    val activeDeck = remember(state.decks, state.selectedDeckId) {
        val targetId = if (state.selectedDeckId == VocabularyDecks.ALL_ID) {
            VocabularyDecks.DEFAULT_SELECTED_DECK_ID
        } else {
            state.selectedDeckId
        }
        state.decks.firstOrNull { it.deckId == targetId }
            ?: state.decks.firstOrNull { it.deckId == VocabularyDecks.DEFAULT_SELECTED_DECK_ID }
    }

    val discoverDecks = remember(state.decks, activeDeck) {
        val activeId = activeDeck?.deckId ?: VocabularyDecks.DEFAULT_SELECTED_DECK_ID
        VocabularyDecks.orderedCurriculumIds
            .filter { it != activeId }
            .mapNotNull { deckId -> state.decks.firstOrNull { it.deckId == deckId } }
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= AdaptiveWideBreakpoint
                val isTabletLandscape = maxWidth >= 900.dp
                val sidePad = adaptiveHorizontalPadding(
                    availableWidth = maxWidth,
                    maxContentWidth = MainContentMaxWidth
                )
                val gridGap = gridItemSpacing(maxWidth)
                val sectionGap = sectionVerticalSpacing(maxWidth)

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = AdaptiveCardMinWidth),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = sidePad,
                        end = sidePad,
                        top = headerHeightDp + 12.dp,
                        bottom = 108.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(gridGap),
                    verticalArrangement = Arrangement.spacedBy(gridGap)
                ) {
                    // Section 2 & 3: Daily Overview (Liquid Glass) + Primary Action
                    item(
                        key = "__daily_overview__",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "daily_overview"
                    ) {
                        HeroGlassDashboard(
                            reviewDueCount = state.reviewDueCount,
                            newWordsCount = state.newWordsCount,
                            dailyReviewedCount = state.dailyReviewedCount,
                            dailyGoal = state.dailyGoal,
                            isWide = isWide,
                            onSelectDailyGoal = onSelectDailyGoal,
                            onStartDueReview = onStartDueReview,
                            modifier = Modifier.padding(bottom = (sectionGap - gridGap).coerceAtLeast(4.dp))
                        )
                    }

                    // Section 4 & 5: On Tablet Landscape (>=900dp), show all 4 decks in 1 balanced row + direct 3-col topics below
                    if (isTabletLandscape && state.decks.isNotEmpty()) {
                        val fourDecks = VocabularyDecks.orderedCurriculumIds.mapNotNull { deckId ->
                            state.decks.firstOrNull { it.deckId == deckId }
                        }
                        item(
                            key = "__tablet_four_decks_header__",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "section_header"
                        ) {
                            HomeSectionHeader(
                                title = "Bộ từ vựng TOEIC",
                                subtitle = "${fourDecks.size} lộ trình"
                            )
                        }

                        item(
                            key = "__tablet_four_decks_row__",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "tablet_four_decks"
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gridGap)
                            ) {
                                fourDecks.forEach { deck ->
                                    TabletDeckFlatCard(
                                        deck = deck,
                                        isActive = deck.deckId == activeDeck?.deckId,
                                        onClick = { onSelectDeck(deck.deckId) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        if (activeDeck != null) {
                            item(
                                key = "__my_decks_header__",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = "section_header"
                            ) {
                                HomeSectionHeader(
                                    title = "Bộ từ vựng của bạn",
                                    subtitle = "${activeDeck.topicCount} chủ đề",
                                    onSubtitleClick = { onOpenDeckTopics(activeDeck.deckId) }
                                )
                            }

                            item(
                                key = "__my_active_deck__",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = "my_deck"
                            ) {
                                MyDeckFlatCard(
                                    deck = activeDeck,
                                    isExpanded = false,
                                    onContinueClick = { onOpenDeckTopics(activeDeck.deckId) }
                                )
                            }
                        }

                        if (discoverDecks.isNotEmpty()) {
                            item(
                                key = "__discover_decks_header__",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = "section_header"
                            ) {
                                HomeSectionHeader(
                                    title = "Khám phá thêm bộ từ",
                                    subtitle = "${discoverDecks.size} lộ trình",
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }

                            items(
                                items = discoverDecks,
                                key = { "discover_${it.deckId}" },
                                contentType = { "discover_deck" }
                            ) { deck ->
                                DiscoverDeckFlatCard(
                                    deck = deck,
                                    onClick = {
                                        onSelectDeck(deck.deckId)
                                        onOpenDeckTopics(deck.deckId)
                                    }
                                )
                            }
                        }
                    }

                    // Detailed topic list: always visible on Tablet Landscape (>=900dp), or when searching on phone
                    if (isTabletLandscape || topicQuery.isNotBlank()) {
                        item(
                            key = "__active_topics_header__",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "section_header"
                        ) {
                            HomeSectionHeader(
                                title = "Chủ đề trong ${VocabularyDecks.dashboardTitleFor(activeDeck?.deckId ?: state.selectedDeckId)}",
                                subtitle = "${filteredTopics.size} bài",
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        items(
                            items = filteredTopics,
                            key = { "topic_${it.topic}" },
                            contentType = { "topic_card" }
                        ) { topicProgress ->
                            val onTopicClick = remember(topicProgress.topic, onSelectTopic) {
                                { onSelectTopic(topicProgress.topic) }
                            }
                            TopicFlatCard(
                                topicProgress = topicProgress,
                                onClick = onTopicClick,
                                isWide = isWide,
                                modifier = Modifier.animateItem()
                            )
                        }

                        if (filteredTopics.isEmpty()) {
                            item(
                                key = "__empty_topics__",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = "empty_topics"
                            ) {
                                FlatSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (topicQuery.isBlank() && state.totalCount == 0) {
                                            "Bộ từ \"${state.selectedDeckName}\" chưa có từ vựng nào. Hãy sang tab Kho từ bấm nút + để thêm từ mới."
                                        } else {
                                            "Không tìm thấy chủ đề phù hợp với \"$topicQuery\"."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    item(
                        key = "__nav_spacer__",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "nav_spacer"
                    ) {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingGlassHomeHeader(
    topicCount: Int,
    topicQuery: String,
    isCollapsed: Boolean = false,
    onTopicQueryChange: (String) -> Unit,
    onOpenReminderSettings: () -> Unit,
    contentHazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val horizPad = screenHorizontalPadding(LocalConfiguration.current.screenWidthDp.dp)
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var topRowHeightPx by remember { mutableIntStateOf(0) }
    val collapseProgressState = animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = CollapsibleHeaderSpring,
        label = "home_header_collapse"
    )

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

    val searchOpen = isSearchExpanded || topicQuery.isNotEmpty()

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = collapseProgressState.value
                    alpha = (1f - p * 0.15f).coerceIn(0.85f, 1f)
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
                        .padding(horizontal = horizPad, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Lexora",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "TOEIC Vocabulary • SM-2",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassIconButton(
                            onClick = {
                                if (searchOpen) {
                                    isSearchExpanded = false
                                    onTopicQueryChange("")
                                } else {
                                    isSearchExpanded = true
                                }
                            },
                            size = 40.dp
                        ) {
                            Icon(
                                imageVector = if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = if (searchOpen) "Đóng tìm kiếm" else "Tìm kiếm nhanh",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        LiquidGlassIconButton(
                            onClick = onOpenReminderSettings,
                            size = 40.dp
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Cài đặt nhắc nhở",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = searchOpen,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
            ) {
                LiquidGlassSearchCapsule(
                    query = topicQuery,
                    placeholder = "Tìm trong $topicCount chủ đề (Marketing, Thuế...)",
                    onQueryChange = onTopicQueryChange,
                    hazeState = contentHazeState,
                    floatingBlurRadius = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizPad, end = horizPad, bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String,
    onSubtitleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = if (onSubtitleClick != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = if (onSubtitleClick != null) {
                Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSubtitleClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            } else {
                Modifier
            }
        )
    }
}

@Composable
internal fun FlatSurfaceCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val flatFill = if (glassColors.isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        Color.White.copy(alpha = 0.84f)
    }
    val borderColor = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFF0F172A).copy(alpha = 0.08f)
    }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(FlatDeckCardShape)
            .background(flatFill)
            .border(width = 1.dp, color = borderColor, shape = FlatDeckCardShape)
            .then(clickModifier)
    ) {
        content()
    }
}

@Composable
private fun TabletDeckFlatCard(
    deck: DeckProgress,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val viFormatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")) }
    val progress = if (deck.totalCount > 0) {
        (deck.learnedCount.toFloat() / deck.totalCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = (progress * 100f).toInt()
    val flatFill = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (glassColors.isDark) 0.14f else 0.09f)
    } else if (glassColors.isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        Color.White.copy(alpha = 0.84f)
    }
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else if (glassColors.isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFF0F172A).copy(alpha = 0.08f)
    }

    Column(
        modifier = modifier
            .clip(FlatDeckCardShape)
            .background(flatFill)
            .border(width = if (isActive) 1.5.dp else 1.dp, color = borderColor, shape = FlatDeckCardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
                onClick = onClick
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = VocabularyDecks.dashboardTitleFor(deck.deckId),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isActive) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Đang học",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            text = "${viFormatter.format(deck.topicCount)} chủ đề • ${viFormatter.format(deck.totalCount)} từ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        LiquidGlassHairlineProgress(
            progress = progress,
            height = 4.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${viFormatter.format(deck.learnedCount)}/${viFormatter.format(deck.totalCount)} từ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun MyDeckFlatCard(
    deck: DeckProgress,
    isExpanded: Boolean,
    onContinueClick: () -> Unit
) {
    val viFormatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")) }
    val progress = if (deck.totalCount > 0) {
        (deck.learnedCount.toFloat() / deck.totalCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = (progress * 100f).toInt()

    FlatSurfaceCard(
        onClick = onContinueClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = VocabularyDecks.dashboardTitleFor(deck.deckId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = VocabularyDecks.descriptionFor(deck.deckId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LiquidGlassHairlineProgress(
                progress = progress,
                height = 5.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${viFormatter.format(deck.learnedCount)} / ${viFormatter.format(deck.totalCount)} từ đã học",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Thu gọn bài học" else "Tiếp tục học",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverDeckFlatCard(
    deck: DeckProgress,
    onClick: () -> Unit
) {
    val viFormatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")) }
    val progress = if (deck.totalCount > 0) {
        (deck.learnedCount.toFloat() / deck.totalCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = (progress * 100f).toInt()

    FlatSurfaceCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = VocabularyDecks.dashboardTitleFor(deck.deckId),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Chọn bộ ${VocabularyDecks.dashboardTitleFor(deck.deckId)}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = VocabularyDecks.descriptionFor(deck.deckId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            LiquidGlassHairlineProgress(
                progress = progress,
                height = 4.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${viFormatter.format(deck.learnedCount)} / ${viFormatter.format(deck.totalCount)} từ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
internal fun TopicFlatCard(
    topicProgress: TopicProgress,
    onClick: () -> Unit,
    isWide: Boolean = false,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val windowMetrics = LocalAppWindowMetrics.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val isFoldInner = windowMetrics.isFoldInnerExpanded ||
        (screenWidthDp >= AdaptiveWideBreakpoint && screenWidthDp < 900.dp)
    val progress = if (topicProgress.totalCount > 0) {
        topicProgress.learnedCount.toFloat() / topicProgress.totalCount.toFloat()
    } else {
        0f
    }
    val visual = remember(topicProgress.topic) { topicVisualFor(topicProgress.topic) }
    val iconCircleBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    }

    FlatSurfaceCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isWide) 16.dp else 14.dp,
                        end = if (isWide) 16.dp else 14.dp,
                        top = 13.dp,
                        bottom = 15.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isFoldInner) 44.dp else 40.dp)
                        .clip(CircleShape)
                        .background(iconCircleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isFoldInner) 24.dp else 20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = topicProgress.topic,
                        style = if (isFoldInner) {
                            MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        fontWeight = FontWeight.SemiBold,
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
