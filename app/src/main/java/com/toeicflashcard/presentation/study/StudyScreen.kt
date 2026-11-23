package com.toeicflashcard.presentation.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toeicflashcard.core.ui.glass.AdaptiveWideBreakpoint
import com.toeicflashcard.core.ui.glass.AmbientMeshBackground
import com.toeicflashcard.core.ui.glass.FoldInnerStudyMaxWidth
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassHairlineProgress
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LocalAppWindowMetrics
import com.toeicflashcard.core.ui.glass.MainContentMaxWidth
import com.toeicflashcard.core.ui.glass.StudyContentMaxWidth
import com.toeicflashcard.core.ui.glass.WideStudyContentMaxWidth
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.usecase.GetStudyQueueUseCase
import com.toeicflashcard.presentation.study.components.Flashcard3D
import com.toeicflashcard.presentation.study.components.Sm2RatingBar

private val CompleteCardShape = RoundedCornerShape(30.dp)
private val StatTileShape = RoundedCornerShape(18.dp)

internal fun triggerStudyHapticIfEnabled(
    enabled: Boolean,
    performHaptic: () -> Unit
) {
    if (enabled) {
        performHaptic()
    }
}

private enum class StudyPhase {
    LOADING,
    COMPLETE,
    ACTIVE
}

@Immutable
private data class StudyCardSlot(
    val key: Int,
    val word: VocabularyWord
)

private class FlipStateLatch(var flipped: Boolean = false)

