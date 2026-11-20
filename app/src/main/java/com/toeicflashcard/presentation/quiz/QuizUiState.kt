package com.toeicflashcard.presentation.quiz

import androidx.compose.runtime.Immutable
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.domain.model.QuizQuestion
import com.toeicflashcard.domain.model.VocabularyWord

@Immutable
data class QuizUiState(
    val title: String = "Kiểm tra nhanh",
    val isLoading: Boolean = true,
    val isListeningMode: Boolean = false,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val correctCount: Int = 0,
    val wrongWords: List<VocabularyWord> = emptyList(),
    val speechRate: Float = TtsManager.DEFAULT_SPEECH_RATE,
    val isFinished: Boolean = false,
    val isUrgentReviewSaving: Boolean = false,
    val isUrgentReviewSaved: Boolean = false
) {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)

    val hasAnsweredCurrent: Boolean
        get() = selectedOptionIndex != null
}
