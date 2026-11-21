package com.toeicflashcard.presentation.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.toeicflashcard.core.ui.glass.FocusedContentMaxWidth
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassHairlineProgress
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LocalAppWindowMetrics
import com.toeicflashcard.core.ui.glass.MainContentMaxWidth
import com.toeicflashcard.core.ui.glass.WideQuizContentMaxWidth
import com.toeicflashcard.core.ui.glass.adaptiveHorizontalPadding
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.core.ui.theme.LocalSm2RatingColors
import com.toeicflashcard.domain.model.QuizOption
import com.toeicflashcard.domain.model.QuizQuestion
import com.toeicflashcard.domain.model.VocabularyWord

private val QuizQuestionCardShape = RoundedCornerShape(28.dp)
private val QuizOptionCardShape = RoundedCornerShape(18.dp)
private val WrongWordCardShape = RoundedCornerShape(20.dp)

private enum class QuizPhase {
    LOADING,
    FINISHED,
    ACTIVE
}

@Immutable
private data class QuizQuestionSlot(
    val index: Int,
    val question: QuizQuestion
)

private class QuizAnswerLatch(
    var selectedIndex: Int? = null,
    var hasAnswered: Boolean = false
)

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val glassColors = LocalLiquidGlassColors.current
    val windowMetrics = LocalAppWindowMetrics.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val isWideScreen = screenWidthDp >= AdaptiveWideBreakpoint || windowMetrics.widthDp >= AdaptiveWideBreakpoint
    val isLandscapeShort = windowMetrics.isLandscapeShort
    val quizMaxWidth = if (isWideScreen) WideQuizContentMaxWidth else FocusedContentMaxWidth
    val contentMaxWidth = if (isLandscapeShort) MainContentMaxWidth else quizMaxWidth

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
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
        state.isLoading -> QuizPhase.LOADING
        state.isFinished -> QuizPhase.FINISHED
        else -> QuizPhase.ACTIVE
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            QuizTopBar(
                title = state.title,
                speechRate = state.speechRate,
                isListeningMode = state.isListeningMode,
                maxWidth = contentMaxWidth,
                onToggleSpeechRate = viewModel::toggleSpeechRate,
                onBack = {
                    viewModel.stopSpeaking()
                    onNavigateBack()
                }
            )

            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (fadeIn(tween(durationMillis = 260)) + scaleIn(
                        initialScale = 0.97f,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    )) togetherWith fadeOut(tween(durationMillis = 160))
                },
                label = "quiz_phase_transition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { currentPhase ->
                when (currentPhase) {
                    QuizPhase.LOADING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    QuizPhase.FINISHED -> {
                        QuizSummaryContent(
                            state = state,
                            maxContentWidth = quizMaxWidth,
                            onSpeakWord = viewModel::speakWord,
                            onMarkUrgentReview = viewModel::markWrongWordsForUrgentReview,
                            onRestartQuiz = viewModel::restartQuiz,
                            onBackHome = onNavigateBack,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    QuizPhase.ACTIVE -> {
                        val question = state.currentQuestion
                        if (question != null) {
                            QuizQuestionContent(
                                question = question,
                                currentIndex = state.currentIndex,
                                totalCount = state.questions.size,
                                selectedOptionIndex = state.selectedOptionIndex,
                                hasAnsweredCurrent = state.hasAnsweredCurrent,
                                isListeningMode = state.isListeningMode,
                                isLandscapeShort = isLandscapeShort,
                                containerMaxWidth = contentMaxWidth,
                                onSpeakWord = viewModel::speakCurrentWord,
                                onSelectOption = viewModel::selectOption,
                                onNextQuestion = viewModel::nextQuestion,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            )
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
private fun QuizTopBar(
    title: String,
    speechRate: Float,
    isListeningMode: Boolean,
    maxWidth: Dp = FocusedContentMaxWidth,
    onToggleSpeechRate: () -> Unit,
    onBack: () -> Unit
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

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isListeningMode) {
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
    }
}

@Composable
private fun QuizQuestionContent(
    question: QuizQuestion,
    currentIndex: Int,
    totalCount: Int,
    selectedOptionIndex: Int?,
    hasAnsweredCurrent: Boolean,
    isListeningMode: Boolean,
    isLandscapeShort: Boolean,
    containerMaxWidth: Dp,
    onSpeakWord: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val progress = if (totalCount > 0) (currentIndex + 1).toFloat() / totalCount.toFloat() else 0f
    val topicPillBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.56f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = containerMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Câu ${currentIndex + 1}/$totalCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(topicPillBg)
                        .border(0.8.dp, glassColors.defaultBorderBrush, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = question.word.topic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            LiquidGlassHairlineProgress(
                progress = progress,
                height = 3.dp,
                animated = true
            )
        }

        AnimatedContent(
            targetState = QuizQuestionSlot(index = currentIndex, question = question),
            contentKey = { it.index },
            transitionSpec = {
                val enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) { (it * 0.35f).toInt() } +
                    fadeIn(tween(durationMillis = 220)) +
                    scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                val exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) { -(it * 0.40f).toInt() } +
                    fadeOut(tween(durationMillis = 180)) +
                    scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )
                enter togetherWith exit
            },
            label = "quiz_question_transition",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { slot ->
            val answerLatch = remember(slot.index) { QuizAnswerLatch() }
            if (slot.index == currentIndex) {
                answerLatch.selectedIndex = selectedOptionIndex
                answerLatch.hasAnswered = hasAnsweredCurrent
            }
            val slotQuestion = slot.question
            val slotSelectedIndex = answerLatch.selectedIndex
            val slotHasAnswered = answerLatch.hasAnswered

            if (isLandscapeShort) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        QuizQuestionPromptCard(
                            question = slotQuestion,
                            revealWord = !isListeningMode || slotHasAnswered,
                            topicPillBg = topicPillBg,
                            onSpeakWord = onSpeakWord
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slotQuestion.options.forEachIndexed { index, option ->
                            QuizOptionGlassCard(
                                option = option,
                                isSelected = slotSelectedIndex == index,
                                hasAnswered = slotHasAnswered,
                                onClick = { onSelectOption(index) }
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = hasAnsweredCurrent,
                            enter = fadeIn(tween(durationMillis = 200)) + slideInVertically(
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            ) { it / 2 },
                            exit = fadeOut(tween(durationMillis = 140))
                        ) {
                            LiquidGlassButton(
                                onClick = onNextQuestion,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    text = if (currentIndex + 1 >= totalCount) "Xem kết quả" else "Tiếp tục",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    QuizQuestionPromptCard(
                        question = slotQuestion,
                        revealWord = !isListeningMode || slotHasAnswered,
                        topicPillBg = topicPillBg,
                        onSpeakWord = onSpeakWord
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        slotQuestion.options.forEachIndexed { index, option ->
                            QuizOptionGlassCard(
                                option = option,
                                isSelected = slotSelectedIndex == index,
                                hasAnswered = slotHasAnswered,
                                onClick = { onSelectOption(index) }
                            )
                        }
                    }
                }
            }
        }

        if (!isLandscapeShort) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = hasAnsweredCurrent,
                    enter = fadeIn(tween(durationMillis = 200)) + slideInVertically(
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    ) { it / 2 },
                    exit = fadeOut(tween(durationMillis = 140))
                ) {
                    LiquidGlassButton(
                        onClick = onNextQuestion,
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (currentIndex + 1 >= totalCount) "Xem kết quả" else "Tiếp tục",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun QuizQuestionPromptCard(
    question: QuizQuestion,
    revealWord: Boolean,
    topicPillBg: Color,
    onSpeakWord: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = QuizQuestionCardShape,
        containerColor = glassColors.elevatedGlassFill
    ) {
        AnimatedContent(
            targetState = revealWord,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 220)) + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(durationMillis = 220)
                )) togetherWith fadeOut(tween(durationMillis = 140))
            },
            label = "listening_word_reveal"
        ) { isWordVisible ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(topicPillBg)
                        .border(0.8.dp, glassColors.defaultBorderBrush, CircleShape)
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = question.word.partOfSpeech,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isWordVisible) {
                    Text(
                        text = question.word.word,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = question.word.phonetic,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    LiquidGlassIconButton(
                        onClick = onSpeakWord,
                        size = 50.dp,
                        containerColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (glassColors.isDark) 0.14f else 0.16f
                        ),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Phát âm ${question.word.word}",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))

                    LiquidGlassIconButton(
                        onClick = onSpeakWord,
                        size = 72.dp,
                        containerColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (glassColors.isDark) 0.16f else 0.20f
                        ),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Nghe lại phát âm",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Nghe phát âm và chọn nghĩa đúng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Chạm biểu tượng loa để nghe lại",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizOptionGlassCard(
    option: QuizOption,
    isSelected: Boolean,
    hasAnswered: Boolean,
    onClick: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val sm2Colors = LocalSm2RatingColors.current
    val (targetContainerColor, targetContentColor, accentBorder) = when {
        !hasAnswered -> Triple(
            glassColors.surfaceFill,
            MaterialTheme.colorScheme.onSurface,
            null
        )
        option.isCorrect -> Triple(
            sm2Colors.easy.container,
            sm2Colors.easy.content,
            sm2Colors.easy.border
        )
        isSelected && !option.isCorrect -> Triple(
            sm2Colors.again.container,
            sm2Colors.again.content,
            sm2Colors.again.border
        )
        else -> Triple(
            glassColors.surfaceFill.copy(alpha = glassColors.cardAlpha * 0.6f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            null
        )
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 220),
        label = "quiz_opt_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 220),
        label = "quiz_opt_fg"
    )

    LiquidGlassCard(
        onClick = onClick,
        enabled = !hasAnswered,
        shape = QuizOptionCardShape,
        containerColor = containerColor,
        borderWidth = if (hasAnswered && (option.isCorrect || isSelected)) 1.4.dp else 1.dp,
        borderBrush = rememberLiquidGlassBorderBrush(glassColors, accentColor = accentBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = option.textVi,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (hasAnswered && option.isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            AnimatedVisibility(
                visible = hasAnswered && (option.isCorrect || isSelected),
                enter = fadeIn(tween(durationMillis = 180)) + scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = fadeOut(tween(durationMillis = 120))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (option.isCorrect) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Đáp án đúng",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = "Đáp án sai",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizSummaryContent(
    state: QuizUiState,
    maxContentWidth: Dp = FocusedContentMaxWidth,
    onSpeakWord: (String) -> Unit,
    onMarkUrgentReview: () -> Unit,
    onRestartQuiz: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    val totalCount = state.questions.size
    val wrongWords = remember(state.wrongWords) { state.wrongWords.distinctBy { it.id } }

    BoxWithConstraints(modifier = modifier) {
        val sidePad = adaptiveHorizontalPadding(
            availableWidth = maxWidth,
            maxContentWidth = maxContentWidth,
            basePadding = 20.dp
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = sidePad,
                end = sidePad,
                top = 14.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        item(contentType = "quiz_summary_header") {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = QuizQuestionCardShape,
                containerColor = glassColors.elevatedGlassFill
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(
                                    alpha = if (glassColors.isDark) 0.28f else 0.16f
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = rememberLiquidGlassBorderBrush(
                                    glassColors = glassColors,
                                    accentColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = if (totalCount == 0) {
                            "Chưa đủ từ vựng để tạo câu hỏi"
                        } else {
                            "Kết quả kiểm tra"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (totalCount > 0) {
                        Text(
                            text = "${state.correctCount}/$totalCount câu đúng",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = when {
                            totalCount == 0 ->
                                "Cần tối thiểu 4 từ vựng trong phạm vi đã chọn để tạo bài trắc nghiệm."
                            wrongWords.isEmpty() ->
                                "Xuất sắc! Bạn đã trả lời đúng toàn bộ câu hỏi."
                            else ->
                                "Bạn có ${wrongWords.size} từ cần xem lại bên dưới."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (wrongWords.isNotEmpty()) {
            item(contentType = "urgent_review_btn") {
                LiquidGlassButton(
                    onClick = onMarkUrgentReview,
                    enabled = !state.isUrgentReviewSaving && !state.isUrgentReviewSaved,
                    tintColor = MaterialTheme.colorScheme.tertiary.copy(
                        alpha = if (glassColors.isDark) 0.34f else 0.22f
                    ),
                    contentColor = if (glassColors.isDark) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    borderColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (state.isUrgentReviewSaved) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.Bolt
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isUrgentReviewSaved) {
                            "Đã đưa vào diện ôn tập gấp"
                        } else {
                            "Đưa các từ sai vào diện ôn tập gấp"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            item(contentType = "wrong_words_header") {
                Text(
                    text = "Các từ làm sai (${wrongWords.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(
                items = wrongWords,
                key = { it.id },
                contentType = { "wrong_word" }
            ) { word ->
                val onSpeak = remember(word.word, onSpeakWord) { { onSpeakWord(word.word) } }
                WrongWordReviewGlassCard(
                    word = word,
                    onSpeak = onSpeak
                )
            }
        }

        item(contentType = "quiz_summary_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LiquidGlassButton(
                    onClick = onRestartQuiz,
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Làm lại bài kiểm tra",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                LiquidGlassButton(
                    onClick = onBackHome,
                    tintColor = glassColors.surfaceFill,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    borderColor = null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Về trang chủ",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun WrongWordReviewGlassCard(
    word: VocabularyWord,
    onSpeak: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        pressScaleEnabled = false,
        shape = WrongWordCardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = word.meaningVi,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            LiquidGlassIconButton(
                onClick = onSpeak,
                size = 42.dp,
                containerColor = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (glassColors.isDark) 0.14f else 0.16f
                ),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Phát âm ${word.word}",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
