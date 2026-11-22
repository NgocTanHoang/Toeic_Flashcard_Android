package com.toeicflashcard.presentation.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.AdaptiveWideBreakpoint
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.MainContentMaxWidth
import com.toeicflashcard.core.ui.glass.adaptiveHorizontalPadding
import com.toeicflashcard.core.ui.glass.cardInnerPadding
import com.toeicflashcard.core.ui.glass.gridItemSpacing
import com.toeicflashcard.core.ui.glass.screenHorizontalPadding
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.TopicProgress
import com.toeicflashcard.presentation.home.topicVisualFor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import java.text.NumberFormat
import java.util.Locale

private val PracticeActionCardShape = RoundedCornerShape(20.dp)

@Composable
fun PracticeScreen(
    headerHeightDp: Dp,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    selectedDeckName: String = "",
    topics: List<TopicProgress> = emptyList(),
    onOpenStandardQuiz: () -> Unit,
    onOpenListeningQuiz: () -> Unit,
    onQuickStartStandardQuiz: () -> Unit = onOpenStandardQuiz,
    onQuickStartListeningQuiz: () -> Unit = onOpenListeningQuiz,
    onStartTopicQuiz: (String) -> Unit = {},
    onReviewHardWords: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= AdaptiveWideBreakpoint
        val isTabletLandscape = maxWidth >= 900.dp
        val basePad = if (isWide) screenHorizontalPadding(maxWidth) else 18.dp
        val sidePad = adaptiveHorizontalPadding(
            availableWidth = maxWidth,
            maxContentWidth = MainContentMaxWidth,
            basePadding = basePad
        )
        val rowGap = if (isWide) gridItemSpacing(maxWidth) else 12.dp
        val cardPad = if (isWide) 22.dp else 16.dp
        val minModeHeight = if (isWide) 108.dp else 84.dp

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                start = sidePad,
                end = sidePad,
                top = headerHeightDp + 12.dp,
                bottom = 108.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isWide) {
                item(key = "__practice_row__", contentType = "practice_row_2col") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(rowGap)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PracticeModeGlassCard(
                                icon = Icons.Rounded.Quiz,
                                title = "Trắc nghiệm nhanh",
                                subtitle = "10 câu trắc nghiệm nghĩa ngẫu nhiên",
                                accentColor = MaterialTheme.colorScheme.primary,
                                innerPadding = cardPad,
                                minHeight = minModeHeight,
                                onClick = onOpenStandardQuiz
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PracticeModeGlassCard(
                                icon = Icons.Rounded.Headphones,
                                title = "Luyện nghe từ vựng",
                                subtitle = "Nghe phát âm TTS và chọn nghĩa đúng",
                                accentColor = MaterialTheme.colorScheme.secondary,
                                innerPadding = cardPad,
                                minHeight = minModeHeight,
                                onClick = onOpenListeningQuiz
                            )
                        }
                    }
                }
            } else {
                item(key = "__practice_quiz__", contentType = "practice_mode") {
                    PracticeModeGlassCard(
                        icon = Icons.Rounded.Quiz,
                        title = "Trắc nghiệm nhanh",
                        subtitle = "10 câu trắc nghiệm nghĩa ngẫu nhiên",
                        accentColor = MaterialTheme.colorScheme.primary,
                        minHeight = minModeHeight,
                        onClick = onOpenStandardQuiz
                    )
                }

                item(key = "__practice_listening__", contentType = "practice_mode") {
                    PracticeModeGlassCard(
                        icon = Icons.Rounded.Headphones,
                        title = "Luyện nghe từ vựng",
                        subtitle = "Nghe phát âm TTS và chọn nghĩa đúng",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        minHeight = minModeHeight,
                        onClick = onOpenListeningQuiz
                    )
                }
            }

            if (selectedDeckName.isNotBlank()) {
                item(key = "__quick_scope_section__", contentType = "quick_scope") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Chọn nhanh phạm vi luyện tập",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(rowGap)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PracticeModeGlassCard(
                                        icon = Icons.Rounded.Bolt,
                                        title = "Trắc nghiệm bộ hiện tại",
                                        subtitle = "10 câu từ $selectedDeckName",
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        innerPadding = 16.dp,
                                        minHeight = 84.dp,
                                        onClick = onQuickStartStandardQuiz
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    PracticeModeGlassCard(
                                        icon = Icons.Rounded.Headphones,
                                        title = "Luyện nghe bộ hiện tại",
                                        subtitle = "Nghe TTS từ $selectedDeckName",
                                        accentColor = MaterialTheme.colorScheme.secondary,
                                        innerPadding = 16.dp,
                                        minHeight = 84.dp,
                                        onClick = onQuickStartListeningQuiz
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    PracticeModeGlassCard(
                                        icon = Icons.Rounded.Refresh,
                                        title = "Ôn tập từ hay quên",
                                        subtitle = "Luyện tập trung các từ dễ nhầm",
                                        accentColor = MaterialTheme.colorScheme.tertiary,
                                        innerPadding = 16.dp,
                                        minHeight = 84.dp,
                                        onClick = onReviewHardWords
                                    )
                                }
                            }
                        } else {
                            PracticeModeGlassCard(
                                icon = Icons.Rounded.Bolt,
                                title = "Trắc nghiệm bộ hiện tại",
                                subtitle = "10 câu từ $selectedDeckName",
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = onQuickStartStandardQuiz
                            )
                            PracticeModeGlassCard(
                                icon = Icons.Rounded.Refresh,
                                title = "Ôn tập từ hay quên",
                                subtitle = "Luyện tập trung các từ dễ nhầm",
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                onClick = onReviewHardWords
                            )
                        }
                    }
                }
            }

            if (isTabletLandscape && topics.isNotEmpty()) {
                item(key = "__practice_topics_header__", contentType = "section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Luyện trắc nghiệm theo chủ đề",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${topics.size} chủ đề",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val topicRows = topics.chunked(3)
                lazyItems(
                    items = topicRows,
                    key = { row -> row.joinToString("_") { it.topic } }
                ) { rowTopics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(rowGap)
                    ) {
                        rowTopics.forEach { tp ->
                            val visual = remember(tp.topic) { topicVisualFor(tp.topic) }
                            Box(modifier = Modifier.weight(1f)) {
                                PracticeModeGlassCard(
                                    icon = visual.icon,
                                    title = tp.topic,
                                    subtitle = "${tp.totalCount} từ • ${visual.subtitleVi}",
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    innerPadding = 14.dp,
                                    minHeight = 74.dp,
                                    onClick = { onStartTopicQuiz(tp.topic) }
                                )
                            }
                        }
                        repeat(3 - rowTopics.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "__practice_nav_spacer__", contentType = "nav_spacer") {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
fun FloatingPracticeHeader(
    selectedDeckName: String,
    totalCount: Int,
    speechRate: Float,
    onToggleSpeechRate: () -> Unit,
    contentHazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val headerHorizPad = if (screenWidthDp >= AdaptiveWideBreakpoint) {
        screenHorizontalPadding(screenWidthDp)
    } else {
        18.dp
    }
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
                .align(Alignment.CenterHorizontally)
                .widthIn(max = MainContentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = headerHorizPad, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Luyện tập",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val viFormatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")) }
                Text(
                    text = "$selectedDeckName • ${viFormatter.format(totalCount)} từ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LiquidGlassButton(
                onClick = onToggleSpeechRate,
                tintColor = if (glassColors.isDark) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                },
                contentColor = if (glassColors.isDark) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                borderColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.primary,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                modifier = Modifier.heightIn(min = 38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = "Đổi tốc độ phát âm",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (speechRate > 0.85f) "1.0x" else "0.75x",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
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
private fun PracticeModeGlassCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    innerPadding: Dp = 16.dp,
    minHeight: Dp = 80.dp,
    onClick: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val iconCircleBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.09f)
    } else {
        accentColor.copy(alpha = 0.14f)
    }
    val iconTint = if (glassColors.isDark) MaterialTheme.colorScheme.primary else accentColor

    LiquidGlassCard(
        onClick = onClick,
        pressScaleEnabled = false,
        shape = PracticeActionCardShape,
        containerColor = glassColors.elevatedGlassFill,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = innerPadding, vertical = innerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
