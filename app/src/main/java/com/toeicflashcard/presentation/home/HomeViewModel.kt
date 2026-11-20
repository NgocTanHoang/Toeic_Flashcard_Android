package com.toeicflashcard.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toeicflashcard.ToeicApplication
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.core.notification.ReminderScheduler
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.streak.StreakCalculator
import com.toeicflashcard.domain.usecase.AddCustomVocabularyUseCase
import com.toeicflashcard.domain.usecase.AddWordResult
import com.toeicflashcard.domain.usecase.CustomWordInput
import com.toeicflashcard.domain.usecase.ImportJsonResult
import com.toeicflashcard.domain.usecase.ObserveHomeSummaryUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val vocabularyRepository: VocabularyRepository,
    observeHomeSummaryUseCase: ObserveHomeSummaryUseCase,
    private val addCustomVocabularyUseCase: AddCustomVocabularyUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider,
    private val streakCalculator: StreakCalculator,
    private val reminderScheduler: ReminderScheduler,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val selectedDeckIdFlow = userPreferencesRepository.userPreferences
        .map { it.selectedDeckId }
        .distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> = combine(
        observeHomeSummaryUseCase(selectedDeckIdFlow),
        userPreferencesRepository.userPreferences,
        searchQuery
    ) { summary, prefs, query ->
        val todayEpochDay = timeProvider.todayEpochDay()
        val effectiveStreak = streakCalculator.effectiveStateForDisplay(
            stored = prefs.toStreakState(),
            todayEpochDay = todayEpochDay
        )
        val trimmedQuery = query.trim()
        val filteredTopics = if (trimmedQuery.isEmpty()) {
            summary.topics
        } else {
            summary.topics.filter { progress ->
                progress.topic.contains(trimmedQuery, ignoreCase = true) ||
                    topicSubtitleViFor(progress.topic).contains(trimmedQuery, ignoreCase = true)
            }
        }
        HomeUiState(
            isLoading = !summary.hasAnyDatabaseWords,
            hasDatabaseWords = summary.hasAnyDatabaseWords,
            selectedDeckId = summary.selectedDeckId,
            selectedDeckName = VocabularyDecks.displayNameFor(summary.selectedDeckId),
            decks = summary.decks,
            dueTodayCount = summary.dueTodayCount,
            learnedCount = summary.learnedCount,
            totalCount = summary.totalCount,
            topics = summary.topics,
            filteredTopics = filteredTopics,
            themeMode = prefs.themeMode,
            darkTone = prefs.darkTone,
            speechRate = prefs.speechRate,
            currentStreak = effectiveStreak.currentStreak,
            hasStudiedToday = prefs.lastStudyDateEpochDay == todayEpochDay,
            dailyReviewedCount = effectiveStreak.dailyReviewedCount,
            dailyGoal = prefs.dailyGoal,
            reminderEnabled = prefs.reminderEnabled,
            reminderHour = prefs.reminderHour,
            reminderMinute = prefs.reminderMinute,
            glassOpacity = prefs.glassOpacity
        )
    }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState()
        )

    init {
        viewModelScope.launch {
            vocabularyRepository.ensureSeeded()
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectDeck(deckId: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSelectedDeckId(deckId)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun toggleLightDark(currentlyDark: Boolean) {
        val nextMode = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(nextMode)
        }
    }

    fun selectDarkTone(tone: DarkTone, currentlyDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkTone(
                tone = tone,
                forceDarkMode = !currentlyDark
            )
        }
    }

    suspend fun addCustomWord(input: CustomWordInput): AddWordResult {
        return addCustomVocabularyUseCase.addSingleWord(input)
    }

    suspend fun importCustomWordsJson(rawJson: String): ImportJsonResult {
        return addCustomVocabularyUseCase.importFromJson(rawJson)
    }

    fun updateDailyGoal(goal: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setDailyGoal(goal)
        }
    }

    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setReminderSettings(enabled, hour, minute)
            if (enabled) {
                reminderScheduler.scheduleDailyReminder(hour, minute)
            } else {
                reminderScheduler.cancelDailyReminder()
            }
        }
    }

    fun previewGlassOpacity(opacity: Float) {
        userPreferencesRepository.previewGlassOpacity(opacity)
    }

    fun updateGlassOpacity(opacity: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setGlassOpacity(opacity)
        }
    }

    fun resetGlassOpacity() {
        viewModelScope.launch {
            userPreferencesRepository.resetGlassOpacity()
        }
    }

    fun toggleSpeechRate() {
        val nextRate = if (uiState.value.speechRate > 0.85f) {
            TtsManager.SLOW_SPEECH_RATE
        } else {
            TtsManager.DEFAULT_SPEECH_RATE
        }
        viewModelScope.launch {
            userPreferencesRepository.setSpeechRate(nextRate)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ToeicApplication
                val container = app.container
                HomeViewModel(
                    vocabularyRepository = container.vocabularyRepository,
                    observeHomeSummaryUseCase = container.observeHomeSummaryUseCase,
                    addCustomVocabularyUseCase = container.addCustomVocabularyUseCase,
                    userPreferencesRepository = container.userPreferencesRepository,
                    timeProvider = container.timeProvider,
                    streakCalculator = container.streakCalculator,
                    reminderScheduler = container.reminderScheduler
                )
            }
        }
    }
}
