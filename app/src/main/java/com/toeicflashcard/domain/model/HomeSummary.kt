package com.toeicflashcard.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeSummary(
    val dueTodayCount: Int,
    val learnedCount: Int,
    val totalCount: Int,
    val topics: List<TopicProgress>,
    val decks: List<DeckProgress> = emptyList(),
    val selectedDeckId: String = VocabularyDecks.ALL_ID,
    val hasAnyDatabaseWords: Boolean = totalCount > 0
)
