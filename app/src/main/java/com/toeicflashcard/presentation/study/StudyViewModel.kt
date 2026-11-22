package com.toeicflashcard.presentation.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toeicflashcard.ToeicApplication
import com.toeicflashcard.core.audio.TtsFeedback
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.core.audio.TtsPlayer
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.scheduler.Sm2Scheduler
import com.toeicflashcard.domain.streak.StreakCalculator
import com.toeicflashcard.domain.usecase.GetStudyQueueUseCase
import com.toeicflashcard.domain.usecase.StudyQueueResult
import com.toeicflashcard.domain.usecase.SubmitReviewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyViewModel(
    private val topic: String?,
    private val hardWordsOnly: Boolean = false,
    private val deckId: String? = null,
    private val getStudyQueueUseCase: GetStudyQueueUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
    private val sm2Scheduler: Sm2Scheduler,
    private val streakCalculator: StreakCalculator,
    private val timeProvider: TimeProvider,
    private val userPreferencesRepository: UserPreferencesRepository,
    createTtsPlayer: ((TtsFeedback) -> Unit) -> TtsPlayer
) : ViewModel() {

    private fun resolvedSessionTitle(): String {
        val activeDeckLabel = deckId
            ?.let { VocabularyDecks.normalizeSelectedDeckId(it) }
            ?.takeIf { it != VocabularyDecks.ALL_ID }
            ?.let { VocabularyDecks.shortLabelFor(it) }
        return when {
            hardWordsOnly -> "Ôn nhóm từ khó"
            topic != null -> topic
            activeDeckLabel != null -> "Ôn tập • $activeDeckLabel"
            else -> "Ôn tập hôm nay"
        }
    }

    private val initialCachedQueue = getStudyQueueUseCase.getCachedIfAvailable(
        topic = topic,
        hardWordsOnly = hardWordsOnly,
        deckId = deckId
    )
    private val cachedSpeechRate = userPreferencesRepository.getCachedSpeechRate()
    private val cachedAutoPlayTts = userPreferencesRepository.getCachedAutoPlayTts()
    private val cachedHapticEnabled = userPreferencesRepository.getCachedHapticEnabled()

    private val _uiState = MutableStateFlow(
        buildInitialUiState(
            initialQueue = initialCachedQueue,
            cachedRate = cachedSpeechRate,
            cachedAutoPlay = cachedAutoPlayTts,
            cachedHaptic = cachedHapticEnabled
        )
    )
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val _ttsMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val ttsMessages: SharedFlow<String> = _ttsMessages.asSharedFlow()

    private val ttsPlayer: TtsPlayer = createTtsPlayer { feedback ->
        emitTtsFeedbackIfError(feedback)
    }

    private val requeuedIds = mutableSetOf<Int>()
    private var initialNewWordIds = initialCachedQueue?.words
        ?.filter { it.repetitions == 0 }
        ?.map { it.id }
        ?.toSet()
        .orEmpty()
    private val evaluatedWordIds = mutableSetOf<Int>()
    private var isSubmitting = false
    private var lastFlipElapsedMs = 0L
    private var isScreenActive = true
    private var lastAutoPlayedCardInstanceKey: Int? = null

    init {
        observePreferences()
        if (initialCachedQueue == null) {
            loadSession()
        } else {
            if (initialCachedQueue.words.isEmpty()) {
                viewModelScope.launch {
                    val remaining = getStudyQueueUseCase.countRemainingNewWords(deckId)
                    _uiState.update { it.copy(remainingNewWordsInDeck = remaining) }
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                delay(TTS_WARMUP_DELAY_MS)
                ttsPlayer.warmUp()
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                val effectiveStreak = streakCalculator.effectiveStateForDisplay(
                    stored = prefs.toStreakState(),
                    todayEpochDay = timeProvider.todayEpochDay()
                )
                _uiState.update { state ->
                    state.copy(
                        speechRate = prefs.speechRate,
                        autoPlayTts = prefs.autoPlayTts,
                        enableHapticFeedback = prefs.enableHapticFeedback,
                        sessionSummary = state.sessionSummary.copy(
                            currentStreak = effectiveStreak.currentStreak
                        )
                    )
                }
            }
        }
    }

    private fun buildInitialUiState(
        initialQueue: StudyQueueResult?,
        cachedRate: Float?,
        cachedAutoPlay: Boolean?,
        cachedHaptic: Boolean?
    ): StudyUiState {
        val rate = cachedRate ?: TtsManager.DEFAULT_SPEECH_RATE
        val autoPlay = cachedAutoPlay ?: true
        val haptic = cachedHaptic ?: false
        if (initialQueue == null) {
            return StudyUiState(
                title = resolvedSessionTitle(),
                speechRate = rate,
                autoPlayTts = autoPlay,
                enableHapticFeedback = haptic
            )
        }
        val firstWord = initialQueue.words.firstOrNull()
        val previews = firstWord?.let {
            sm2Scheduler.previewAll(it, timeProvider.nowMillis())
        }.orEmpty()
        return StudyUiState(
            title = resolvedSessionTitle(),
            isLoading = false,
            isPracticeMode = initialQueue.isPracticeMode,
            queue = initialQueue.words,
            totalInitialCount = initialQueue.words.size,
            completedUniqueCount = 0,
            requeuedCount = 0,
            isFlipped = false,
            hasRevealedBack = false,
            cardInstanceKey = 0,
            intervalPreviews = previews,
            speechRate = rate,
            autoPlayTts = autoPlay,
            enableHapticFeedback = haptic,
            isSessionComplete = initialQueue.words.isEmpty()
        )
    }

    private fun loadSession() {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferences.first()
            val result = getStudyQueueUseCase(
                topic = topic,
                hardWordsOnly = hardWordsOnly,
                deckId = deckId
            )
            applyNewQueue(
                result = result,
                speechRate = prefs.speechRate,
                autoPlayTts = prefs.autoPlayTts,
                enableHapticFeedback = prefs.enableHapticFeedback
            )

            viewModelScope.launch(Dispatchers.IO) {
                delay(TTS_WARMUP_DELAY_MS)
                ttsPlayer.warmUp()
            }
        }
    }

    private suspend fun applyNewQueue(
        result: StudyQueueResult,
        speechRate: Float,
        autoPlayTts: Boolean,
        enableHapticFeedback: Boolean
    ) {
        requeuedIds.clear()
        evaluatedWordIds.clear()
        initialNewWordIds = result.words
            .asSequence()
            .filter { it.repetitions == 0 }
            .map { it.id }
            .toSet()
        lastAutoPlayedCardInstanceKey = null

        val firstWord = result.words.firstOrNull()
        val previews = firstWord?.let {
            sm2Scheduler.previewAll(it, timeProvider.nowMillis())
        }.orEmpty()
        val remainingNew = if (result.words.isEmpty()) {
            getStudyQueueUseCase.countRemainingNewWords(deckId)
        } else {
            _uiState.value.remainingNewWordsInDeck
        }
        val prefs = userPreferencesRepository.userPreferences.first()
        val effectiveStreak = streakCalculator.effectiveStateForDisplay(
            stored = prefs.toStreakState(),
            todayEpochDay = timeProvider.todayEpochDay()
        )

        _uiState.update { state ->
            state.copy(
                title = resolvedSessionTitle(),
                isLoading = false,
                isPracticeMode = result.isPracticeMode,
                queue = result.words,
                totalInitialCount = result.words.size,
                completedUniqueCount = 0,
                requeuedCount = 0,
                isFlipped = false,
                hasRevealedBack = false,
                cardInstanceKey = state.cardInstanceKey + 1,
                intervalPreviews = previews,
                speechRate = speechRate,
                autoPlayTts = autoPlayTts,
                enableHapticFeedback = enableHapticFeedback,
                isSessionComplete = result.words.isEmpty(),
                sessionSummary = SessionSummaryStats(currentStreak = effectiveStreak.currentStreak),
                remainingNewWordsInDeck = remainingNew
            )
        }
    }

    fun setScreenActive(active: Boolean) {
        isScreenActive = active
        if (!active) {
            ttsPlayer.stop()
        }
    }

    /**
     * Plays the English front-face word at most once per [StudyUiState.cardInstanceKey]
     * when Auto-play TTS is enabled, the screen is active, and the card is on its front face.
     */
    fun maybeAutoPlayCurrentCard() {
        val state = _uiState.value
        val currentWord = state.currentWord ?: return
        if (!isScreenActive || !state.autoPlayTts || state.isLoading || state.isSessionComplete || state.isFlipped) {
            return
        }
        if (lastAutoPlayedCardInstanceKey == state.cardInstanceKey) {
            return
        }
        lastAutoPlayedCardInstanceKey = state.cardInstanceKey
        ttsPlayer.stop()
        val feedback = ttsPlayer.speak(currentWord.word, state.speechRate)
        emitTtsFeedbackIfError(feedback)
    }

    fun toggleAutoPlayTts() {
        val next = !_uiState.value.autoPlayTts
        if (!next) {
            ttsPlayer.stop()
        }
        _uiState.update { it.copy(autoPlayTts = next) }
        viewModelScope.launch {
            userPreferencesRepository.setAutoPlayTts(next)
        }
    }

    fun flipCard() {
        if (isSubmitting) return
        val now = System.currentTimeMillis()
        if (now - lastFlipElapsedMs < FLIP_DEBOUNCE_MS) return
        lastFlipElapsedMs = now
        _uiState.update { current ->
            val nextFlipped = !current.isFlipped
            current.copy(
                isFlipped = nextFlipped,
                hasRevealedBack = current.hasRevealedBack || nextFlipped
            )
        }
    }

    fun speakWord() {
        val word = _uiState.value.currentWord?.word ?: return
        val feedback = ttsPlayer.speak(word, _uiState.value.speechRate)
        emitTtsFeedbackIfError(feedback)
    }

    fun speakExample() {
        val example = _uiState.value.currentWord?.exampleEn ?: return
        val feedback = ttsPlayer.speak(example, _uiState.value.speechRate)
        emitTtsFeedbackIfError(feedback)
    }

    fun toggleSpeechRate() {
        val currentRate = _uiState.value.speechRate
        val nextRate = if (currentRate > 0.85f) {
            TtsManager.SLOW_SPEECH_RATE
        } else {
            TtsManager.DEFAULT_SPEECH_RATE
        }
        _uiState.update { it.copy(speechRate = nextRate) }
        viewModelScope.launch {
            userPreferencesRepository.setSpeechRate(nextRate)
        }
    }

    fun submitRating(quality: ReviewQuality) {
        val currentWord = _uiState.value.currentWord ?: return
        if (isSubmitting) return
        isSubmitting = true
        ttsPlayer.stop()

        viewModelScope.launch {
            try {
                val updatedWord = submitReviewUseCase(currentWord, quality)
                val firstTimeEvaluated = evaluatedWordIds.add(currentWord.id)
                val isNewWordInSession = currentWord.id in initialNewWordIds
                val isImmediateRecall = quality == ReviewQuality.GOOD || quality == ReviewQuality.EASY

                val remainingCountPreview = _uiState.value.queue.size - 1
                val willFinish = remainingCountPreview <= 0 && quality != ReviewQuality.AGAIN
                val remainingNewInDeck = if (willFinish) {
                    getStudyQueueUseCase.countRemainingNewWords(deckId)
                } else {
                    _uiState.value.remainingNewWordsInDeck
                }
                val latestPrefs = if (willFinish) {
                    userPreferencesRepository.userPreferences.first()
                } else {
                    null
                }
                val latestStreak = latestPrefs?.let {
                    streakCalculator.effectiveStateForDisplay(
                        stored = it.toStreakState(),
                        todayEpochDay = timeProvider.todayEpochDay()
                    ).currentStreak
                } ?: _uiState.value.sessionSummary.currentStreak

                _uiState.update { state ->
                    val remaining = state.queue.drop(1)
                    val nextQueue = if (quality == ReviewQuality.AGAIN) {
                        remaining + updatedWord
                    } else {
                        remaining
                    }

                    val nextRequeuedCount = if (quality == ReviewQuality.AGAIN && requeuedIds.add(updatedWord.id)) {
                        state.requeuedCount + 1
                    } else {
                        state.requeuedCount
                    }

                    val nextCompletedUnique = if (quality != ReviewQuality.AGAIN) {
                        state.completedUniqueCount + 1
                    } else {
                        state.completedUniqueCount
                    }

                    val prevSummary = state.sessionSummary
                    val nextSummary = if (firstTimeEvaluated) {
                        prevSummary.copy(
                            newWordsStudied = prevSummary.newWordsStudied + if (isNewWordInSession) 1 else 0,
                            reviewedWordsCount = prevSummary.reviewedWordsCount + if (!isNewWordInSession) 1 else 0,
                            totalEvaluatedCount = prevSummary.totalEvaluatedCount + 1,
                            rememberedImmediatelyCount = prevSummary.rememberedImmediatelyCount + if (isImmediateRecall) 1 else 0,
                            currentStreak = latestStreak
                        )
                    } else {
                        prevSummary.copy(currentStreak = latestStreak)
                    }

                    val nextFrontWord = nextQueue.firstOrNull()
                    val nextPreviews = nextFrontWord?.let {
                        sm2Scheduler.previewAll(it, timeProvider.nowMillis())
                    }.orEmpty()

                    state.copy(
                        queue = nextQueue,
                        completedUniqueCount = nextCompletedUnique,
                        requeuedCount = nextRequeuedCount,
                        isFlipped = false,
                        hasRevealedBack = false,
                        cardInstanceKey = state.cardInstanceKey + 1,
                        intervalPreviews = nextPreviews,
                        isSessionComplete = nextQueue.isEmpty(),
                        sessionSummary = nextSummary,
                        remainingNewWordsInDeck = if (nextQueue.isEmpty()) remainingNewInDeck else state.remainingNewWordsInDeck
                    )
                }
            } finally {
                isSubmitting = false
            }
        }
    }

    fun startNextNewWordsSession() {
        if (isSubmitting) return
        viewModelScope.launch {
            val nextBatch = getStudyQueueUseCase.getNextNewWordsQueue(
                deckId = deckId,
                limit = GetStudyQueueUseCase.DEFAULT_DAILY_SESSION_LIMIT
            )
            if (nextBatch.words.isEmpty()) {
                _uiState.update { it.copy(remainingNewWordsInDeck = 0) }
                return@launch
            }
            val current = _uiState.value
            applyNewQueue(
                result = nextBatch,
                speechRate = current.speechRate,
                autoPlayTts = current.autoPlayTts,
                enableHapticFeedback = current.enableHapticFeedback
            )
        }
    }

    fun stopSpeaking() {
        ttsPlayer.stop()
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
        ttsPlayer.shutdown()
    }

    companion object {
        private const val TTS_WARMUP_DELAY_MS = 350L
        private const val FLIP_DEBOUNCE_MS = 200L

        fun provideFactory(
            topic: String?,
            hardWordsOnly: Boolean = false,
            deckId: String? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ToeicApplication
                val container = app.container
                StudyViewModel(
                    topic = topic,
                    hardWordsOnly = hardWordsOnly,
                    deckId = deckId,
                    getStudyQueueUseCase = container.getStudyQueueUseCase,
                    submitReviewUseCase = container.submitReviewUseCase,
                    sm2Scheduler = container.sm2Scheduler,
                    streakCalculator = container.streakCalculator,
                    timeProvider = container.timeProvider,
                    userPreferencesRepository = container.userPreferencesRepository,
                    createTtsPlayer = { onDeferred -> container.createTtsManager(onDeferred) }
                )
            }
        }
    }
}
