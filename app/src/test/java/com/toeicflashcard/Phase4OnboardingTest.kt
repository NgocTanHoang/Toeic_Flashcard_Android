package com.toeicflashcard

import com.toeicflashcard.core.notification.ReminderScheduler
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.ToeicTargetLevel
import com.toeicflashcard.domain.model.UserPreferences
import com.toeicflashcard.domain.model.VocabularyDecks
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Phase4OnboardingTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePrefsRepo(
        initial: UserPreferences = UserPreferences()
    ) : UserPreferencesRepository {
        val flow = MutableStateFlow(initial)
        override val userPreferences: Flow<UserPreferences> = flow
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setDarkTone(tone: DarkTone, forceDarkMode: Boolean) = Unit
        override suspend fun setSpeechRate(rate: Float) = Unit
        override suspend fun setAutoPlayTts(enabled: Boolean) = Unit
        override suspend fun setEnableHapticFeedback(enabled: Boolean) = Unit
        override suspend fun setDailyGoal(goal: Int) {
            flow.value = flow.value.copy(dailyGoal = goal)
        }
        override suspend fun setSelectedDeckId(deckId: String) {
            flow.value = flow.value.copy(selectedDeckId = deckId)
        }
        override suspend fun recordStudyReview(todayEpochDay: Long, calculator: StreakCalculator) = Unit
        override suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) = Unit
        override fun previewGlassOpacity(opacity: Float?) = Unit
        override suspend fun setGlassOpacity(opacity: Float) = Unit
        override suspend fun resetGlassOpacity() = Unit
        override suspend fun completeOnboarding(dailyGoal: Int?, targetToeicLevel: String?) {
            flow.value = flow.value.copy(
                hasCompletedOnboarding = true,
                dailyGoal = dailyGoal ?: flow.value.dailyGoal,
                targetToeicLevel = targetToeicLevel?.let { ToeicTargetLevel.fromId(it).id }
                    ?: flow.value.targetToeicLevel
            )
        }
    }

    private class StubVocabRepo(initialWords: List<VocabularyWord> = emptyList()) : VocabularyRepository {
        private val flow = MutableStateFlow(initialWords)
        override suspend fun ensureSeeded() = Unit
        override fun observeAllWords(): Flow<List<VocabularyWord>> = flow
        override suspend fun getAllWords(): List<VocabularyWord> = flow.value
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

    private class NoOpScheduler : ReminderScheduler {
        override fun scheduleDailyReminder(hour: Int, minute: Int) = Unit
        override fun cancelDailyReminder() = Unit
    }

    @Test
    fun `UserPreferences defaults have onboarding incomplete and essential_600 selected`() {
        val prefs = UserPreferences()
        assertFalse(prefs.hasCompletedOnboarding)
        assertEquals(VocabularyDecks.ESSENTIAL_600_ID, prefs.selectedDeckId)
        assertEquals(ToeicTargetLevel.TARGET_650.id, prefs.targetToeicLevel)
    }

    @Test
    fun `completeOnboarding saves goal and target without locking selectedDeckId`() = runTest {
        val prefsRepo = FakePrefsRepo()
        val vocabRepo = StubVocabRepo()
        val timeProvider = StubTimeProvider()
        val vm = HomeViewModel(
            vocabularyRepository = vocabRepo,
            observeHomeSummaryUseCase = ObserveHomeSummaryUseCase(vocabRepo, timeProvider, testDispatcher),
            addCustomVocabularyUseCase = AddCustomVocabularyUseCase(vocabRepo, timeProvider),
            userPreferencesRepository = prefsRepo,
            timeProvider = timeProvider,
            streakCalculator = StreakCalculator(),
            reminderScheduler = NoOpScheduler(),
            defaultDispatcher = testDispatcher
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.preferencesLoaded)
        assertFalse(vm.uiState.value.hasCompletedOnboarding)

        vm.completeOnboarding(dailyGoal = 20, targetToeicLevel = "800+")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasCompletedOnboarding)
        assertEquals(20, vm.uiState.value.dailyGoal)
        assertEquals("800+", vm.uiState.value.targetToeicLevel)
        assertEquals(VocabularyDecks.ESSENTIAL_600_ID, vm.uiState.value.selectedDeckId)

        // User remains free to switch decks at any time
        vm.selectDeck(VocabularyDecks.LEVEL_450_800_ID)
        advanceUntilIdle()
        assertEquals(VocabularyDecks.LEVEL_450_800_ID, vm.uiState.value.selectedDeckId)

        job.cancel()
        advanceUntilIdle()
    }

    @Test
    fun `skipOnboarding marks completed without overwriting existing dailyGoal or restored state`() = runTest {
        val prefsRepo = FakePrefsRepo(
            UserPreferences(
                dailyGoal = 15,
                hasCompletedOnboarding = false
            )
        )
        val vocabRepo = StubVocabRepo()
        val timeProvider = StubTimeProvider()
        val vm = HomeViewModel(
            vocabularyRepository = vocabRepo,
            observeHomeSummaryUseCase = ObserveHomeSummaryUseCase(vocabRepo, timeProvider, testDispatcher),
            addCustomVocabularyUseCase = AddCustomVocabularyUseCase(vocabRepo, timeProvider),
            userPreferencesRepository = prefsRepo,
            timeProvider = timeProvider,
            streakCalculator = StreakCalculator(),
            reminderScheduler = NoOpScheduler(),
            defaultDispatcher = testDispatcher
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.skipOnboarding()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasCompletedOnboarding)
        assertEquals(15, vm.uiState.value.dailyGoal)
        assertEquals(VocabularyDecks.ESSENTIAL_600_ID, vm.uiState.value.selectedDeckId)

        job.cancel()
        advanceUntilIdle()
    }

    @Test
    fun `HomeScreen dashboard deck titles, descriptions, and 3 daily overview metrics are consistent`() = runTest {
        VocabularyDecks.orderedCurriculumIds.forEach { deckId ->
            assertTrue(VocabularyDecks.dashboardTitleFor(deckId).isNotBlank())
            assertTrue(VocabularyDecks.descriptionFor(deckId).isNotBlank())
        }
        assertEquals("600 Từ TOEIC Cốt Lõi", VocabularyDecks.dashboardTitleFor(VocabularyDecks.ESSENTIAL_600_ID))
        assertEquals("775 Từ TOEIC Mở Rộng", VocabularyDecks.dashboardTitleFor(VocabularyDecks.CORE_775_ID))
        assertEquals("800 Từ TOEIC Nâng Cao", VocabularyDecks.dashboardTitleFor(VocabularyDecks.LEVEL_450_800_ID))
        assertEquals("Từ Vựng Của Tôi", VocabularyDecks.dashboardTitleFor(VocabularyDecks.CUSTOM_USER_ID))

        val prefsRepo = FakePrefsRepo(
            UserPreferences(dailyGoal = 15, dailyReviewedCount = 6, lastStudyDateEpochDay = 100L)
        )
        val dueWord = VocabularyWord(
            id = 1,
            topic = "Contracts",
            word = "abide",
            phonetic = "/əˈbaɪd/",
            partOfSpeech = "v.",
            meaningVi = "tuân thủ",
            exampleEn = "Abide by the rules.",
            exampleVi = "Tuân thủ quy định.",
            deckId = VocabularyDecks.ESSENTIAL_600_ID,
            deckName = VocabularyDecks.ESSENTIAL_600_NAME
        )
        val vocabRepo = StubVocabRepo(listOf(dueWord))
        val timeProvider = StubTimeProvider()
        val vm = HomeViewModel(
            vocabularyRepository = vocabRepo,
            observeHomeSummaryUseCase = ObserveHomeSummaryUseCase(vocabRepo, timeProvider, testDispatcher),
            addCustomVocabularyUseCase = AddCustomVocabularyUseCase(vocabRepo, timeProvider),
            userPreferencesRepository = prefsRepo,
            timeProvider = timeProvider,
            streakCalculator = StreakCalculator(),
            reminderScheduler = NoOpScheduler(),
            defaultDispatcher = testDispatcher
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.reviewDueCount)
        assertEquals(1, vm.uiState.value.newWordsCount)
        assertEquals(6, vm.uiState.value.dailyReviewedCount)
        assertEquals(15, vm.uiState.value.dailyGoal)
        val discoverDeckIds = VocabularyDecks.orderedCurriculumIds.filter {
            it != vm.uiState.value.selectedDeckId
        }
        assertEquals(3, discoverDeckIds.size)
        assertFalse(VocabularyDecks.ESSENTIAL_600_ID in discoverDeckIds)

        job.cancel()
        advanceUntilIdle()
    }
}
