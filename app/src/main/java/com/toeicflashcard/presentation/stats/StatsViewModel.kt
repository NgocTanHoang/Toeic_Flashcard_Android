package com.toeicflashcard.presentation.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toeicflashcard.ToeicApplication
import com.toeicflashcard.core.audio.TtsFeedback
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.streak.StreakCalculator
import com.toeicflashcard.presentation.home.CUSTOM_TOPIC_CATEGORY
import com.toeicflashcard.presentation.home.TOPIC_CATEGORIES
import com.toeicflashcard.presentation.home.topicCategoryFor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@Immutable
data class DailyBarStat(
    val epochDay: Long,
    val dayLabel: String,
    val count: Int,
    val isToday: Boolean
)

@Immutable
data class CategoryMasteryStat(
    val category: String,
    val learnedCount: Int,
    val totalCount: Int
)

@Immutable
data class DeckMasteryStat(
    val deckId: String,
    val deckName: String,
    val learnedCount: Int,
    val totalCount: Int,
    val dueCount: Int
)

@Immutable
data class StatsUiState(
    val isLoading: Boolean = true,
    val totalWords: Int = 0,
    val learnedWords: Int = 0,
    val currentStreak: Int = 0,
    val dailyGoal: Int = 10,
    val last7Days: List<DailyBarStat> = emptyList(),
    val totalReviewsLast7Days: Int = 0,
    val deckStats: List<DeckMasteryStat> = emptyList(),
    val categoryStats: List<CategoryMasteryStat> = emptyList(),
    val hardestWords: List<VocabularyWord> = emptyList(),
    val hasHardWords: Boolean = false,
    val speechRate: Float = TtsManager.DEFAULT_SPEECH_RATE
)

class StatsViewModel(
    vocabularyRepository: VocabularyRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider,
    private val streakCalculator: StreakCalculator,
    createTtsManager: ((TtsFeedback) -> Unit) -> TtsManager,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _ttsMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val ttsMessages: SharedFlow<String> = _ttsMessages.asSharedFlow()

    private val ttsManager: TtsManager = createTtsManager { feedback ->
        emitTtsFeedbackIfError(feedback)
    }

    val uiState: StateFlow<StatsUiState> = combine(
        vocabularyRepository.observeAllWords(),
        userPreferencesRepository.userPreferences
    ) { words, prefs ->
        val todayEpochDay = timeProvider.todayEpochDay()
        val effectiveStreak = streakCalculator.effectiveStateForDisplay(
            stored = prefs.toStreakState(),
            todayEpochDay = todayEpochDay
        )

        val minDay = todayEpochDay - 6L
        val roomReviewedByDay = HashMap<Long, Int>()
        words.forEach { word ->
            if (word.lastReviewedTimestamp > 0L) {
                val day = timeProvider.epochDayForMillis(word.lastReviewedTimestamp)
                if (day in minDay..todayEpochDay) {
                    roomReviewedByDay[day] = (roomReviewedByDay[day] ?: 0) + 1
                }
            }
        }

        val bars = (-6L..0L).map { offset ->
            val day = todayEpochDay + offset
            val historyCount = prefs.dailyReviewHistory[day] ?: 0
            val todayPrefCount = if (day == prefs.lastStudyDateEpochDay) prefs.dailyReviewedCount else 0
            val roomCount = roomReviewedByDay[day] ?: 0
            val count = maxOf(historyCount, todayPrefCount, roomCount)
            DailyBarStat(
                epochDay = day,
                dayLabel = if (day == todayEpochDay) "Nay" else weekdayShortLabel(day),
                count = count,
                isToday = day == todayEpochDay
            )
        }

        val cutoffMillis = timeProvider.endOfTodayMillis()
        val wordsByDeck = words.groupBy { it.deckId }
        val deckStats = VocabularyDecks.orderedCurriculumIds
            .mapNotNull { deckId ->
                val deckWords = wordsByDeck[deckId].orEmpty()
                if (deckId == VocabularyDecks.CUSTOM_USER_ID && deckWords.isEmpty()) {
                    null
                } else {
                    DeckMasteryStat(
                        deckId = deckId,
                        deckName = VocabularyDecks.displayNameFor(deckId),
                        learnedCount = deckWords.count { it.isLearned },
                        totalCount = deckWords.size,
                        dueCount = deckWords.count { it.isDue(cutoffMillis) }
                    )
                }
            }

        val groupedCategories = words.groupBy { topicCategoryFor(it.topic) }
        val categoriesToDisplay = buildList {
            addAll(TOPIC_CATEGORIES)
            if (!groupedCategories[CUSTOM_TOPIC_CATEGORY].isNullOrEmpty()) {
                add(CUSTOM_TOPIC_CATEGORY)
            }
        }
        val categoryStats = categoriesToDisplay.map { category ->
            val catWords = groupedCategories[category].orEmpty()
            CategoryMasteryStat(
                category = category,
                learnedCount = catWords.count { it.isLearned },
                totalCount = catWords.size
            )
        }

        val hardPool = words
            .filter { it.lastReviewedTimestamp > 0L || it.easeFactor < 2.5f }
            .sortedWith(
                compareBy<VocabularyWord> { it.easeFactor }
                    .thenBy { it.repetitions }
                    .thenBy { it.id }
            )

        StatsUiState(
            isLoading = words.isEmpty(),
            totalWords = words.size,
            learnedWords = words.count { it.isLearned },
            currentStreak = effectiveStreak.currentStreak,
            dailyGoal = prefs.dailyGoal.coerceAtLeast(1),
            last7Days = bars,
            totalReviewsLast7Days = bars.sumOf { it.count },
            deckStats = deckStats,
            categoryStats = categoryStats,
            hardestWords = hardPool.take(MAX_HARD_WORDS_PREVIEW),
            hasHardWords = hardPool.isNotEmpty(),
            speechRate = prefs.speechRate
        )
    }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = StatsUiState()
        )

    fun speakWord(word: String) {
        val feedback = ttsManager.speak(word, uiState.value.speechRate)
        emitTtsFeedbackIfError(feedback)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    private fun emitTtsFeedbackIfError(feedback: TtsFeedback) {
        when (feedback) {
            is TtsFeedback.MissingVoiceData -> _ttsMessages.tryEmit(feedback.messageVi)
            is TtsFeedback.Unavailable -> _ttsMessages.tryEmit(feedback.messageVi)
            TtsFeedback.Spoken, TtsFeedback.Initializing -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    companion object {
        private const val MAX_HARD_WORDS_PREVIEW = 8

        // 1970-01-01 (epochDay = 0) was Thursday -> T5.
        private fun weekdayShortLabel(epochDay: Long): String {
            return when (((epochDay % 7L) + 7L) % 7L) {
                0L -> "T5"
                1L -> "T6"
                2L -> "T7"
                3L -> "CN"
                4L -> "T2"
                5L -> "T3"
                else -> "T4"
            }
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ToeicApplication
                val container = app.container
                StatsViewModel(
                    vocabularyRepository = container.vocabularyRepository,
                    userPreferencesRepository = container.userPreferencesRepository,
                    timeProvider = container.timeProvider,
                    streakCalculator = container.streakCalculator,
                    createTtsManager = { onDeferred -> container.createTtsManager(onDeferred) }
                )
            }
        }
    }
}
