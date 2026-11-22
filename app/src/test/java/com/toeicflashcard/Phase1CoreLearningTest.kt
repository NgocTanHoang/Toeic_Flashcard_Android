package com.toeicflashcard

import com.toeicflashcard.core.audio.TtsFeedback
import com.toeicflashcard.core.audio.TtsPlayer
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.UserPreferences
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.scheduler.Sm2Scheduler
import com.toeicflashcard.domain.streak.StreakCalculator
import com.toeicflashcard.domain.usecase.GetStudyQueueUseCase
import com.toeicflashcard.domain.usecase.SubmitReviewUseCase
import com.toeicflashcard.presentation.study.SessionSummaryStats
import com.toeicflashcard.presentation.study.StudyViewModel
import com.toeicflashcard.presentation.study.triggerStudyHapticIfEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class Phase1CoreLearningTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeTtsPlayer : TtsPlayer {
        val events = mutableListOf<String>()
        val spokenTexts = mutableListOf<String>()
        var stopCount = 0

        override fun warmUp() {
            events += "warmUp"
        }

        override fun speak(text: String, speechRate: Float): TtsFeedback {
            events += "speak:$text"
            spokenTexts += text
            return TtsFeedback.Spoken
        }

        override fun stop() {
            events += "stop"
            stopCount++
        }

        override fun shutdown() {
            events += "shutdown"
        }
    }

    private class MutableTimeProvider(
        var nowMillisValue: Long = 1_700_000_000_000L,
        var epochDayValue: Long = 100L
    ) : TimeProvider {
        override fun nowMillis(): Long = nowMillisValue
        override fun endOfTodayMillis(): Long = nowMillisValue
        override fun todayEpochDay(): Long = epochDayValue
        override fun epochDayForMillis(timestampMillis: Long): Long = epochDayValue
    }

    private class InMemoryVocabRepo(
        initialWords: List<VocabularyWord>
    ) : VocabularyRepository {
        private val state = MutableStateFlow(initialWords)
        override suspend fun ensureSeeded() = Unit
        override fun getCachedAllWords(): List<VocabularyWord> = state.value
        override fun observeAllWords(): Flow<List<VocabularyWord>> = state
        override suspend fun getAllWords(): List<VocabularyWord> = state.value
        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> =
            state.value.filter { it.isDue(cutoffMillis) }.sortedBy { it.id }
        override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> =
            state.value.filter { it.topic == topic }.sortedBy { it.id }
        override suspend fun updateWord(word: VocabularyWord) {
            val key = VocabularyDecks.normalizeWordKey(word.word)
            state.value = state.value.map {
                if (it.id == word.id || VocabularyDecks.normalizeWordKey(it.word) == key) {
                    it.copy(
                        repetitions = word.repetitions,
                        intervalDays = word.intervalDays,
                        easeFactor = word.easeFactor,
                        nextReviewTimestamp = word.nextReviewTimestamp,
                        lastReviewedTimestamp = word.lastReviewedTimestamp
                    )
                } else {
                    it
                }
            }
        }
        override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) = Unit
    }

    private class InMemoryPrefsRepo(
        initial: UserPreferences = UserPreferences()
    ) : UserPreferencesRepository {
        val state = MutableStateFlow(initial)
        override val userPreferences: Flow<UserPreferences> = state
        override fun getCachedSpeechRate(): Float = state.value.speechRate
        override fun getCachedSelectedDeckId(): String = state.value.selectedDeckId
        override fun getCachedAutoPlayTts(): Boolean = state.value.autoPlayTts
        override fun getCachedHapticEnabled(): Boolean = state.value.enableHapticFeedback

        override suspend fun setThemeMode(mode: ThemeMode) { state.value = state.value.copy(themeMode = mode) }
        override suspend fun setDarkTone(tone: DarkTone, forceDarkMode: Boolean) { state.value = state.value.copy(darkTone = tone) }
        override suspend fun setSpeechRate(rate: Float) { state.value = state.value.copy(speechRate = rate) }
        override suspend fun setAutoPlayTts(enabled: Boolean) { state.value = state.value.copy(autoPlayTts = enabled) }
        override suspend fun setEnableHapticFeedback(enabled: Boolean) { state.value = state.value.copy(enableHapticFeedback = enabled) }
        override suspend fun setDailyGoal(goal: Int) { state.value = state.value.copy(dailyGoal = goal) }
        override suspend fun setSelectedDeckId(deckId: String) { state.value = state.value.copy(selectedDeckId = deckId) }
        override suspend fun recordStudyReview(todayEpochDay: Long, calculator: StreakCalculator) {
            val current = state.value
            val updated = calculator.onCardReviewed(current.toStreakState(), todayEpochDay)
            state.value = current.copy(
                currentStreak = updated.currentStreak,
                lastStudyDateEpochDay = updated.lastStudyDateEpochDay,
                dailyReviewedCount = updated.dailyReviewedCount
            )
        }
        override suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) = Unit
        override fun previewGlassOpacity(opacity: Float?) = Unit
        override suspend fun setGlassOpacity(opacity: Float) = Unit
        override suspend fun resetGlassOpacity() = Unit
    }

    private fun word(
        id: Int,
        term: String = "term_$id",
        repetitions: Int = 0,
        nextReviewTimestamp: Long = 0L,
        deckId: String = VocabularyDecks.ESSENTIAL_600_ID
    ) = VocabularyWord(
        id = id,
        topic = "L01 • Contracts",
        word = term,
        phonetic = "/t$id/",
        partOfSpeech = "n.",
        meaningVi = "nghĩa $id",
        exampleEn = "Example $term.",
        exampleVi = "Ví dụ $term.",
        repetitions = repetitions,
        nextReviewTimestamp = nextReviewTimestamp,
        deckId = deckId,
        deckName = VocabularyDecks.displayNameFor(deckId)
    )

    // ==================== A. AUTO-PLAY TTS ====================

    @Test
    fun `auto-play TTS speaks once per card, skips back-to-front flip and recomposition, stops before next, and respects toggle`() = runTest {
        val words = listOf(
            word(1001, "abide"),
            word(1002, "agreement")
        )
        val vocabRepo = InMemoryVocabRepo(words)
        val prefsRepo = InMemoryPrefsRepo(UserPreferences(autoPlayTts = true))
        val timeProvider = MutableTimeProvider()
        val tts = FakeTtsPlayer()

        val vm = StudyViewModel(
            topic = null,
            deckId = VocabularyDecks.ESSENTIAL_600_ID,
            getStudyQueueUseCase = GetStudyQueueUseCase(vocabRepo, timeProvider, testDispatcher),
            submitReviewUseCase = SubmitReviewUseCase(vocabRepo, prefsRepo, Sm2Scheduler(), StreakCalculator(), timeProvider),
            sm2Scheduler = Sm2Scheduler(),
            streakCalculator = StreakCalculator(),
            timeProvider = timeProvider,
            userPreferencesRepository = prefsRepo,
            createTtsPlayer = { tts }
        )
        advanceUntilIdle()

        // 1. New front card: speaks once; simulated recomposition calls do not repeat
        vm.maybeAutoPlayCurrentCard()
        vm.maybeAutoPlayCurrentCard()
        assertEquals(listOf("abide"), tts.spokenTexts)

        // 2. Flip to back and back to front: must NOT auto-play again
        vm.flipCard()
        vm.maybeAutoPlayCurrentCard()
        Thread.sleep(220) // pass 200ms flip debounce
        vm.flipCard()
        assertFalse(vm.uiState.value.isFlipped)
        vm.maybeAutoPlayCurrentCard()
        assertEquals(listOf("abide"), tts.spokenTexts)

        // 3. Advancing to next card calls stop() before speaking new word
        vm.submitRating(ReviewQuality.GOOD)
        advanceUntilIdle()
        val stopsBeforeNext = tts.stopCount
        vm.maybeAutoPlayCurrentCard()
        assertTrue(tts.stopCount > stopsBeforeNext)
        assertEquals(listOf("abide", "agreement"), tts.spokenTexts)

        // 4. Turn Auto-play OFF: does not auto-play, manual speak still works
        vm.toggleAutoPlayTts()
        assertFalse(vm.uiState.value.autoPlayTts)
        assertFalse(prefsRepo.userPreferences.first().autoPlayTts)
        vm.speakWord()
        assertEquals(listOf("abide", "agreement", "agreement"), tts.spokenTexts)
    }

    // ==================== B. HAPTIC FEEDBACK ====================

    @Test
    fun `haptic feedback helper triggers only when enabled`() {
        var hapticCalls = 0
        triggerStudyHapticIfEnabled(enabled = false) { hapticCalls++ }
        assertEquals(0, hapticCalls)

        triggerStudyHapticIfEnabled(enabled = true) { hapticCalls++ }
        triggerStudyHapticIfEnabled(enabled = true) { hapticCalls++ }
        assertEquals(2, hapticCalls)
    }

    // ==================== C. SESSION SUMMARY & D. STREAK ====================

    @Test
    fun `session summary distinguishes new vs review words, recall rate, zero case, and streak increments at most once per day`() = runTest {
        // Zero-evaluated summary check
        assertEquals(0, SessionSummaryStats().recallRatePercent)

        val timeProvider = MutableTimeProvider(nowMillisValue = 1_700_000_000_000L, epochDayValue = 200L)
        val initialWords = listOf(
            word(1001, "alpha", repetitions = 0), // new
            word(1002, "beta", repetitions = 2, nextReviewTimestamp = 1_699_999_000_000L), // review due
            word(1003, "gamma", repetitions = 0) // new
        )
        val vocabRepo = InMemoryVocabRepo(initialWords)
        val prefsRepo = InMemoryPrefsRepo(UserPreferences(currentStreak = 0, lastStudyDateEpochDay = -1L))
        val streakCalc = StreakCalculator()

        val vm = StudyViewModel(
            topic = null,
            deckId = VocabularyDecks.ESSENTIAL_600_ID,
            getStudyQueueUseCase = GetStudyQueueUseCase(vocabRepo, timeProvider, testDispatcher),
            submitReviewUseCase = SubmitReviewUseCase(vocabRepo, prefsRepo, Sm2Scheduler(), streakCalc, timeProvider),
            sm2Scheduler = Sm2Scheduler(),
            streakCalculator = streakCalc,
            timeProvider = timeProvider,
            userPreferencesRepository = prefsRepo,
            createTtsPlayer = { FakeTtsPlayer() }
        )
        advanceUntilIdle()

        // Word 1 (alpha, new): AGAIN -> requeued, then GOOD later; must only count once in session stats!
        vm.submitRating(ReviewQuality.AGAIN)
        advanceUntilIdle()
        // Word 2 (beta, review): GOOD -> remembered immediately
        vm.submitRating(ReviewQuality.GOOD)
        advanceUntilIdle()
        // Word 3 (gamma, new): HARD -> NOT remembered immediately
        vm.submitRating(ReviewQuality.HARD)
        advanceUntilIdle()
        // Requeued alpha (new): EASY -> unique word count stays 3!
        vm.submitRating(ReviewQuality.EASY)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSessionComplete)
        val summary = vm.uiState.value.sessionSummary
        assertEquals(2, summary.newWordsStudied)
        assertEquals(1, summary.reviewedWordsCount)
        assertEquals(3, summary.totalEvaluatedCount)
        // alpha (first rating AGAIN -> not immediate), beta (GOOD -> yes), gamma (HARD -> no) => 1/3 = 33%
        assertEquals(1, summary.rememberedImmediatelyCount)
        assertEquals(33, summary.recallRatePercent)
        // First session on Day 200 -> Streak = 1
        assertEquals(1, summary.currentStreak)

        // Second session on SAME day (Day 200) -> Streak stays 1
        val nextBatchWords = listOf(word(1004, "delta", repetitions = 0))
        val vm2 = StudyViewModel(
            topic = null,
            deckId = VocabularyDecks.ESSENTIAL_600_ID,
            getStudyQueueUseCase = GetStudyQueueUseCase(InMemoryVocabRepo(nextBatchWords), timeProvider, testDispatcher),
            submitReviewUseCase = SubmitReviewUseCase(InMemoryVocabRepo(nextBatchWords), prefsRepo, Sm2Scheduler(), streakCalc, timeProvider),
            sm2Scheduler = Sm2Scheduler(),
            streakCalculator = streakCalc,
            timeProvider = timeProvider,
            userPreferencesRepository = prefsRepo,
            createTtsPlayer = { FakeTtsPlayer() }
        )
        advanceUntilIdle()
        vm2.submitRating(ReviewQuality.GOOD)
        advanceUntilIdle()
        assertEquals(1, vm2.uiState.value.sessionSummary.currentStreak)

        // Day 201 -> Streak advances to 2
        timeProvider.epochDayValue = 201L
        timeProvider.nowMillisValue += 86_400_000L
        val day2Words = listOf(word(1005, "epsilon", repetitions = 0))
        val vm3 = StudyViewModel(
            topic = null,
            deckId = VocabularyDecks.ESSENTIAL_600_ID,
            getStudyQueueUseCase = GetStudyQueueUseCase(InMemoryVocabRepo(day2Words), timeProvider, testDispatcher),
            submitReviewUseCase = SubmitReviewUseCase(InMemoryVocabRepo(day2Words), prefsRepo, Sm2Scheduler(), streakCalc, timeProvider),
            sm2Scheduler = Sm2Scheduler(),
            streakCalculator = streakCalc,
            timeProvider = timeProvider,
            userPreferencesRepository = prefsRepo,
            createTtsPlayer = { FakeTtsPlayer() }
        )
        advanceUntilIdle()
        vm3.submitRating(ReviewQuality.EASY)
        advanceUntilIdle()
        assertEquals(2, vm3.uiState.value.sessionSummary.currentStreak)
    }

    // ==================== E. CONTINUE 15 NEW WORDS & EMPTY DECK ====================

    @Test
    fun `continue 15 new words respects batch limit 15, partial batch, and empty deck state`() = runTest {
        val timeProvider = MutableTimeProvider()
        // 18 new words in ESSENTIAL_600_ID + 5 new words in CORE_775_ID (must not cross decks)
        val essWords = (1001..1018).map { word(it, "ess_$it", repetitions = 0, deckId = VocabularyDecks.ESSENTIAL_600_ID) }
        val coreWords = (1..5).map { word(it, "core_$it", repetitions = 0, deckId = VocabularyDecks.CORE_775_ID) }
        val vocabRepo = InMemoryVocabRepo(essWords + coreWords)
        val useCase = GetStudyQueueUseCase(vocabRepo, timeProvider, testDispatcher)

        // Batch 1: capped at 15 new words
        val batch1 = useCase.getNextNewWordsQueue(VocabularyDecks.ESSENTIAL_600_ID, limit = 15)
        assertEquals(15, batch1.words.size)
        assertTrue(batch1.words.all { it.deckId == VocabularyDecks.ESSENTIAL_600_ID })

        // Mark all 15 as learned
        batch1.words.forEach { vocabRepo.updateWord(it.copy(repetitions = 1, nextReviewTimestamp = 9_999_999_999_999L)) }

        // Batch 2 (< 15 remaining): takes exact remaining 3 words
        assertEquals(3, useCase.countRemainingNewWords(VocabularyDecks.ESSENTIAL_600_ID))
        val batch2 = useCase.getNextNewWordsQueue(VocabularyDecks.ESSENTIAL_600_ID, limit = 15)
        assertEquals(3, batch2.words.size)

        // Mark remaining 3 as learned
        batch2.words.forEach { vocabRepo.updateWord(it.copy(repetitions = 1, nextReviewTimestamp = 9_999_999_999_999L)) }

        // No more new words in ESSENTIAL_600_ID -> 0 remaining, empty queue, does NOT pull from CORE_775_ID
        assertEquals(0, useCase.countRemainingNewWords(VocabularyDecks.ESSENTIAL_600_ID))
        val emptyBatch = useCase.getNextNewWordsQueue(VocabularyDecks.ESSENTIAL_600_ID, limit = 15)
        assertTrue(emptyBatch.words.isEmpty())
        assertEquals(5, useCase.countRemainingNewWords(VocabularyDecks.CORE_775_ID))
    }
}
