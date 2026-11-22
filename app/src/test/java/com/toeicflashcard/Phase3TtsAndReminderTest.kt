package com.toeicflashcard

import android.speech.tts.TextToSpeech
import com.toeicflashcard.core.audio.TtsLanguageStatus
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.core.notification.ReminderBootRestorer
import com.toeicflashcard.core.notification.ReminderScheduler
import com.toeicflashcard.core.notification.WorkManagerReminderScheduler
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.UserPreferences
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.streak.StreakCalculator
import com.toeicflashcard.domain.usecase.AddCustomVocabularyUseCase
import com.toeicflashcard.domain.usecase.ObserveHomeSummaryUseCase
import com.toeicflashcard.presentation.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class Phase3TtsAndReminderTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class RecordingReminderScheduler : ReminderScheduler {
        var activeSchedule: Pair<Int, Int>? = null
        var scheduleCalls = 0
        var cancelCalls = 0

        override fun scheduleDailyReminder(hour: Int, minute: Int) {
            scheduleCalls++
            activeSchedule = hour to minute
        }

        override fun cancelDailyReminder() {
            cancelCalls++
            activeSchedule = null
        }
    }

    private class FakePrefsRepo(
        initial: UserPreferences = UserPreferences()
    ) : UserPreferencesRepository {
        private val flow = MutableStateFlow(initial)
        override val userPreferences: Flow<UserPreferences> = flow
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setDarkTone(tone: DarkTone, forceDarkMode: Boolean) = Unit
        override suspend fun setSpeechRate(rate: Float) = Unit
        override suspend fun setAutoPlayTts(enabled: Boolean) = Unit
        override suspend fun setEnableHapticFeedback(enabled: Boolean) = Unit
        override suspend fun setDailyGoal(goal: Int) = Unit
        override suspend fun setSelectedDeckId(deckId: String) = Unit
        override suspend fun recordStudyReview(todayEpochDay: Long, calculator: StreakCalculator) = Unit
        override suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
            flow.value = flow.value.copy(
                reminderEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute
            )
        }
        override fun previewGlassOpacity(opacity: Float?) = Unit
        override suspend fun setGlassOpacity(opacity: Float) = Unit
        override suspend fun resetGlassOpacity() = Unit
    }

    private class StubVocabRepo : VocabularyRepository {
        private val flow = MutableStateFlow<List<VocabularyWord>>(emptyList())
        override suspend fun ensureSeeded() = Unit
        override fun observeAllWords(): Flow<List<VocabularyWord>> = flow
        override suspend fun getAllWords(): List<VocabularyWord> = emptyList()
        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> = emptyList()
        override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> = emptyList()
        override suspend fun updateWord(word: VocabularyWord) = Unit
        override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) = Unit
    }

    private class StubTimeProvider : TimeProvider {
        override fun nowMillis(): Long = 1_700_000_000_000L
        override fun endOfTodayMillis(): Long = 1_700_000_000_000L
        override fun todayEpochDay(): Long = 100L
        override fun epochDayForMillis(timestampMillis: Long): Long = 100L
    }

    @Test
    fun `tts language resolver selects US when available and falls back to UK when US missing`() {
        val usReady = TtsManager.resolveEnglishLanguage(
            checkAvailability = { locale ->
                if (locale == Locale.US) TextToSpeech.LANG_COUNTRY_AVAILABLE
                else TextToSpeech.LANG_NOT_SUPPORTED
            },
            applyLanguage = { TextToSpeech.LANG_COUNTRY_AVAILABLE }
        )
        assertEquals(TtsLanguageStatus.READY, usReady.status)
        assertEquals(Locale.US, usReady.selectedLocale)

        val ukFallback = TtsManager.resolveEnglishLanguage(
            checkAvailability = { locale ->
                when (locale) {
                    Locale.US -> TextToSpeech.LANG_MISSING_DATA
                    Locale.UK -> TextToSpeech.LANG_COUNTRY_AVAILABLE
                    else -> TextToSpeech.LANG_NOT_SUPPORTED
                }
            },
            applyLanguage = { locale ->
                if (locale == Locale.UK) TextToSpeech.LANG_COUNTRY_AVAILABLE else TextToSpeech.LANG_MISSING_DATA
            }
        )
        assertEquals(TtsLanguageStatus.READY, ukFallback.status)
        assertEquals(Locale.UK, ukFallback.selectedLocale)
    }

    @Test
    fun `tts language resolver reports MISSING_VOICE_DATA when all English packs lack data and survives exceptions`() {
        val missingData = TtsManager.resolveEnglishLanguage(
            checkAvailability = { TextToSpeech.LANG_MISSING_DATA },
            applyLanguage = { TextToSpeech.LANG_MISSING_DATA }
        )
        assertEquals(TtsLanguageStatus.MISSING_VOICE_DATA, missingData.status)
        assertNull(missingData.selectedLocale)

        val engineError = TtsManager.resolveEnglishLanguage(
            checkAvailability = { throw IllegalStateException("OEM TTS binder died") },
            applyLanguage = { throw IllegalStateException("OEM TTS binder died") }
        )
        assertEquals(TtsLanguageStatus.UNSUPPORTED, engineError.status)
        assertNull(engineError.selectedLocale)
    }

    @Test
    fun `computeInitialDelayMillis handles same-day future, exact current minute, and past time`() {
        val tz = TimeZone.getTimeZone("UTC")
        val nowCal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 22, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = nowCal.timeInMillis

        // 15:00 today -> 30 minutes = 1,800,000 ms
        val futureDelay = WorkManagerReminderScheduler.computeInitialDelayMillis(15, 0, nowMillis, tz)
        assertEquals(30L * 60L * 1000L, futureDelay)

        // Exact 14:30 -> shifts to 14:30 tomorrow = 24 hours
        val exactDelay = WorkManagerReminderScheduler.computeInitialDelayMillis(14, 30, nowMillis, tz)
        assertEquals(24L * 60L * 60L * 1000L, exactDelay)

        // Past 14:00 -> shifts to 14:00 tomorrow = 23.5 hours
        val pastDelay = WorkManagerReminderScheduler.computeInitialDelayMillis(14, 0, nowMillis, tz)
        assertEquals((23L * 60L + 30L) * 60L * 1000L, pastDelay)
    }

    @Test
    fun `toggling reminder and changing time updates single schedule without duplicates and cancels cleanly`() = runTest {
        val prefsRepo = FakePrefsRepo()
        val scheduler = RecordingReminderScheduler()
        val vocabRepo = StubVocabRepo()
        val timeProvider = StubTimeProvider()
        val streakCalc = StreakCalculator()
        val vm = HomeViewModel(
            vocabularyRepository = vocabRepo,
            observeHomeSummaryUseCase = ObserveHomeSummaryUseCase(vocabRepo, timeProvider),
            addCustomVocabularyUseCase = AddCustomVocabularyUseCase(vocabRepo, timeProvider),
            userPreferencesRepository = prefsRepo,
            timeProvider = timeProvider,
            streakCalculator = streakCalc,
            reminderScheduler = scheduler,
            defaultDispatcher = testDispatcher
        )

        vm.updateReminderSettings(enabled = true, hour = 20, minute = 0)
        advanceUntilIdle()
        assertEquals(20 to 0, scheduler.activeSchedule)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)

        // Changing reminder hour updates active schedule instead of adding a second one
        vm.updateReminderSettings(enabled = true, hour = 21, minute = 30)
        advanceUntilIdle()
        assertEquals(21 to 30, scheduler.activeSchedule)
        assertEquals(2, scheduler.scheduleCalls)

        // Disabling cancels cleanly
        vm.updateReminderSettings(enabled = false, hour = 21, minute = 30)
        advanceUntilIdle()
        assertNull(scheduler.activeSchedule)
        assertEquals(1, scheduler.cancelCalls)
    }

    @Test
    fun `ReminderBootRestorer restores enabled schedule and cancels disabled schedule`() = runTest {
        val enabledPrefs = FakePrefsRepo(
            UserPreferences(reminderEnabled = true, reminderHour = 19, reminderMinute = 45)
        )
        val enabledScheduler = RecordingReminderScheduler()
        val restored = ReminderBootRestorer.restoreIfEnabled(enabledPrefs, enabledScheduler)
        assertTrue(restored)
        assertEquals(19 to 45, enabledScheduler.activeSchedule)
        assertEquals(1, enabledScheduler.scheduleCalls)
        assertEquals(0, enabledScheduler.cancelCalls)

        val disabledPrefs = FakePrefsRepo(
            UserPreferences(reminderEnabled = false, reminderHour = 19, reminderMinute = 45)
        )
        val disabledScheduler = RecordingReminderScheduler()
        val notRestored = ReminderBootRestorer.restoreIfEnabled(disabledPrefs, disabledScheduler)
        assertFalse(notRestored)
        assertNull(disabledScheduler.activeSchedule)
        assertEquals(0, disabledScheduler.scheduleCalls)
        assertEquals(1, disabledScheduler.cancelCalls)
    }
}
