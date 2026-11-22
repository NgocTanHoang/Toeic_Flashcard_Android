package com.toeicflashcard.domain.model

import androidx.compose.runtime.Immutable

object VocabularyDecks {
    const val ALL_ID = "ALL"
    const val ALL_NAME = "Tất cả bộ từ"
    const val ESSENTIAL_600_ID = "essential_600"
    const val ESSENTIAL_600_NAME = "600 Từ TOEIC Cốt Lõi"
    const val LEVEL_450_800_ID = "level_450_800"
    const val LEVEL_450_800_NAME = "800 Từ TOEIC Nâng Cao"
    const val CORE_775_ID = "core_775"
    const val CORE_775_NAME = "775 Từ TOEIC Mở Rộng"
    const val CUSTOM_USER_ID = "custom_user"
    const val CUSTOM_USER_NAME = "Từ Vựng Của Tôi"
    const val DEFAULT_SELECTED_DECK_ID = ESSENTIAL_600_ID

    val orderedSelectableIds: List<String> = listOf(
        ESSENTIAL_600_ID,
        CORE_775_ID,
        LEVEL_450_800_ID,
        CUSTOM_USER_ID,
        ALL_ID
    )

    val orderedCurriculumIds: List<String> = listOf(
        ESSENTIAL_600_ID,
        CORE_775_ID,
        LEVEL_450_800_ID,
        CUSTOM_USER_ID
    )

    fun displayNameFor(deckId: String): String = when (deckId) {
        ALL_ID -> ALL_NAME
        ESSENTIAL_600_ID -> ESSENTIAL_600_NAME
        LEVEL_450_800_ID -> LEVEL_450_800_NAME
        CORE_775_ID -> CORE_775_NAME
        CUSTOM_USER_ID -> CUSTOM_USER_NAME
        else -> ESSENTIAL_600_NAME
    }

    fun selectorCardTitleFor(deckId: String): String = when (deckId) {
        ESSENTIAL_600_ID -> "600 Từ Cốt Lõi"
        CORE_775_ID -> "775 Từ Mở Rộng"
        LEVEL_450_800_ID -> "800 Từ Nâng Cao"
        CUSTOM_USER_ID -> "Từ Của Tôi"
        ALL_ID -> "Tất cả"
        else -> "600 Từ Cốt Lõi"
    }

    fun shortLabelFor(deckId: String): String = when (deckId) {
        ALL_ID -> "Tất cả"
        ESSENTIAL_600_ID -> "600 Cốt lõi"
        LEVEL_450_800_ID -> "800 Nâng cao"
        CORE_775_ID -> "775 Mở rộng"
        CUSTOM_USER_ID -> "Từ của tôi"
        else -> "600 Cốt lõi"
    }

    fun dashboardTitleFor(deckId: String): String = when (deckId) {
        ESSENTIAL_600_ID -> "600 Từ TOEIC Cốt Lõi"
        CORE_775_ID -> "775 Từ TOEIC Mở Rộng"
        LEVEL_450_800_ID -> "800 Từ TOEIC Nâng Cao"
        CUSTOM_USER_ID -> "Từ Vựng Của Tôi"
        else -> "600 Từ TOEIC Cốt Lõi"
    }

    fun descriptionFor(deckId: String): String = when (deckId) {
        ESSENTIAL_600_ID -> "50 chủ đề nền tảng thiết yếu cho mục tiêu 500–650+ TOEIC"
        CORE_775_ID -> "36 chủ đề giao tiếp công sở & thương mại mở rộng"
        LEVEL_450_800_ID -> "18 nhóm từ học thuật & ngữ cảnh khó cho 750–850+ TOEIC"
        CUSTOM_USER_ID -> "Bộ từ vựng cá nhân bạn tự thêm và quản lý riêng"
        else -> "Lộ trình từ vựng TOEIC theo thuật toán lặp lại ngắt quãng SM-2"
    }

    fun normalizeSelectedDeckId(rawId: String?): String {
        if (rawId.isNullOrBlank()) return DEFAULT_SELECTED_DECK_ID
        return if (rawId in orderedSelectableIds) rawId else DEFAULT_SELECTED_DECK_ID
    }

    fun normalizeWordKey(rawWord: String): String = rawWord.trim().lowercase(java.util.Locale.ROOT)
}

@Immutable
data class DeckProgress(
    val deckId: String,
    val deckName: String,
    val learnedCount: Int,
    val totalCount: Int,
    val dueCount: Int,
    val topicCount: Int = 0
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
    val deckName: String = VocabularyDecks.CORE_775_NAME,
    val isStarred: Boolean = false
) {
    val isLearned: Boolean
        get() = repetitions >= 1

    fun isDue(cutoffMillis: Long): Boolean = nextReviewTimestamp <= cutoffMillis
}
