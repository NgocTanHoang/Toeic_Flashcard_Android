package com.toeicflashcard.presentation.vault

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.toeicflashcard.core.ui.glass.CollapsibleHeaderSpring
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LiquidGlassSearchCapsule
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.core.ui.theme.LocalSm2RatingColors
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val VaultWordCardShape = RoundedCornerShape(20.dp)
private val ExamplePanelShape = RoundedCornerShape(14.dp)

@Composable
fun VocabularyVaultScreen(
    state: VocabularyVaultUiState,
    viewModel: VocabularyVaultViewModel,
    query: String,
    headerHeightDp: Dp,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
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
        label = "vault_loading",
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.words.isEmpty()) {
                    item(
                        key = "__vault_empty__",
                        contentType = "vault_empty"
                    ) {
                        LiquidGlassCard(
                            shape = VaultWordCardShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (query.isBlank()) {
                                    "Chưa có từ vựng nào trong nhóm \"${state.selectedFilter.label}\"."
                                } else {
                                    "Không tìm thấy từ nào khớp với \"$query\"."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = state.words,
                        key = { it.id },
                        contentType = { "vocab_card" }
                    ) { word ->
                        val onSpeak = remember(word.word, viewModel) {
                            { viewModel.speakWord(word.word) }
                        }
                        VaultWordCard(
                            word = word,
                            onSpeak = onSpeak
                        )
                    }
                }

                item(
                    key = "__vault_nav_spacer__",
                    contentType = "vault_nav_spacer"
                ) {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
fun FloatingVaultHeader(
    selectedDeckName: String,
    totalCount: Int,
    query: String,
    selectedFilter: VaultFilter,
    isCollapsed: Boolean = false,
    contentHazeState: HazeState,
    onQueryChange: (String) -> Unit,
    onSelectFilter: (VaultFilter) -> Unit,
    onOpenDeckSwitcher: () -> Unit,
    onOpenAddVocabulary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    var topRowHeightPx by remember { mutableIntStateOf(0) }
    var filterRowHeightPx by remember { mutableIntStateOf(0) }
    val collapseProgressState = animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = CollapsibleHeaderSpring,
        label = "vault_header_collapse"
    )
    val extraTopGapPx = remember(density) { with(density) { 8.dp.toPx() } }
    val extraSideMarginPx = remember(density) { with(density) { 16.dp.toPx() } }
    val floatingShadowPx = remember(density, glassColors.isDark) {
        with(density) { (if (glassColors.isDark) 8.dp else 10.dp).toPx() }
    }
    val ambientShadowTint = Color(0xFF0F172A).copy(alpha = if (glassColors.isDark) 0.45f else 0.16f)
    val spotShadowTint = Color(0xFF0F172A).copy(alpha = if (glassColors.isDark) 0.55f else 0.24f)

    val formattedTotalCount = remember(totalCount) {
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(totalCount)
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

        // Layer 2: Collapsing top bar + Sticky floating frosted search pill + collapsing filter row
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Kho từ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        VaultDeckDropdownPill(
                            selectedDeckName = selectedDeckName,
                            enabled = !isCollapsed,
                            onClick = onOpenDeckSwitcher,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    LiquidGlassIconButton(
                        onClick = onOpenAddVocabulary,
                        size = 40.dp,
                        enabled = !isCollapsed
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Thêm từ vựng mới",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            LiquidGlassSearchCapsule(
                query = query,
                placeholder = "Tìm trong $formattedTotalCount từ...",
                onQueryChange = onQueryChange,
                hazeState = contentHazeState,
                floatingBlurRadius = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged {
                        if (it.height > 0) filterRowHeightPx = it.height
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = collapseProgressState.value
                            translationY = -filterRowHeightPx * p
                            alpha = (1f - p * 1.6f).coerceIn(0f, 1f)
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VaultFilter.entries.forEach { filter ->
                        VaultFilterChip(
                            label = filter.label,
                            selected = selectedFilter == filter,
                            interactive = !isCollapsed,
                            onClick = { onSelectFilter(filter) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultDeckDropdownPill(
    selectedDeckName: String,
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

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(pillFill)
            .border(
                width = if (glassColors.isDark) 0.75.dp else 1.dp,
                brush = glassColors.defaultBorderBrush,
                shape = CircleShape
            )
            .then(clickModifier)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = selectedDeckName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Chuyển bộ từ trong Kho từ",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun VaultFilterChip(
    label: String,
    selected: Boolean,
    interactive: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val targetBg = when {
        selected && glassColors.isDark -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        glassColors.isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.56f)
    }
    val targetFg = when {
        selected && glassColors.isDark -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bg by animateColorAsState(targetBg, tween(180), label = "vault_chip_bg")
    val fg by animateColorAsState(targetFg, tween(180), label = "vault_chip_fg")
    val borderBrush = rememberLiquidGlassBorderBrush(
        glassColors = glassColors,
        accentColor = if (selected && !glassColors.isDark) MaterialTheme.colorScheme.primary else null
    )
    val clickModifier = if (interactive) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = fg),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .border(
                width = if (glassColors.isDark) 0.75.dp else 1.dp,
                brush = borderBrush,
                shape = CircleShape
            )
            .then(clickModifier)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VaultWordCard(
    word: VocabularyWord,
    onSpeak: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val sm2Colors = LocalSm2RatingColors.current
    var expanded by remember(word.id) { mutableStateOf(false) }

    val isForgotten = remember(word.lastReviewedTimestamp, word.repetitions, word.easeFactor) {
        VocabularyVaultViewModel.isForgotten(word)
    }

    val (badgeLabel, badgeBg, badgeFg) = when {
        isForgotten -> Triple("Hay quên", sm2Colors.again.container, sm2Colors.again.content)
        word.isLearned -> Triple("Đã thuộc", sm2Colors.easy.container, sm2Colors.easy.content)
        else -> Triple(
            "Đang học",
            if (glassColors.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.60f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    LiquidGlassCard(
        onClick = { expanded = !expanded },
        pressScaleEnabled = false,
        shape = VaultWordCardShape,
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = word.word,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "(${word.partOfSpeech})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (glassColors.isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }

                    if (word.phonetic.isNotBlank()) {
                        Text(
                            text = word.phonetic,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = word.meaningVi,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                LiquidGlassIconButton(
                    onClick = onSpeak,
                    size = 40.dp,
                    containerColor = if (glassColors.isDark) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    },
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Phát âm ${word.word}",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${VocabularyDecks.shortLabelFor(word.deckId)} • ${word.topic}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeBg)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeFg
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ExamplePanelShape)
                        .background(
                            if (glassColors.isDark) Color.White.copy(alpha = 0.05f)
                            else Color.White.copy(alpha = 0.50f)
                        )
                        .border(
                            width = 0.6.dp,
                            brush = glassColors.defaultBorderBrush,
                            shape = ExamplePanelShape
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (word.exampleEn.isNotBlank()) {
                        Text(
                            text = word.exampleEn,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (word.exampleVi.isNotBlank()) {
                            Text(
                                text = word.exampleVi,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Chưa có câu ví dụ cho từ này.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
