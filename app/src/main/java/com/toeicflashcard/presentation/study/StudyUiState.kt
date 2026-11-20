package com.toeicflashcard.presentation.study

import androidx.compose.runtime.Immutable
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.VocabularyWord

@Immutable
data class StudyUiState(
    val title: String = "Ôn tập hôm nay",
    val isLoading: Boolean = true,
    val isPracticeMode: Boolean = false,
    val queue: List<VocabularyWord> = emptyList(),
    val totalInitialCount: Int = 0,
    val completedUniqueCount: Int = 0,
    val requeuedCount: Int = 0,
    val isFlipped: Boolean = false,
    val hasRevealedBack: Boolean = false,
    val cardInstanceKey: Int = 0,
    val intervalPreviews: Map<ReviewQuality, String> = emptyMap(),
    val speechRate: Float = TtsManager.DEFAULT_SPEECH_RATE,
    val isSessionComplete: Boolean = false
) {
    val currentWord: VocabularyWord?
        get() = queue.firstOrNull()
}
