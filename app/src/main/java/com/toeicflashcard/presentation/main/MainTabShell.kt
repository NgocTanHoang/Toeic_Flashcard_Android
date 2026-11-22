package com.toeicflashcard.presentation.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toeicflashcard.core.ui.glass.AmbientMeshBackground
import com.toeicflashcard.core.ui.glass.DockScrollController
import com.toeicflashcard.core.ui.glass.LiquidGlassBottomDock
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.MainDockTab
import com.toeicflashcard.core.ui.glass.rememberCollapsibleHeaderState
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.presentation.home.FloatingGlassHomeHeader
import com.toeicflashcard.presentation.home.HomeScreen
import com.toeicflashcard.presentation.home.HomeViewModel
import com.toeicflashcard.presentation.home.components.AddVocabularySheet
import com.toeicflashcard.presentation.home.components.DeckSwitcherSheet
import com.toeicflashcard.presentation.home.components.OnboardingSheet
import com.toeicflashcard.presentation.home.components.QuizScopeDialog
import com.toeicflashcard.presentation.home.components.ReminderSettingsSheet
import com.toeicflashcard.presentation.practice.FloatingPracticeHeader
import com.toeicflashcard.presentation.practice.PracticeScreen
import com.toeicflashcard.presentation.stats.FloatingStatsHeader
import com.toeicflashcard.presentation.stats.StatsScreen
import com.toeicflashcard.presentation.stats.StatsViewModel
import com.toeicflashcard.presentation.vault.FloatingVaultHeader
import com.toeicflashcard.presentation.vault.VocabularyVaultScreen
import com.toeicflashcard.presentation.vault.VocabularyVaultViewModel
import com.toeicflashcard.presentation.vault.WordDetailSheet
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

private enum class QuizScopeLaunchMode {
    STANDARD,
    LISTENING
}

