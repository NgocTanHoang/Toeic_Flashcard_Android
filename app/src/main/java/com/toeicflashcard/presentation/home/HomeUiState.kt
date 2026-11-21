package com.toeicflashcard.presentation.home

import androidx.compose.runtime.Immutable
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.DeckProgress
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.TopicProgress
import com.toeicflashcard.domain.model.VocabularyDecks

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val hasDatabaseWords: Boolean = false,
    val selectedDeckId: String = VocabularyDecks.ALL_ID,
    val selectedDeckName: String = VocabularyDecks.ALL_NAME,
    val decks: List<DeckProgress> = emptyList(),
    val dueTodayCount: Int = 0,
    val reviewDueCount: Int = 0,
    val newWordsCount: Int = 0,
    val learnedCount: Int = 0,
    val totalCount: Int = 0,
    val topics: List<TopicProgress> = emptyList(),
    val filteredTopics: List<TopicProgress> = emptyList(),
    val continueTopic: TopicProgress? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val darkTone: DarkTone = DarkTone.OLED_OBSIDIAN,
    val speechRate: Float = TtsManager.DEFAULT_SPEECH_RATE,
    val currentStreak: Int = 0,
    val hasStudiedToday: Boolean = false,
    val dailyReviewedCount: Int = 0,
    val dailyGoal: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val glassOpacity: Float? = null
)
