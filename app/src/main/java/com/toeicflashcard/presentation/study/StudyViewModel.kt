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
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.scheduler.Sm2Scheduler
import com.toeicflashcard.domain.usecase.GetStudyQueueUseCase
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
    private val timeProvider: TimeProvider,
    private val userPreferencesRepository: UserPreferencesRepository,
    createTtsManager: ((TtsFeedback) -> Unit) -> TtsManager
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

    private val _uiState = MutableStateFlow(
        buildInitialUiState(initialQueue = initialCachedQueue, cachedRate = cachedSpeechRate)
    )
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val _ttsMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val ttsMessages: SharedFlow<String> = _ttsMessages.asSharedFlow()

    private val ttsManager: TtsManager = createTtsManager { feedback ->
        emitTtsFeedbackIfError(feedback)
    }

    private val requeuedIds = mutableSetOf<Int>()
    private var isSubmitting = false

    init {
        if (initialCachedQueue == null) {
            loadSession()
        } else {
            if (cachedSpeechRate == null) {
                viewModelScope.launch {
                    val rate = userPreferencesRepository.userPreferences.first().speechRate
                    _uiState.update { it.copy(speechRate = rate) }
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                delay(TTS_WARMUP_DELAY_MS)
                ttsManager.warmUp()
            }
        }
    }

    private fun buildInitialUiState(
        initialQueue: com.toeicflashcard.domain.usecase.StudyQueueResult?,
        cachedRate: Float?
    ): StudyUiState {
        val rate = cachedRate ?: TtsManager.DEFAULT_SPEECH_RATE
        if (initialQueue == null) {
            return StudyUiState(
                title = resolvedSessionTitle(),
                speechRate = rate
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
            val firstWord = result.words.firstOrNull()
            val previews = firstWord?.let {
                sm2Scheduler.previewAll(it, timeProvider.nowMillis())
            }.orEmpty()

            _uiState.update {
                it.copy(
                    title = resolvedSessionTitle(),
                    isLoading = false,
                    isPracticeMode = result.isPracticeMode,
                    queue = result.words,
                    totalInitialCount = result.words.size,
                    completedUniqueCount = 0,
                    requeuedCount = 0,
                    isFlipped = false,
                    hasRevealedBack = false,
                    cardInstanceKey = 0,
                    intervalPreviews = previews,
                    speechRate = prefs.speechRate,
                    isSessionComplete = result.words.isEmpty()
                )
            }

            viewModelScope.launch(Dispatchers.IO) {
                delay(TTS_WARMUP_DELAY_MS)
                ttsManager.warmUp()
            }
        }
    }

    fun flipCard() {
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
        val feedback = ttsManager.speak(word, _uiState.value.speechRate)
        emitTtsFeedbackIfError(feedback)
    }

    fun speakExample() {
        val example = _uiState.value.currentWord?.exampleEn ?: return
        val feedback = ttsManager.speak(example, _uiState.value.speechRate)
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
        ttsManager.stop()

        viewModelScope.launch {
            try {
                val updatedWord = submitReviewUseCase(currentWord, quality)
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
                        isSessionComplete = nextQueue.isEmpty()
                    )
                }
            } finally {
                isSubmitting = false
            }
        }
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
        private const val TTS_WARMUP_DELAY_MS = 350L

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
                    timeProvider = container.timeProvider,
                    userPreferencesRepository = container.userPreferencesRepository,
                    createTtsManager = { onDeferred -> container.createTtsManager(onDeferred) }
                )
            }
        }
    }
}