@Composable
fun MainTabShell(
    homeViewModel: HomeViewModel,
    onStartDueReview: () -> Unit,
    onOpenDeckTopics: (String) -> Unit,
    onSelectTopic: (String) -> Unit,
    onReviewHardWords: () -> Unit,
    onStartQuiz: (topic: String?, listeningMode: Boolean) -> Unit
) {
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val vaultViewModel: VocabularyVaultViewModel = viewModel(factory = VocabularyVaultViewModel.Factory)
    val vaultState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val statsViewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(MainDockTab.HOME) }
    var homeQuery by rememberSaveable { mutableStateOf("") }
    var vaultQuery by rememberSaveable { mutableStateOf("") }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showAddVocabSheet by remember { mutableStateOf(false) }
    var showDeckSwitcher by remember { mutableStateOf(false) }
    var quizScopeMode by remember { mutableStateOf<QuizScopeLaunchMode?>(null) }
    var selectedDetailWordId by rememberSaveable { mutableStateOf<Int?>(null) }

    val selectedDetailWord = remember(selectedDetailWordId, vaultState.allScopedWords) {
        selectedDetailWordId?.let { id ->
            vaultState.allScopedWords.firstOrNull { it.id == id }
        }
    }

    var homeHeaderHeightPx by rememberSaveable { mutableIntStateOf(0) }
    var vaultHeaderHeightPx by rememberSaveable { mutableIntStateOf(0) }
    var practiceHeaderHeightPx by rememberSaveable { mutableIntStateOf(0) }
    var statsHeaderHeightPx by rememberSaveable { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val homeHeaderHeightDp = with(density) { if (homeHeaderHeightPx > 0) homeHeaderHeightPx.toDp() else 92.dp }
    val vaultHeaderHeightDp = with(density) { if (vaultHeaderHeightPx > 0) vaultHeaderHeightPx.toDp() else 196.dp }
    val practiceHeaderHeightDp = with(density) { if (practiceHeaderHeightPx > 0) practiceHeaderHeightPx.toDp() else 92.dp }
    val statsHeaderHeightDp = with(density) { if (statsHeaderHeightPx > 0) statsHeaderHeightPx.toDp() else 92.dp }

    val glassColors = LocalLiquidGlassColors.current
    val contentHazeState = remember { HazeState() }
    val dockScrollController = remember { DockScrollController() }
    val collapsibleHeaderState = rememberCollapsibleHeaderState(dockScrollController)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val homeGridState = rememberLazyGridState()
    val vaultGridState = rememberLazyGridState()
    val practiceListState = rememberLazyListState()
    val statsGridState = rememberLazyGridState()

    LaunchedEffect(selectedTab) {
        collapsibleHeaderState.expand()
    }

    LaunchedEffect(selectedTab, homeGridState, vaultGridState, practiceListState, statsGridState) {
        snapshotFlow {
            when (selectedTab) {
                MainDockTab.HOME -> !homeGridState.canScrollForward || !homeGridState.canScrollBackward
                MainDockTab.VAULT -> !vaultGridState.canScrollForward || !vaultGridState.canScrollBackward
                MainDockTab.PRACTICE -> !practiceListState.canScrollForward || !practiceListState.canScrollBackward
                MainDockTab.STATS -> !statsGridState.canScrollForward || !statsGridState.canScrollBackward
            }
        }
            .distinctUntilChanged()
            .collect { atBoundary ->
                if (atBoundary) {
                    collapsibleHeaderState.expand()
                }
            }
    }

    val showOnboarding = homeState.preferencesLoaded && !homeState.hasCompletedOnboarding
    val isOverlayOpen = showReminderSheet || showAddVocabSheet || showDeckSwitcher ||
        quizScopeMode != null || selectedDetailWord != null || showOnboarding

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Single Haze source containing AmbientMeshBackground + scrollable tab lists
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = contentHazeState)
        ) {
            AmbientMeshBackground(modifier = Modifier.fillMaxSize())

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 180)) togetherWith fadeOut(tween(durationMillis = 140))
                },
                label = "main_dock_tab_content",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    MainDockTab.HOME -> {
                        HomeScreen(
                            state = homeState,
                            topicQuery = homeQuery,
                            headerHeightDp = homeHeaderHeightDp,
                            gridState = homeGridState,
                            nestedScrollConnection = collapsibleHeaderState.nestedScrollConnection,
                            onSelectDeck = homeViewModel::selectDeck,
                            onOpenDeckTopics = { deckId ->
                                homeViewModel.selectDeck(deckId)
                                onOpenDeckTopics(deckId)
                            },
                            onSelectDailyGoal = homeViewModel::updateDailyGoal,
                            onStartDueReview = onStartDueReview,
                            onSelectTopic = onSelectTopic
                        )
                    }

                    MainDockTab.VAULT -> {
                        VocabularyVaultScreen(
                            state = vaultState,
                            viewModel = vaultViewModel,
                            query = vaultQuery,
                            headerHeightDp = vaultHeaderHeightDp,
                            gridState = vaultGridState,
                            nestedScrollConnection = collapsibleHeaderState.nestedScrollConnection,
                            onSelectWord = { word -> selectedDetailWordId = word.id },
                            onShowMessage = { msg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                    }

                    MainDockTab.PRACTICE -> {
                        PracticeScreen(
                            headerHeightDp = practiceHeaderHeightDp,
                            listState = practiceListState,
                            nestedScrollConnection = dockScrollController.nestedScrollConnection,
                            selectedDeckName = homeState.selectedDeckName,
                            topics = homeState.topics,
                            onOpenStandardQuiz = { quizScopeMode = QuizScopeLaunchMode.STANDARD },
                            onOpenListeningQuiz = { quizScopeMode = QuizScopeLaunchMode.LISTENING },
                            onQuickStartStandardQuiz = { onStartQuiz(null, false) },
                            onQuickStartListeningQuiz = { onStartQuiz(null, true) },
                            onStartTopicQuiz = { topic -> onStartQuiz(topic, false) },
                            onReviewHardWords = onReviewHardWords
                        )
                    }

                    MainDockTab.STATS -> {
                        StatsScreen(
                            state = statsState,
                            viewModel = statsViewModel,
                            headerHeightDp = statsHeaderHeightDp,
                            gridState = statsGridState,
                            nestedScrollConnection = dockScrollController.nestedScrollConnection,
                            onReviewHardWords = onReviewHardWords,
                            onShowMessage = { msg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Layer 2a: Top Floating Glass Header rendered as a sibling OUTSIDE Modifier.haze(contentHazeState)
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(durationMillis = 160)) togetherWith fadeOut(tween(durationMillis = 120))
            },
            label = "main_floating_header",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) { tab ->
            when (tab) {
                MainDockTab.HOME -> {
                    FloatingGlassHomeHeader(
                        topicCount = homeState.topics.size,
                        topicQuery = homeQuery,
                        isCollapsed = collapsibleHeaderState.isCollapsed,
                        onTopicQueryChange = { next ->
                            homeQuery = next
                            homeViewModel.updateSearchQuery(next)
                        },
                        onOpenReminderSettings = { showReminderSheet = true },
                        contentHazeState = contentHazeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { homeHeaderHeightPx = it.height }
                    )
                }

                MainDockTab.VAULT -> {
                    FloatingVaultHeader(
                        selectedDeckName = vaultState.selectedDeckName,
                        totalCount = vaultState.totalCount,
                        query = vaultQuery,
                        selectedFilter = vaultState.selectedFilter,
                        counts = vaultState.counts,
                        isCollapsed = collapsibleHeaderState.isCollapsed,
                        contentHazeState = contentHazeState,
                        onQueryChange = { next ->
                            vaultQuery = next
                            vaultViewModel.updateSearchQuery(next)
                        },
                        onSelectFilter = vaultViewModel::selectFilter,
                        onOpenDeckSwitcher = { showDeckSwitcher = true },
                        onOpenAddVocabulary = { showAddVocabSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { vaultHeaderHeightPx = it.height }
                    )
                }

                MainDockTab.PRACTICE -> {
                    FloatingPracticeHeader(
                        selectedDeckName = homeState.selectedDeckName,
                        totalCount = homeState.totalCount,
                        speechRate = homeState.speechRate,
                        onToggleSpeechRate = homeViewModel::toggleSpeechRate,
                        contentHazeState = contentHazeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { practiceHeaderHeightPx = it.height }
                    )
                }

                MainDockTab.STATS -> {
                    FloatingStatsHeader(
                        learnedWords = statsState.learnedWords,
                        totalWords = statsState.totalWords,
                        currentStreak = statsState.currentStreak,
                        contentHazeState = contentHazeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { statsHeaderHeightPx = it.height }
                    )
                }
            }
        }

        // Layer 2b: Floating Liquid Glass Bottom Dock centered at bottom across all viewports
        val isDockVisible = dockScrollController.isVisible && !isOverlayOpen
        LiquidGlassBottomDock(
            selectedTab = selectedTab,
            onSelectTab = { nextTab ->
                selectedTab = nextTab
                collapsibleHeaderState.expand()
            },
            visible = isDockVisible,
            hazeState = contentHazeState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Layer 3: Frosted Glass Snackbar elevated cleanly above Bottom Dock
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = if (isDockVisible) 112.dp else 20.dp)
        ) { data ->
            LiquidGlassCard(
                hazeState = contentHazeState,
                blurRadius = 18.dp,
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

        // Layer 4: In-window Settings Sheet
        ReminderSettingsSheet(
            visible = showReminderSheet,
            hazeState = contentHazeState,
            themeMode = homeState.themeMode,
            darkTone = homeState.darkTone,
            reminderEnabled = homeState.reminderEnabled,
            reminderHour = homeState.reminderHour,
            reminderMinute = homeState.reminderMinute,
            enableHapticFeedback = homeState.enableHapticFeedback,
            onSelectThemeMode = homeViewModel::setThemeMode,
            onSelectDarkTone = { tone ->
                homeViewModel.selectDarkTone(tone = tone, currentlyDark = glassColors.isDark)
            },
            onPreviewGlassOpacity = homeViewModel::previewGlassOpacity,
            onCommitGlassOpacity = homeViewModel::updateGlassOpacity,
            onResetGlassOpacity = homeViewModel::resetGlassOpacity,
            onSetHapticFeedback = homeViewModel::setEnableHapticFeedback,
            onDismiss = { showReminderSheet = false },
            onSaveReminderSettings = { enabled, hour, minute ->
                val wasEnabled = homeState.reminderEnabled
                val timeChanged = hour != homeState.reminderHour || minute != homeState.reminderMinute
                homeViewModel.updateReminderSettings(enabled, hour, minute)
                if (enabled && (!wasEnabled || timeChanged)) {
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Đã đặt nhắc nhở hàng ngày lúc $timeStr")
                    }
                } else if (!enabled && wasEnabled) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Đã tắt nhắc nhở hàng ngày")
                    }
                }
            }
        )

        // Layer 5: In-window Add Custom Vocabulary Sheet
        AddVocabularySheet(
            visible = showAddVocabSheet,
            hazeState = contentHazeState,
            existingTopics = remember(homeState.topics) { homeState.topics.map { it.topic } },
            onAddWord = homeViewModel::addCustomWord,
            onImportJson = homeViewModel::importCustomWordsJson,
            onShowMessage = { msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            },
            onDismiss = { showAddVocabSheet = false }
        )

        // Layer 6: In-window Quiz / Listening Scope Dialog
        QuizScopeDialog(
            visible = quizScopeMode != null,
            hazeState = contentHazeState,
            topics = homeState.topics,
            selectedDeckName = homeState.selectedDeckName,
            title = if (quizScopeMode == QuizScopeLaunchMode.LISTENING) {
                "Chọn phạm vi luyện nghe"
            } else {
                "Chọn phạm vi kiểm tra"
            },
            subtitle = if (quizScopeMode == QuizScopeLaunchMode.LISTENING) {
                "Nghe phát âm tiếng Anh và chọn nghĩa tiếng Việt trong tối đa 10 câu:"
            } else {
                "Kiểm tra nhanh tối đa 10 câu trắc nghiệm nghĩa tiếng Việt:"
            },
            onDismiss = { quizScopeMode = null },
            onSelectScope = { selectedTopic ->
                val isListening = quizScopeMode == QuizScopeLaunchMode.LISTENING
                quizScopeMode = null
                onStartQuiz(selectedTopic, isListening)
            }
        )

        // Layer 7: In-window Deck Switcher BottomSheet
        DeckSwitcherSheet(
            visible = showDeckSwitcher,
            hazeState = contentHazeState,
            decks = homeState.decks,
            selectedDeckId = homeState.selectedDeckId,
            onSelectDeck = { deckId ->
                homeViewModel.selectDeck(deckId)
            },
            onDismiss = { showDeckSwitcher = false }
        )

        // Layer 8: In-window Word Detail, Edit & Delete Sheet
        WordDetailSheet(
            word = selectedDetailWord,
            hazeState = contentHazeState,
            onSpeakAtRate = vaultViewModel::speakWordAtRate,
            onToggleStarred = vaultViewModel::toggleStarred,
            onResetProgress = vaultViewModel::resetWordProgress,
            onUpdateCustomWord = vaultViewModel::updateCustomWord,
            onDeleteCustomWord = vaultViewModel::deleteCustomWord,
            onShowMessage = { msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            },
            onDismiss = { selectedDetailWordId = null }
        )

        // Layer 9: Optional 1-Step Onboarding Sheet
        OnboardingSheet(
            visible = showOnboarding,
            hazeState = contentHazeState,
            initialDailyGoal = homeState.dailyGoal,
            initialTargetLevel = homeState.targetToeicLevel,
            onStartLearning = { goal, target ->
                homeViewModel.completeOnboarding(goal, target)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Đã lưu mục tiêu $target • $goal từ/ngày")
                }
            },
            onSkip = homeViewModel::skipOnboarding
        )

    }
}
