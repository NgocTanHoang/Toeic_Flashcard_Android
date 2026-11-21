package com.toeicflashcard.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toeicflashcard.ToeicApplication
import com.toeicflashcard.core.audio.TtsFeedback
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.usecase.GenerateQuizUseCase
import com.toeicflashcard.domain.usecase.MarkWordsUrgentReviewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

class QuizViewModel(
    private val topic: String?,
    private val isListeningMode: Boolean = false,
    private val deckId: String? = null,
    private val generateQuizUseCase: GenerateQuizUseCase,
    private val markWordsUrgentReviewUseCase: MarkWordsUrgentReviewUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    createTtsManager: ((TtsFeedback) -> Unit) -> TtsManager
) : ViewModel() {

    private val initialCachedQuestions = generateQuizUseCase.generateFromCacheIfAvailable(
        topic = topic,
        deckId = deckId
    )
    private val cachedSpeechRate = userPreferencesRepository.getCachedSpeechRate()

    private val _uiState = MutableStateFlow(
        buildInitialUiState(
            initialQuestions = initialCachedQuestions,
            cachedRate = cachedSpeechRate
        )
    )
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val ttsManager: TtsManager = createTtsManager { feedback ->
        emitTtsFeedbackIfError(feedback)
    }

    private var autoAdvanceJob: Job? = null
    private var lastAdvanceEpochMs = 0L

    init {
        val questions = initialCachedQuestions
        if (questions == null) {
            loadQuiz()
        } else {
            val needSyncRate = cachedSpeechRate == null
            if (isListeningMode && questions.isNotEmpty()) {
                viewModelScope.launch {
                    if (needSyncRate) {
                        val rate = userPreferencesRepository.userPreferences.first().speechRate
                        _uiState.update { it.copy(speechRate = rate) }
                    }
                    delay(LISTENING_INITIAL_SPEAK_DELAY_MS)
                    speakWord(questions.first().word.word)
                }
            } else {
                if (needSyncRate) {
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
    }

    private fun buildInitialUiState(
        initialQuestions: List<com.toeicflashcard.domain.model.QuizQuestion>?,
        cachedRate: Float?
    ): QuizUiState {
        val rate = cachedRate ?: TtsManager.DEFAULT_SPEECH_RATE
        if (initialQuestions == null) {
            return QuizUiState(
                title = resolvedTitle(),
                isListeningMode = isListeningMode,
                speechRate = rate
            )
        }
        return QuizUiState(
            title = resolvedTitle(),
            isLoading = false,
            isListeningMode = isListeningMode,
            questions = initialQuestions,
            currentIndex = 0,
            selectedOptionIndex = null,
            correctCount = 0,
            wrongWords = emptyList(),
            speechRate = rate,
            isFinished = initialQuestions.isEmpty(),
            isUrgentReviewSaving = false,
            isUrgentReviewSaved = false
        )
    }

    private fun resolvedTitle(): String {
        val activeDeckLabel = deckId
            ?.let { VocabularyDecks.normalizeSelectedDeckId(it) }
            ?.takeIf { it != VocabularyDecks.ALL_ID }
            ?.let { VocabularyDecks.shortLabelFor(it) }
        return if (isListeningMode) {
            topic?.let { "Nghe • $it" }
                ?: activeDeckLabel?.let { "Nghe • $it" }
                ?: "Luyện nghe từ vựng"
        } else {
            topic
                ?: activeDeckLabel?.let { "Kiểm tra • $it" }
                ?: "Kiểm tra nhanh"
        }
    }

    private fun loadQuiz() {
        autoAdvanceJob?.cancel()
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferences.first()
            val questions = generateQuizUseCase(topic = topic, deckId = deckId)
            _uiState.value = QuizUiState(
                title = resolvedTitle(),
                isLoading = false,
                isListeningMode = isListeningMode,
                questions = questions,
                currentIndex = 0,
                selectedOptionIndex = null,
                correctCount = 0,
                wrongWords = emptyList(),
                speechRate = prefs.speechRate,
                isFinished = questions.isEmpty(),
                isUrgentReviewSaving = false,
                isUrgentReviewSaved = false
            )
            if (isListeningMode && questions.isNotEmpty()) {
                speakWord(questions.first().word.word)
            } else if (questions.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    delay(TTS_WARMUP_DELAY_MS)
                    ttsManager.warmUp()
                }
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        val currentState = _uiState.value
        if (currentState.selectedOptionIndex != null) return
        val question = currentState.currentQuestion ?: return
        val selectedOption = question.options.getOrNull(optionIndex) ?: return

        val isCorrect = selectedOption.isCorrect
        _uiState.update { state ->
            state.copy(
                selectedOptionIndex = optionIndex,
                correctCount = if (isCorrect) state.correctCount + 1 else state.correctCount,
                wrongWords = if (isCorrect) state.wrongWords else state.wrongWords + question.word
            )
        }

        if (!currentState.isListeningMode) {
            speakWord(question.word.word)
        }

        val delayMs = if (currentState.isListeningMode) {
            LISTENING_AUTO_ADVANCE_DELAY_MS
        } else {
            AUTO_ADVANCE_DELAY_MS
        }
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(delayMs)
            nextQuestion()
        }
    }

    fun nextQuestion() {
        val now = System.currentTimeMillis()
        if (now - lastAdvanceEpochMs < NEXT_QUESTION_DEBOUNCE_MS) return
        lastAdvanceEpochMs = now
        autoAdvanceJob?.cancel()
        ttsManager.stop()
        var wordToSpeak: String? = null
        _uiState.update { state ->
            if (state.isFinished || state.questions.isEmpty()) return@update state
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.questions.size) {
                state.copy(
                    selectedOptionIndex = null,
                    isFinished = true
                )
            } else {
                if (state.isListeningMode) {
                    wordToSpeak = state.questions.getOrNull(nextIndex)?.word?.word
                }
                state.copy(
                    currentIndex = nextIndex,
                    selectedOptionIndex = null
                )
            }
        }
        wordToSpeak?.let { speakWord(it) }
    }

    fun toggleSpeechRate() {
        val nextRate = if (_uiState.value.speechRate > 0.85f) {
            TtsManager.SLOW_SPEECH_RATE
        } else {
            TtsManager.DEFAULT_SPEECH_RATE
        }
        _uiState.update { it.copy(speechRate = nextRate) }
        viewModelScope.launch {
            userPreferencesRepository.setSpeechRate(nextRate)
        }
    }

    fun speakCurrentWord() {
        val word = _uiState.value.currentQuestion?.word?.word ?: return
        speakWord(word)
    }

    fun speakWord(word: String) {
        val feedback = ttsManager.speak(word, _uiState.value.speechRate)
        emitTtsFeedbackIfError(feedback)
    }

    fun markWrongWordsForUrgentReview() {
        val current = _uiState.value
        if (current.wrongWords.isEmpty() || current.isUrgentReviewSaving || current.isUrgentReviewSaved) {
            return
        }
        _uiState.update { it.copy(isUrgentReviewSaving = true) }
        viewModelScope.launch {
            try {
                val ids = current.wrongWords.map { it.id }
                markWordsUrgentReviewUseCase(ids)
                _uiState.update {
                    it.copy(
                        isUrgentReviewSaving = false,
                        isUrgentReviewSaved = true
                    )
                }
                _messages.tryEmit("Đã đưa ${ids.distinct().size} từ sai vào diện ôn tập gấp!")
            } catch (_: Exception) {
                _uiState.update { it.copy(isUrgentReviewSaving = false) }
                _messages.tryEmit("Không thể cập nhật lịch ôn tập lúc này.")
            }
        }
    }

    fun restartQuiz() {
        ttsManager.stop()
        _uiState.update { it.copy(isLoading = true) }
        loadQuiz()
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    private fun emitTtsFeedbackIfError(feedback: TtsFeedback) {
        when (feedback) {
            is TtsFeedback.MissingVoiceData -> _messages.tryEmit(feedback.messageVi)
            is TtsFeedback.Unavailable -> _messages.tryEmit(feedback.messageVi)
            TtsFeedback.Spoken, TtsFeedback.Initializing -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
        ttsManager.shutdown()
    }

    companion object {
        private const val AUTO_ADVANCE_DELAY_MS = 1_200L
        private const val LISTENING_AUTO_ADVANCE_DELAY_MS = 1_600L
        private const val LISTENING_INITIAL_SPEAK_DELAY_MS = 300L
        private const val TTS_WARMUP_DELAY_MS = 350L
        private const val NEXT_QUESTION_DEBOUNCE_MS = 250L

        fun provideFactory(
            topic: String?,
            isListeningMode: Boolean = false,
            deckId: String? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ToeicApplication
                val container = app.container
                QuizViewModel(
                    topic = topic,
                    isListeningMode = isListeningMode,
                    deckId = deckId,
                    generateQuizUseCase = container.generateQuizUseCase,
                    markWordsUrgentReviewUseCase = container.markWordsUrgentReviewUseCase,
                    userPreferencesRepository = container.userPreferencesRepository,
                    createTtsManager = { onDeferred -> container.createTtsManager(onDeferred) }
                )
            }
        }
    }
}
