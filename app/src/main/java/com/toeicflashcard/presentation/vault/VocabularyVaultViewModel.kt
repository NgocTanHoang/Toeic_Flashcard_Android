package com.toeicflashcard.presentation.vault

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
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.repository.VocabularyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class VaultFilter(val label: String) {
    ALL("Tất cả"),
    FORGOTTEN("Hay quên"),
    LEARNING("Đang học"),
    LEARNED("Đã thuộc")
}

@Immutable
data class VocabularyVaultUiState(
    val isLoading: Boolean = true,
    val words: List<VocabularyWord> = emptyList(),
    val totalCount: Int = 0,
    val counts: Map<VaultFilter, Int> = emptyMap(),
    val selectedFilter: VaultFilter = VaultFilter.ALL,
    val selectedDeckId: String = VocabularyDecks.ALL_ID,
    val selectedDeckName: String = VocabularyDecks.ALL_NAME,
    val speechRate: Float = TtsManager.DEFAULT_SPEECH_RATE
)

class VocabularyVaultViewModel(
    vocabularyRepository: VocabularyRepository,
    userPreferencesRepository: UserPreferencesRepository,
    createTtsManager: ((TtsFeedback) -> Unit) -> TtsManager,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(VaultFilter.ALL)

    private val _ttsMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val ttsMessages: SharedFlow<String> = _ttsMessages.asSharedFlow()

    private val ttsManager: TtsManager = createTtsManager { feedback ->
        emitTtsFeedbackIfError(feedback)
    }

    val uiState: StateFlow<VocabularyVaultUiState> = combine(
        vocabularyRepository.observeAllWords(),
        userPreferencesRepository.userPreferences
            .map {
                it.speechRate to VocabularyDecks.normalizeSelectedDeckId(it.selectedDeckId)
            }
            .distinctUntilChanged(),
        searchQuery,
        selectedFilter
    ) { allWords, (speechRate, activeDeckId), query, filter ->
        val deckScopedWords = if (activeDeckId == VocabularyDecks.ALL_ID) {
            allWords
        } else {
            allWords.filter { it.deckId == activeDeckId }
        }

        var forgottenCount = 0
        var learningCount = 0
        var learnedCount = 0
        deckScopedWords.forEach { word ->
            if (isForgotten(word)) forgottenCount++
            if (word.isLearned) learnedCount++ else learningCount++
        }

        val counts = mapOf(
            VaultFilter.ALL to deckScopedWords.size,
            VaultFilter.FORGOTTEN to forgottenCount,
            VaultFilter.LEARNING to learningCount,
            VaultFilter.LEARNED to learnedCount
        )

        val trimmed = query.trim()
        val filteredWords = deckScopedWords.filter { word ->
            val matchesFilter = when (filter) {
                VaultFilter.ALL -> true
                VaultFilter.FORGOTTEN -> isForgotten(word)
                VaultFilter.LEARNING -> !word.isLearned
                VaultFilter.LEARNED -> word.isLearned
            }
            if (!matchesFilter) return@filter false
            if (trimmed.isEmpty()) return@filter true
            word.word.contains(trimmed, ignoreCase = true) ||
                word.meaningVi.contains(trimmed, ignoreCase = true) ||
                word.topic.contains(trimmed, ignoreCase = true) ||
                word.phonetic.contains(trimmed, ignoreCase = true) ||
                word.deckName.contains(trimmed, ignoreCase = true)
        }

        VocabularyVaultUiState(
            isLoading = allWords.isEmpty(),
            words = filteredWords,
            totalCount = deckScopedWords.size,
            counts = counts,
            selectedFilter = filter,
            selectedDeckId = activeDeckId,
            selectedDeckName = VocabularyDecks.displayNameFor(activeDeckId),
            speechRate = speechRate
        )
    }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = VocabularyVaultUiState()
        )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectFilter(filter: VaultFilter) {
        selectedFilter.value = filter
    }

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
        fun isForgotten(word: VocabularyWord): Boolean {
            return word.lastReviewedTimestamp > 0L &&
                (word.repetitions == 0 || word.easeFactor < 2.2f)
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ToeicApplication
                val container = app.container
                VocabularyVaultViewModel(
                    vocabularyRepository = container.vocabularyRepository,
                    userPreferencesRepository = container.userPreferencesRepository,
                    createTtsManager = { onDeferred -> container.createTtsManager(onDeferred) }
                )
            }
        }
    }
}
