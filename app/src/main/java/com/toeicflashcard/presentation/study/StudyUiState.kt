package com.toeicflashcard.presentation.study

import androidx.compose.runtime.Immutable
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.VocabularyWord
import kotlin.math.roundToInt

@Immutable
data class SessionSummaryStats(
    val newWordsStudied: Int = 0,
    val reviewedWordsCount: Int = 0,
    val totalEvaluatedCount: Int = 0,
    val rememberedImmediatelyCount: Int = 0,
    val currentStreak: Int = 0
) {
    val recallRatePercent: Int
        get() = if (totalEvaluatedCount > 0) {
            ((rememberedImmediatelyCount.toFloat() / totalEvaluatedCount.toFloat()) * 100f).roundToInt()
        } else {
            0
        }
}

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
    val autoPlayTts: Boolean = true,
    val enableHapticFeedback: Boolean = false,
    val isSessionComplete: Boolean = false,
    val sessionSummary: SessionSummaryStats = SessionSummaryStats(),
    val remainingNewWordsInDeck: Int = 0
) {
    val currentWord: VocabularyWord?
        get() = queue.firstOrNull()
}
