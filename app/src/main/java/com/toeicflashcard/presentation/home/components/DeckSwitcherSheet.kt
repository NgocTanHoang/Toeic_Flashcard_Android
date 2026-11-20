package com.toeicflashcard.presentation.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassHairlineProgress
import com.toeicflashcard.core.ui.glass.LiquidGlassSheetSurface
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.DeckProgress
import com.toeicflashcard.domain.model.VocabularyDecks
import dev.chrisbanes.haze.HazeState

private val DeckCardShape = RoundedCornerShape(20.dp)

@Composable
fun DeckSwitcherSheet(
    visible: Boolean,
    hazeState: HazeState,
    decks: List<DeckProgress>,
    selectedDeckId: String,
    onSelectDeck: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current

    BackHandler(enabled = visible) {
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(glassColors.sheetScrim)
                    .semantics { contentDescription = "Đóng chọn bộ từ" }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
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
            ) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) { it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DeckSwitcherSheetContent(
                hazeState = hazeState,
                decks = decks,
                selectedDeckId = selectedDeckId,
                onSelectDeck = { deckId ->
                    onSelectDeck(deckId)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun DeckSwitcherSheetContent(
    hazeState: HazeState,
    decks: List<DeckProgress>,
    selectedDeckId: String,
    onSelectDeck: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    LiquidGlassSheetSurface(
        hazeState = hazeState,
        blurRadius = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) {
                            accumulatedDrag += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (accumulatedDrag > 160f) {
                            onDismiss()
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.LibraryBooks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Chọn bộ từ / Giáo trình",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Bảo toàn 100% tiến độ SM-2 khi chuyển đổi giáo trình",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                decks.forEach { deck ->
                    val isSelected = deck.deckId == selectedDeckId
                    DeckOptionGlassCard(
                        deck = deck,
                        selected = isSelected,
                        onClick = { onSelectDeck(deck.deckId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckOptionGlassCard(
    deck: DeckProgress,
    selected: Boolean,
    onClick: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val ratio = if (deck.totalCount > 0) {
        deck.learnedCount.toFloat() / deck.totalCount.toFloat()
    } else {
        0f
    }
    val pct = (ratio * 100f).toInt()

    val targetFill = when {
        selected && glassColors.isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        selected && !glassColors.isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> glassColors.surfaceFill
    }
    val containerColor by animateColorAsState(targetFill, tween(180), label = "deck_card_fill")
    val borderBrush = rememberLiquidGlassBorderBrush(
        glassColors = glassColors,
        accentColor = if (selected) MaterialTheme.colorScheme.primary else null
    )

    LiquidGlassCard(
        onClick = onClick,
        pressScaleEnabled = false,
        shape = DeckCardShape,
        containerColor = containerColor,
        borderBrush = borderBrush,
        modifier = Modifier.fillMaxWidth()
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
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else if (glassColors.isDark) Color.White.copy(alpha = 0.08f)
                            else Color.White.copy(alpha = 0.70f)
                        )
                        .border(
                            width = if (glassColors.isDark) 0.75.dp else 1.dp,
                            brush = borderBrush,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForDeck(deck.deckId),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = deck.deckName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Text(
                        text = subtitleForDeck(deck.deckId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (deck.totalCount > 0) {
                                "Đã thuộc ${deck.learnedCount}/${deck.totalCount} ($pct%)"
                            } else {
                                "Chưa có từ nào"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (deck.dueCount > 0) {
                            Text(
                                text = "• Cần ôn ${deck.dueCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Đang chọn",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LiquidGlassHairlineProgress(
                progress = ratio,
                height = 2.dp,
                trackColor = glassColors.hairlineTrack,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            )
        }
    }
}

private fun iconForDeck(deckId: String): ImageVector = when (deckId) {
    VocabularyDecks.ALL_ID -> Icons.Rounded.AutoStories
    VocabularyDecks.ESSENTIAL_600_ID -> Icons.AutoMirrored.Rounded.MenuBook
    VocabularyDecks.LEVEL_450_800_ID -> Icons.Rounded.Headphones
    VocabularyDecks.CORE_775_ID -> Icons.Rounded.School
    VocabularyDecks.CUSTOM_USER_ID -> Icons.Rounded.BookmarkAdded
    else -> Icons.AutoMirrored.Rounded.LibraryBooks
}

private fun subtitleForDeck(deckId: String): String = when (deckId) {
    VocabularyDecks.ALL_ID -> "Gộp toàn bộ giáo trình & từ tự thêm"
    VocabularyDecks.ESSENTIAL_600_ID -> "50 bài học kinh doanh & giao tiếp TOEIC chuẩn"
    VocabularyDecks.LEVEL_450_800_ID -> "Từ vựng Part 1–3 & trọng tâm Part 5–6"
    VocabularyDecks.CORE_775_ID -> "36 chủ đề TOEIC cốt lõi mặc định"
    VocabularyDecks.CUSTOM_USER_ID -> "Từ vựng tự nhập hoặc nạp qua tệp JSON"
    else -> ""
}
