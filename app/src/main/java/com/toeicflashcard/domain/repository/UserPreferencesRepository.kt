package com.toeicflashcard.domain.repository

import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.UserPreferences
import com.toeicflashcard.domain.streak.StreakCalculator
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    fun getCachedSpeechRate(): Float? = null
    fun getCachedSelectedDeckId(): String? = null
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDarkTone(tone: DarkTone, forceDarkMode: Boolean = false)
    suspend fun setSpeechRate(rate: Float)
    suspend fun setDailyGoal(goal: Int)
    suspend fun setSelectedDeckId(deckId: String)
    suspend fun recordStudyReview(todayEpochDay: Long, calculator: StreakCalculator)
    suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int)
    fun previewGlassOpacity(opacity: Float?)
    suspend fun setGlassOpacity(opacity: Float)
    suspend fun resetGlassOpacity()
}
