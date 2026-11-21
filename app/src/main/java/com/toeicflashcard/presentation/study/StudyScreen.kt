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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Celebration
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toeicflashcard.core.ui.glass.AmbientMeshBackground
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassHairlineProgress
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.presentation.study.components.Flashcard3D
import com.toeicflashcard.presentation.study.components.Sm2RatingBar

private val CompleteCardShape = RoundedCornerShape(30.dp)

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
    val glassColors = LocalLiquidGlassColors.current

    LaunchedEffect(viewModel) {
        viewModel.ttsMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
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
                completedCount = state.completedUniqueCount,
                totalCount = state.totalInitialCount,
                remainingCount = state.queue.size,
                isPracticeMode = state.isPracticeMode,
                showCounter = !state.isLoading && !state.isSessionComplete,
                onBack = {
                    viewModel.stopSpeaking()
                    onNavigateBack()
                },
                onToggleSpeechRate = viewModel::toggleSpeechRate
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
                        SessionCompleteContent(
                            totalWords = state.totalInitialCount,
                            requeuedCount = state.requeuedCount,
                            isPracticeMode = state.isPracticeMode,
                            onBackHome = onNavigateBack,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(22.dp)
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

                            Column(modifier = Modifier.fillMaxSize()) {
                                LiquidGlassHairlineProgress(
                                    progress = progress,
                                    height = 3.dp,
                                    animated = true,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )

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
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                ) { slot ->
                                    val flipLatch = remember(slot.key) { FlipStateLatch(false) }
                                    if (slot.key == state.cardInstanceKey) {
                                        flipLatch.flipped = state.isFlipped
                                    }
                                    Flashcard3D(
                                        word = slot.word,
                                        isFlipped = flipLatch.flipped,
                                        onFlip = viewModel::flipCard,
                                        onSpeakWord = viewModel::speakWord,
                                        onSpeakExample = viewModel::speakExample,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp)
                                        .padding(bottom = 14.dp)
                                        .heightIn(min = 76.dp),
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
                                                onSelectQuality = viewModel::submitRating
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
    completedCount: Int,
    totalCount: Int,
    remainingCount: Int,
    isPracticeMode: Boolean,
    showCounter: Boolean,
    onBack: () -> Unit,
    onToggleSpeechRate: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val rateLabel = if (speechRate > 0.85f) "1.0x" else "0.75x"
    val isSlow = speechRate <= 0.85f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SessionCompleteContent(
    totalWords: Int,
    requeuedCount: Int,
    isPracticeMode: Boolean,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = CompleteCardShape,
            containerColor = glassColors.elevatedGlassFill
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
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
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (totalWords == 0) {
                        "Không có từ cần ôn ngay lúc này"
                    } else {
                        "Hoàn thành phiên ôn tập!"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val summaryMessage = when {
                    totalWords == 0 ->
                        "Tất cả từ vựng đều đã được lên lịch ôn lại trong các ngày tới."
                    requeuedCount > 0 ->
                        "Bạn đã hoàn thành $totalWords từ vựng, bao gồm $requeuedCount từ cần ôn lại kỹ hơn."
                    isPracticeMode ->
                        "Bạn đã luyện tập xong $totalWords từ trong chủ đề này và cập nhật lịch SM-2."
                    else ->
                        "Bạn đã ghi nhớ tốt toàn bộ $totalWords từ đến hạn hôm nay."
                }

                Text(
                    text = summaryMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                LiquidGlassButton(
                    onClick = onBackHome,
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
                    Text(
                        text = "Về trang chủ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