@Composable
fun StudyScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val glassColors = LocalLiquidGlassColors.current
    val windowMetrics = LocalAppWindowMetrics.current
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp.dp
    val screenHeightDp = config.screenHeightDp.dp
    val isWideScreen = screenWidthDp >= AdaptiveWideBreakpoint || windowMetrics.widthDp >= AdaptiveWideBreakpoint
    val isNarrowCover = windowMetrics.isNarrowCover || screenWidthDp < 385.dp
    val isShortCover = windowMetrics.isShortCover || (screenWidthDp < AdaptiveWideBreakpoint && screenHeightDp < 780.dp)
    val isFoldInner = windowMetrics.isFoldInnerExpanded ||
        (screenWidthDp >= AdaptiveWideBreakpoint && screenWidthDp < 900.dp && screenHeightDp >= 580.dp)
    val isLandscapeShort = windowMetrics.isLandscapeShort
    val isTabletLandscape = (screenWidthDp >= 900.dp || windowMetrics.widthDp >= 900.dp) &&
        (windowMetrics.widthDp > windowMetrics.heightDp)
    val useTwoColumnStudy = isLandscapeShort || isTabletLandscape
    val studyMaxWidth = when {
        isFoldInner -> FoldInnerStudyMaxWidth
        isWideScreen -> WideStudyContentMaxWidth
        else -> StudyContentMaxWidth
    }
    val contentMaxWidth = when {
        isTabletLandscape -> 960.dp
        useTwoColumnStudy -> MainContentMaxWidth
        else -> studyMaxWidth
    }

    val onFlipWithHaptic = remember(viewModel, state.enableHapticFeedback, haptic) {
        {
            triggerStudyHapticIfEnabled(state.enableHapticFeedback) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            viewModel.flipCard()
        }
    }

    val onSubmitRatingWithHaptic: (ReviewQuality) -> Unit = remember(viewModel, state.enableHapticFeedback, haptic) {
        { quality ->
            triggerStudyHapticIfEnabled(state.enableHapticFeedback) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            viewModel.submitRating(quality)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.ttsMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(state.cardInstanceKey, state.isLoading, state.isSessionComplete, state.autoPlayTts) {
        viewModel.maybeAutoPlayCurrentCard()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setScreenActive(true)
                Lifecycle.Event.ON_STOP -> viewModel.setScreenActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setScreenActive(false)
        }
    }

    val phase = when {
        state.isLoading -> StudyPhase.LOADING
        state.isSessionComplete -> StudyPhase.COMPLETE
        else -> StudyPhase.ACTIVE
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            StudyTopBar(
                title = state.title,
                speechRate = state.speechRate,
                autoPlayTts = state.autoPlayTts,
                completedCount = state.completedUniqueCount,
                totalCount = state.totalInitialCount,
                remainingCount = state.queue.size,
                isPracticeMode = state.isPracticeMode,
                showCounter = !state.isLoading && !state.isSessionComplete,
                maxWidth = contentMaxWidth,
                isWideScreen = isWideScreen,
                isNarrowCover = isNarrowCover,
                onBack = {
                    viewModel.stopSpeaking()
                    onNavigateBack()
                },
                onToggleSpeechRate = viewModel::toggleSpeechRate,
                onToggleAutoPlayTts = viewModel::toggleAutoPlayTts
            )

            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (fadeIn(tween(durationMillis = 240)) + scaleIn(
                        initialScale = 0.97f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )) togetherWith fadeOut(tween(durationMillis = 160))
                },
                label = "study_phase_transition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { currentPhase ->
                when (currentPhase) {
                    StudyPhase.LOADING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    StudyPhase.COMPLETE -> {
                        SessionSummaryCard(
                            summary = state.sessionSummary,
                            totalInitialWords = state.totalInitialCount,
                            remainingNewWordsInDeck = state.remainingNewWordsInDeck,
                            maxWidth = studyMaxWidth,
                            onContinueNewWords = viewModel::startNextNewWordsSession,
                            onBackHome = onNavigateBack,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                    }

                    StudyPhase.ACTIVE -> {
                        val currentWord = state.currentWord
                        if (currentWord != null) {
                            val progress = if (state.totalInitialCount > 0) {
                                state.completedUniqueCount.toFloat() / state.totalInitialCount.toFloat()
                            } else {
                                0f
                            }

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LiquidGlassHairlineProgress(
                                        progress = progress,
                                        height = 3.dp,
                                        animated = true,
                                        modifier = Modifier
                                            .widthIn(max = contentMaxWidth)
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                    )
                                }

                                if (useTwoColumnStudy) {
                                    Row(
                                        modifier = Modifier
                                            .widthIn(max = contentMaxWidth)
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AnimatedContent(
                                            targetState = StudyCardSlot(
                                                key = state.cardInstanceKey,
                                                word = currentWord
                                            ),
                                            contentKey = { it.key },
                                            transitionSpec = {
                                                val enter = slideInHorizontally(
                                                    animationSpec = tween(
                                                        durationMillis = 320,
                                                        easing = FastOutSlowInEasing
                                                    )
                                                ) { (it * 0.35f).toInt() } +
                                                    fadeIn(tween(durationMillis = 240)) +
                                                    scaleIn(
                                                        initialScale = 0.94f,
                                                        animationSpec = tween(
                                                            durationMillis = 320,
                                                            easing = FastOutSlowInEasing
                                                        )
                                                    )
                                                val exit = slideOutHorizontally(
                                                    animationSpec = tween(
                                                        durationMillis = 260,
                                                        easing = FastOutSlowInEasing
                                                    )
                                                ) { -(it * 0.45f).toInt() } +
                                                    fadeOut(tween(durationMillis = 200)) +
                                                    scaleOut(
                                                        targetScale = 0.92f,
                                                        animationSpec = tween(
                                                            durationMillis = 260,
                                                            easing = FastOutSlowInEasing
                                                        )
                                                    )
                                                enter togetherWith exit
                                            },
                                            label = "card_transition_landscape",
                                            modifier = Modifier
                                                .weight(0.64f)
                                                .fillMaxHeight()
                                        ) { slot ->
                                            val flipLatch = remember(slot.key) { FlipStateLatch(false) }
                                            if (slot.key == state.cardInstanceKey) {
                                                flipLatch.flipped = state.isFlipped
                                            }
                                            Flashcard3D(
                                                word = slot.word,
                                                isFlipped = flipLatch.flipped,
                                                onFlip = onFlipWithHaptic,
                                                onSpeakWord = viewModel::speakWord,
                                                onSpeakExample = viewModel::speakExample,
                                                isTabletLandscape = isTabletLandscape,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(0.36f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedContent(
                                                targetState = state.hasRevealedBack,
                                                transitionSpec = {
                                                    fadeIn(tween(durationMillis = 220)) togetherWith
                                                        fadeOut(tween(durationMillis = 140))
                                                },
                                                label = "rating_bar_transition_landscape",
                                                modifier = if (isTabletLandscape) Modifier.fillMaxSize() else Modifier
                                            ) { revealed ->
                                                if (revealed) {
                                                    Sm2RatingBar(
                                                        visible = true,
                                                        intervalPreviews = state.intervalPreviews,
                                                        onSelectQuality = onSubmitRatingWithHaptic,
                                                        isVertical = true,
                                                        isLargeVertical = isTabletLandscape,
                                                        modifier = if (isTabletLandscape) Modifier.fillMaxSize() else Modifier
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = if (isTabletLandscape) Modifier.fillMaxSize() else Modifier,
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "Chạm vào thẻ để xem nghĩa và chọn mức độ ghi nhớ",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(horizontal = 12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedContent(
                                            targetState = StudyCardSlot(
                                                key = state.cardInstanceKey,
                                                word = currentWord
                                            ),
                                            contentKey = { it.key },
                                            transitionSpec = {
                                                val enter = slideInHorizontally(
                                                    animationSpec = tween(
                                                        durationMillis = 320,
                                                        easing = FastOutSlowInEasing
                                                    )
                                                ) { (it * 0.35f).toInt() } +
                                                    fadeIn(tween(durationMillis = 240)) +
                                                    scaleIn(
                                                        initialScale = 0.94f,
                                                        animationSpec = tween(
                                                            durationMillis = 320,
                                                            easing = FastOutSlowInEasing
                                                        )
                                                    )
                                                val exit = slideOutHorizontally(
                                                    animationSpec = tween(
                                                        durationMillis = 260,
                                                        easing = FastOutSlowInEasing
                                                    )
                                                ) { -(it * 0.45f).toInt() } +
                                                    fadeOut(tween(durationMillis = 200)) +
                                                    scaleOut(
                                                        targetScale = 0.92f,
                                                        animationSpec = tween(
                                                            durationMillis = 260,
                                                            easing = FastOutSlowInEasing
                                                        )
                                                    )
                                                enter togetherWith exit
                                            },
                                            label = "card_transition",
                                            modifier = Modifier
                                                .widthIn(max = studyMaxWidth)
                                                .fillMaxWidth()
                                                .fillMaxHeight()
                                                .padding(
                                                    horizontal = if (isNarrowCover) 12.dp else 20.dp,
                                                    vertical = if (isShortCover) 10.dp else 16.dp
                                                )
                                        ) { slot ->
                                            val flipLatch = remember(slot.key) { FlipStateLatch(false) }
                                            if (slot.key == state.cardInstanceKey) {
                                                flipLatch.flipped = state.isFlipped
                                            }
                                            Flashcard3D(
                                                word = slot.word,
                                                isFlipped = flipLatch.flipped,
                                                onFlip = onFlipWithHaptic,
                                                onSpeakWord = viewModel::speakWord,
                                                onSpeakExample = viewModel::speakExample,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = when {
                                                    isWideScreen -> 0.dp
                                                    isNarrowCover -> 12.dp
                                                    else -> 18.dp
                                                }
                                            )
                                            .padding(bottom = if (isShortCover) 10.dp else 14.dp)
                                            .heightIn(
                                                min = when {
                                                    isShortCover -> 56.dp
                                                    isFoldInner -> 72.dp
                                                    else -> 76.dp
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = studyMaxWidth)
                                                .fillMaxWidth()
                                                .padding(horizontal = if (isWideScreen) 20.dp else 0.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedContent(
                                                targetState = state.hasRevealedBack,
                                                transitionSpec = {
                                                    (fadeIn(tween(durationMillis = 220)) +
                                                        slideInVertically(
                                                            animationSpec = tween(
                                                                durationMillis = 260,
                                                                easing = FastOutSlowInEasing
                                                            )
                                                        ) { it / 3 }) togetherWith
                                                        fadeOut(tween(durationMillis = 140))
                                                },
                                                label = "rating_bar_transition"
                                            ) { revealed ->
                                                if (revealed) {
                                                    Sm2RatingBar(
                                                        visible = true,
                                                        intervalPreviews = state.intervalPreviews,
                                                        onSelectQuality = onSubmitRatingWithHaptic
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Chạm vào thẻ để xem nghĩa và chọn mức độ ghi nhớ",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) { data ->
            LiquidGlassCard(
                shape = CircleShape,
                containerColor = glassColors.dialogSheetFill
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun StudyTopBar(
    title: String,
    speechRate: Float,
    autoPlayTts: Boolean,
    completedCount: Int,
    totalCount: Int,
    remainingCount: Int,
    isPracticeMode: Boolean,
    showCounter: Boolean,
    maxWidth: Dp = StudyContentMaxWidth,
    isWideScreen: Boolean = false,
    isNarrowCover: Boolean = false,
    onBack: () -> Unit,
    onToggleSpeechRate: () -> Unit,
    onToggleAutoPlayTts: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val rateLabel = if (speechRate > 0.85f) "1.0x" else "0.75x"
    val isSlow = speechRate <= 0.85f

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(
                    horizontal = when {
                        isWideScreen -> 20.dp
                        isNarrowCover -> 12.dp
                        else -> 18.dp
                    },
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isNarrowCover) 6.dp else 12.dp)
        ) {
        LiquidGlassIconButton(
            onClick = onBack,
            size = 42.dp
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Quay lại",
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (showCounter) {
                val sub = buildString {
                    append("Đã xong $completedCount/$totalCount • còn $remainingCount")
                    if (isPracticeMode) append(" • Tự do")
                }
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(if (isNarrowCover) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val autoPlayTint = when {
                glassColors.isDark && autoPlayTts -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                glassColors.isDark -> Color.White.copy(alpha = 0.08f)
                autoPlayTts -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
            }
            val autoPlayContent = when {
                autoPlayTts -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            LiquidGlassIconButton(
                onClick = onToggleAutoPlayTts,
                size = 38.dp,
                containerColor = autoPlayTint,
                contentColor = autoPlayContent
            ) {
                Icon(
                    imageVector = if (autoPlayTts) {
                        Icons.AutoMirrored.Rounded.VolumeUp
                    } else {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    },
                    contentDescription = if (autoPlayTts) {
                        "Tắt tự động phát âm"
                    } else {
                        "Bật tự động phát âm"
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            val speedTint = when {
                glassColors.isDark -> Color.White.copy(alpha = if (isSlow) 0.16f else 0.08f)
                isSlow -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
            }
            val speedContent = when {
                glassColors.isDark && isSlow -> MaterialTheme.colorScheme.primary
                glassColors.isDark -> MaterialTheme.colorScheme.onSurface
                isSlow -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
            LiquidGlassButton(
                onClick = onToggleSpeechRate,
                tintColor = speedTint,
                contentColor = speedContent,
                borderColor = if (!glassColors.isDark && isSlow) MaterialTheme.colorScheme.primary else null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.heightIn(min = 38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = "Tốc độ đọc",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = rateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    }
}

@Composable
private fun SessionSummaryCard(
    summary: SessionSummaryStats,
    totalInitialWords: Int,
    remainingNewWordsInDeck: Int,
    maxWidth: Dp = StudyContentMaxWidth,
    onContinueNewWords: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val nextBatchCount = minOf(remainingNewWordsInDeck, GetStudyQueueUseCase.DEFAULT_DAILY_SESSION_LIMIT)
    val streakBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }
    val statTileBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.055f)
    } else {
        Color.White.copy(alpha = 0.56f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            shape = CompleteCardShape,
            containerColor = glassColors.elevatedGlassFill
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (glassColors.isDark) {
                                    Color.White.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                                }
                            )
                            .border(
                                width = if (glassColors.isDark) 0.75.dp else 1.dp,
                                brush = rememberLiquidGlassBorderBrush(
                                    glassColors = glassColors,
                                    accentColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.secondary
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Celebration,
                            contentDescription = null,
                            tint = if (glassColors.isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(streakBg)
                            .border(
                                width = if (glassColors.isDark) 0.75.dp else 1.dp,
                                brush = glassColors.defaultBorderBrush,
                                shape = CircleShape
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (summary.currentStreak > 0) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Streak ${summary.currentStreak} ngày",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (totalInitialWords == 0 && summary.totalEvaluatedCount == 0) {
                            "Không có từ cần ôn ngay lúc này"
                        } else {
                            "Tổng kết phiên học"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tỷ lệ nhớ: ${summary.recallRatePercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatTile(
                            label = "Từ mới đã học",
                            value = "${summary.newWordsStudied}",
                            containerColor = statTileBg,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            label = "Từ ôn tập",
                            value = "${summary.reviewedWordsCount}",
                            containerColor = statTileBg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatTile(
                            label = "Tổng số từ đã ôn",
                            value = "${summary.totalEvaluatedCount}",
                            containerColor = statTileBg,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            label = "Số từ nhớ ngay",
                            value = "${summary.rememberedImmediatelyCount}",
                            containerColor = statTileBg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (nextBatchCount > 0) {
                    LiquidGlassButton(
                        onClick = onContinueNewWords,
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
                        borderColor = if (glassColors.isDark) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Học tiếp $nextBatchCount từ mới",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(StatTileShape)
                            .background(statTileBg)
                            .border(
                                width = if (glassColors.isDark) 0.75.dp else 1.dp,
                                brush = glassColors.defaultBorderBrush,
                                shape = StatTileShape
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bạn đã hoàn thành tất cả từ mới trong bộ này",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                LiquidGlassButton(
                    onClick = onBackHome,
                    tintColor = if (glassColors.isDark) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.White.copy(alpha = 0.65f)
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    borderColor = null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Về Trang chủ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    Column(
        modifier = modifier
            .clip(StatTileShape)
            .background(containerColor)
            .border(
                width = if (glassColors.isDark) 0.75.dp else 1.dp,
                brush = glassColors.defaultBorderBrush,
                shape = StatTileShape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
