package com.toeicflashcard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.data.local.preferences.userPreferencesDataStore
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.UserPreferences
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.streak.StreakCalculator
import com.toeicflashcard.domain.streak.StreakState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesRepositoryImpl internal constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    constructor(context: Context) : this(context.applicationContext.userPreferencesDataStore)

    private val glassOpacityPreview = MutableStateFlow<Float?>(null)

    @Volatile
    private var cachedSpeechRate: Float? = null

    @Volatile
    private var cachedSelectedDeckId: String? = null

    private val persistedPreferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeName = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = ThemeMode.entries.firstOrNull { it.name == themeName } ?: ThemeMode.SYSTEM
            val darkTone = DarkTone.fromId(prefs[KEY_DARK_TONE])
            val speechRate = prefs[KEY_SPEECH_RATE] ?: TtsManager.DEFAULT_SPEECH_RATE
            cachedSpeechRate = speechRate
            val dailyGoal = prefs[KEY_DAILY_GOAL] ?: DEFAULT_DAILY_GOAL
            val currentStreak = prefs[KEY_CURRENT_STREAK] ?: 0
            val lastStudyDate = prefs[KEY_LAST_STUDY_DATE] ?: StreakState.NO_STUDY_DATE
            val dailyReviewedCount = prefs[KEY_DAILY_REVIEWED_COUNT] ?: 0
            val reminderEnabled = prefs[KEY_REMINDER_ENABLED] ?: false
            val reminderHour = (prefs[KEY_REMINDER_HOUR] ?: DEFAULT_REMINDER_HOUR).coerceIn(0, 23)
            val reminderMinute = (prefs[KEY_REMINDER_MINUTE] ?: DEFAULT_REMINDER_MINUTE).coerceIn(0, 59)
            val glassOpacity = prefs[KEY_GLASS_OPACITY]?.coerceIn(
                UserPreferences.MIN_GLASS_OPACITY,
                UserPreferences.MAX_GLASS_OPACITY
            )
            val dailyReviewHistory = decodeDailyReviewHistory(prefs[KEY_DAILY_REVIEW_HISTORY])
            val selectedDeckId = VocabularyDecks.normalizeSelectedDeckId(prefs[KEY_SELECTED_DECK_ID])
            cachedSelectedDeckId = selectedDeckId
            UserPreferences(
                themeMode = themeMode,
                darkTone = darkTone,
                speechRate = speechRate,
                dailyGoal = dailyGoal,
                currentStreak = currentStreak,
                lastStudyDateEpochDay = lastStudyDate,
                dailyReviewedCount = dailyReviewedCount,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                glassOpacity = glassOpacity,
                dailyReviewHistory = dailyReviewHistory,
                selectedDeckId = selectedDeckId
            )
        }

    override val userPreferences: Flow<UserPreferences> = combine(
        persistedPreferences,
        glassOpacityPreview
    ) { prefs, previewOpacity ->
        if (previewOpacity != null) {
            prefs.copy(glassOpacity = previewOpacity)
        } else {
            prefs
        }
    }.distinctUntilChanged()

    override fun getCachedSpeechRate(): Float? = cachedSpeechRate

    override fun getCachedSelectedDeckId(): String? = cachedSelectedDeckId

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    override suspend fun setDarkTone(tone: DarkTone, forceDarkMode: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DARK_TONE] = tone.id
            if (forceDarkMode) {
                prefs[KEY_THEME_MODE] = ThemeMode.DARK.name
            }
        }
    }

    override suspend fun setSpeechRate(rate: Float) {
        cachedSpeechRate = rate
        dataStore.edit { prefs ->
            prefs[KEY_SPEECH_RATE] = rate
        }
    }

    override suspend fun setDailyGoal(goal: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_GOAL] = goal.coerceAtLeast(1)
        }
    }

    override suspend fun setSelectedDeckId(deckId: String) {
        val normalized = VocabularyDecks.normalizeSelectedDeckId(deckId)
        cachedSelectedDeckId = normalized
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_DECK_ID] = normalized
        }
    }

    override suspend fun recordStudyReview(todayEpochDay: Long, calculator: StreakCalculator) {
        dataStore.edit { prefs ->
            val currentState = StreakState(
                currentStreak = prefs[KEY_CURRENT_STREAK] ?: 0,
                lastStudyDateEpochDay = prefs[KEY_LAST_STUDY_DATE] ?: StreakState.NO_STUDY_DATE,
                dailyReviewedCount = prefs[KEY_DAILY_REVIEWED_COUNT] ?: 0
            )
            val updated = calculator.onCardReviewed(currentState, todayEpochDay)
            prefs[KEY_CURRENT_STREAK] = updated.currentStreak
            prefs[KEY_LAST_STUDY_DATE] = updated.lastStudyDateEpochDay
            prefs[KEY_DAILY_REVIEWED_COUNT] = updated.dailyReviewedCount

            val history = decodeDailyReviewHistory(prefs[KEY_DAILY_REVIEW_HISTORY]).toMutableMap()
            val minKeepDay = todayEpochDay - (MAX_HISTORY_DAYS - 1)
            history.keys.removeAll { it < minKeepDay || it > todayEpochDay }
            val previousTodayCount = maxOf(
                history[todayEpochDay] ?: 0,
                if (currentState.lastStudyDateEpochDay == todayEpochDay) currentState.dailyReviewedCount else 0
            )
            history[todayEpochDay] = maxOf(previousTodayCount + 1, updated.dailyReviewedCount)
            prefs[KEY_DAILY_REVIEW_HISTORY] = encodeDailyReviewHistory(history)
        }
    }

    override suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_REMINDER_ENABLED] = enabled
            prefs[KEY_REMINDER_HOUR] = hour.coerceIn(0, 23)
            prefs[KEY_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    override fun previewGlassOpacity(opacity: Float?) {
        glassOpacityPreview.value = opacity?.coerceIn(
            UserPreferences.MIN_GLASS_OPACITY,
            UserPreferences.MAX_GLASS_OPACITY
        )
    }

    override suspend fun setGlassOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(
            UserPreferences.MIN_GLASS_OPACITY,
            UserPreferences.MAX_GLASS_OPACITY
        )
        glassOpacityPreview.value = clamped
        dataStore.edit { prefs ->
            prefs[KEY_GLASS_OPACITY] = clamped
        }
        glassOpacityPreview.value = null
    }

    override suspend fun resetGlassOpacity() {
        glassOpacityPreview.value = null
        dataStore.edit { prefs ->
            prefs.remove(KEY_GLASS_OPACITY)
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DARK_TONE = stringPreferencesKey("dark_tone")
        private val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        private val KEY_DAILY_GOAL = intPreferencesKey("daily_goal")
        private val KEY_CURRENT_STREAK = intPreferencesKey("current_streak")
        private val KEY_LAST_STUDY_DATE = longPreferencesKey("last_study_date_epoch_day")
        private val KEY_DAILY_REVIEWED_COUNT = intPreferencesKey("daily_reviewed_count")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val KEY_GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        private val KEY_DAILY_REVIEW_HISTORY = stringPreferencesKey("daily_review_history")
        private val KEY_SELECTED_DECK_ID = stringPreferencesKey("selected_deck_id")

        private const val DEFAULT_DAILY_GOAL = 10
        private const val DEFAULT_REMINDER_HOUR = 20
        private const val DEFAULT_REMINDER_MINUTE = 0
        private const val MAX_HISTORY_DAYS = 14L

        private fun decodeDailyReviewHistory(raw: String?): Map<Long, Int> {
            if (raw.isNullOrBlank()) return emptyMap()
            val result = LinkedHashMap<Long, Int>()
            raw.split(',').forEach { entry ->
                val idx = entry.indexOf(':')
                if (idx <= 0 || idx >= entry.lastIndex) return@forEach
                val day = entry.substring(0, idx).toLongOrNull() ?: return@forEach
                val count = entry.substring(idx + 1).toIntOrNull() ?: return@forEach
                if (count > 0) {
                    result[day] = count
                }
            }
            return result
        }

        private fun encodeDailyReviewHistory(history: Map<Long, Int>): String {
            return history.entries
                .filter { it.value > 0 }
                .sortedBy { it.key }
                .joinToString(",") { "${it.key}:${it.value}" }
        }
    }
}
