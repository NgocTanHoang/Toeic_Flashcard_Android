package com.toeicflashcard.domain.model

import androidx.compose.runtime.Immutable

object VocabularyDecks {
    const val ALL_ID = "ALL"
    const val ALL_NAME = "Tất cả bộ từ"
    const val ESSENTIAL_600_ID = "essential_600"
    const val ESSENTIAL_600_NAME = "600 Essential Words"
    const val LEVEL_450_800_ID = "level_450_800"
    const val LEVEL_450_800_NAME = "800 Từ TOEIC Level 450"
    const val CORE_775_ID = "core_775"
    const val CORE_775_NAME = "TOEIC Cốt Lõi (775)"
    const val CUSTOM_USER_ID = "custom_user"
    const val CUSTOM_USER_NAME = "Từ vựng tự thêm"

    val orderedSelectableIds: List<String> = listOf(
        ALL_ID,
        ESSENTIAL_600_ID,
        LEVEL_450_800_ID,
        CORE_775_ID,
        CUSTOM_USER_ID
    )

    val orderedCurriculumIds: List<String> = listOf(
        ESSENTIAL_600_ID,
        LEVEL_450_800_ID,
        CORE_775_ID,
        CUSTOM_USER_ID
    )

    fun displayNameFor(deckId: String): String = when (deckId) {
        ALL_ID -> ALL_NAME
        ESSENTIAL_600_ID -> ESSENTIAL_600_NAME
        LEVEL_450_800_ID -> LEVEL_450_800_NAME
        CORE_775_ID -> CORE_775_NAME
        CUSTOM_USER_ID -> CUSTOM_USER_NAME
        else -> ALL_NAME
    }

    fun shortLabelFor(deckId: String): String = when (deckId) {
        ALL_ID -> "Tất cả"
        ESSENTIAL_600_ID -> "600 Essential"
        LEVEL_450_800_ID -> "800 Level 450"
        CORE_775_ID -> "Cốt lõi 775"
        CUSTOM_USER_ID -> "Tự thêm"
        else -> "Tất cả"
    }

    fun normalizeSelectedDeckId(rawId: String?): String {
        if (rawId.isNullOrBlank()) return ALL_ID
        return if (rawId in orderedSelectableIds) rawId else ALL_ID
    }
}

@Immutable
data class DeckProgress(
    val deckId: String,
    val deckName: String,
    val learnedCount: Int,
    val totalCount: Int,
    val dueCount: Int
)

@Immutable
data class VocabularyWord(
    val id: Int,
    val topic: String,
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val meaningVi: String,
    val exampleEn: String,
    val exampleVi: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val nextReviewTimestamp: Long = 0L,
    val lastReviewedTimestamp: Long = 0L,
    val deckId: String = VocabularyDecks.CORE_775_ID,
    val deckName: String = VocabularyDecks.CORE_775_NAME
) {
    val isLearned: Boolean
        get() = repetitions >= 1

    fun isDue(cutoffMillis: Long): Boolean = nextReviewTimestamp <= cutoffMillis
}
