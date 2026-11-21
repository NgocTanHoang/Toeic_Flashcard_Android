package com.toeicflashcard

import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.core.ui.glass.AdaptiveCardMinWidth
import com.toeicflashcard.core.ui.glass.AdaptiveWideBreakpoint
import com.toeicflashcard.core.ui.glass.AppWindowMetrics
import com.toeicflashcard.core.ui.glass.MainContentMaxWidth
import com.toeicflashcard.core.ui.glass.WideQuizContentMaxWidth
import com.toeicflashcard.core.ui.glass.WideStudyContentMaxWidth
import com.toeicflashcard.core.ui.glass.adaptiveHorizontalPadding
import com.toeicflashcard.core.ui.glass.cardInnerPadding
import com.toeicflashcard.core.ui.glass.gridItemSpacing
import com.toeicflashcard.core.ui.glass.screenHorizontalPadding
import com.toeicflashcard.core.ui.glass.sectionVerticalSpacing
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
import com.toeicflashcard.domain.usecase.AddCustomVocabularyUseCase
import com.toeicflashcard.domain.usecase.AddWordResult
import com.toeicflashcard.domain.usecase.CustomWordInput
import com.toeicflashcard.domain.usecase.GenerateQuizUseCase
import com.toeicflashcard.domain.usecase.GetStudyQueueUseCase
import com.toeicflashcard.domain.usecase.MarkWordsUrgentReviewUseCase
import com.toeicflashcard.domain.usecase.ObserveHomeSummaryUseCase
import com.toeicflashcard.domain.usecase.SubmitReviewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

class SystemFlowAndAdaptiveUiTest {

    private class MutableTimeProvider(
        var currentMillis: Long,
        var currentEpochDay: Long
    ) : TimeProvider {
        override fun nowMillis(): Long = currentMillis
        override fun endOfTodayMillis(): Long = currentMillis
        override fun todayEpochDay(): Long = currentEpochDay
        override fun epochDayForMillis(timestampMillis: Long): Long =
            currentEpochDay + ((timestampMillis - currentMillis) / DAY_MILLIS)
    }

    private class InMemoryVocabularyRepository(
        initialWords: List<VocabularyWord>
    ) : VocabularyRepository {
        private val state = MutableStateFlow(initialWords)

        override suspend fun ensureSeeded() = Unit
        override fun getCachedAllWords(): List<VocabularyWord> = state.value
        override fun observeAllWords(): Flow<List<VocabularyWord>> = state
        override suspend fun getAllWords(): List<VocabularyWord> = state.value
        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> =
            state.value.filter { it.isDue(cutoffMillis) }
        override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> =
            state.value.filter { it.topic == topic }

        override suspend fun insertCustomWord(word: VocabularyWord): Boolean {
            if (state.value.any { it.word.equals(word.word, ignoreCase = true) }) return false
            state.value = state.value + word
            return true
        }

        override suspend fun insertCustomWords(words: List<VocabularyWord>): Int {
            val existing = state.value.map { it.word.lowercase() }.toHashSet()
            val toAdd = words.filter { existing.add(it.word.lowercase()) }
            state.value = state.value + toAdd
            return toAdd.size
        }

        override suspend fun updateWord(word: VocabularyWord) {
            state.value = state.value.map { if (it.id == word.id) word else it }
        }

        override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) {
            val idSet = wordIds.toSet()
            state.value = state.value.map { word ->
                if (word.id in idSet) {
                    word.copy(nextReviewTimestamp = nowMillis)
                } else {
                    word
                }
            }
        }
    }

    private class InMemoryUserPreferencesRepository : UserPreferencesRepository {
        private val state = MutableStateFlow(UserPreferences())
        override val userPreferences: Flow<UserPreferences> = state
        override fun getCachedSpeechRate(): Float = state.value.speechRate
        override fun getCachedSelectedDeckId(): String = state.value.selectedDeckId

        override suspend fun setThemeMode(mode: ThemeMode) {
            state.value = state.value.copy(themeMode = mode)
        }

        override suspend fun setDarkTone(tone: DarkTone, forceDarkMode: Boolean) {
            state.value = state.value.copy(
                darkTone = tone,
                themeMode = if (forceDarkMode) ThemeMode.DARK else state.value.themeMode
            )
        }

        override suspend fun setSpeechRate(rate: Float) {
            state.value = state.value.copy(speechRate = rate)
        }

        override suspend fun setDailyGoal(goal: Int) {
            state.value = state.value.copy(dailyGoal = goal)
        }

        override suspend fun setSelectedDeckId(deckId: String) {
            state.value = state.value.copy(selectedDeckId = deckId)
        }

        override suspend fun recordStudyReview(todayEpochDay: Long, calculator: StreakCalculator) {
            val current = state.value
            val next = calculator.onCardReviewed(current.toStreakState(), todayEpochDay)
            val updatedHistory = (current.dailyReviewHistory + (todayEpochDay to next.dailyReviewedCount))
                .filterKeys { it >= todayEpochDay - 13L }
            state.value = current.copy(
                currentStreak = next.currentStreak,
                lastStudyDateEpochDay = next.lastStudyDateEpochDay,
                dailyReviewedCount = next.dailyReviewedCount,
                dailyReviewHistory = updatedHistory
            )
        }

        override suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
            state.value = state.value.copy(
                reminderEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute
            )
        }

        override fun previewGlassOpacity(opacity: Float?) = Unit

        override suspend fun setGlassOpacity(opacity: Float) {
            state.value = state.value.copy(glassOpacity = opacity)
        }

        override suspend fun resetGlassOpacity() {
            state.value = state.value.copy(glassOpacity = null)
        }
    }

    private fun sampleWord(
        id: Int,
        topic: String,
        deckId: String = VocabularyDecks.ESSENTIAL_600_ID
    ) = VocabularyWord(
        id = id,
        topic = topic,
        word = "term_$id",
        phonetic = "/tɜːrm$id/",
        partOfSpeech = "n.",
        meaningVi = "nghĩa số $id",
        exampleEn = "Sample sentence for term $id.",
        exampleVi = "Câu ví dụ cho từ $id.",
        deckId = deckId,
        deckName = VocabularyDecks.displayNameFor(deckId)
    )

    @Test
    fun `end to end SM-2 study flow updates HomeSummary, continueTopic, next-day due count and streak`() = runTest {
        val day0Millis = 1_700_000_000_000L
        val timeProvider = MutableTimeProvider(currentMillis = day0Millis, currentEpochDay = 100L)
        val initialWords = listOf(
            sampleWord(1001, "L01 • Contracts"),
            sampleWord(1002, "L01 • Contracts"),
            sampleWord(1003, "L02 • Marketing"),
            sampleWord(1004, "L02 • Marketing")
        )
        val vocabRepo = InMemoryVocabularyRepository(initialWords)
        val prefsRepo = InMemoryUserPreferencesRepository()
        val scheduler = Sm2Scheduler()
        val streakCalculator = StreakCalculator()

        val observeHome = ObserveHomeSummaryUseCase(vocabRepo, timeProvider, Dispatchers.Unconfined)
        val getStudyQueue = GetStudyQueueUseCase(vocabRepo, timeProvider)
        val submitReview = SubmitReviewUseCase(vocabRepo, prefsRepo, scheduler, streakCalculator, timeProvider)

        // Initial state: 0 review due, 4 brand new words, no continueTopic
        val initialSummary = observeHome(prefsRepo.userPreferences.map { it.selectedDeckId }).first()
        assertEquals(0, initialSummary.reviewDueCount)
        assertEquals(4, initialSummary.newWordsCount)
        assertEquals(0, initialSummary.learnedCount)
        assertNull(initialSummary.continueTopic)

        // Study 1 word in L01 • Contracts with GOOD (q=4 -> 1 day interval)
        val contractsQueue = getStudyQueue(topic = "L01 • Contracts", hardWordsOnly = false, deckId = null)
        assertEquals(2, contractsQueue.words.size)
        submitReview(contractsQueue.words[0], ReviewQuality.GOOD)

        // Study 1 word in L02 • Marketing later with AGAIN (q=1 -> repetitions=0, not learned yet)
        timeProvider.currentMillis = day0Millis + 5_000L
        val marketingQueue = getStudyQueue(topic = "L02 • Marketing", hardWordsOnly = false, deckId = null)
        submitReview(marketingQueue.words[0], ReviewQuality.AGAIN)

        val midSummary = observeHome(prefsRepo.userPreferences.map { it.selectedDeckId }).first()
        assertEquals(0, midSummary.reviewDueCount)
        assertEquals(3, midSummary.newWordsCount)
        assertEquals(1, midSummary.learnedCount)
        // Only L01 • Contracts has learnedCount > 0 && < totalCount
        assertEquals("L01 • Contracts", midSummary.continueTopic?.topic)
        assertEquals(1, prefsRepo.userPreferences.first().currentStreak)
        assertEquals(2, prefsRepo.userPreferences.first().dailyReviewedCount)

        // Advance to Day 1 (+24h): the GOOD word (interval=1d) becomes reviewDue
        timeProvider.currentMillis = day0Millis + DAY_MILLIS + 10_000L
        timeProvider.currentEpochDay = 101L

        val day1Summary = observeHome(prefsRepo.userPreferences.map { it.selectedDeckId }).first()
        assertEquals(1, day1Summary.reviewDueCount)
        assertEquals(3, day1Summary.newWordsCount)

        // Review the due word on Day 1 -> streak increments to 2
        val day1DueQueue = getStudyQueue(topic = null, hardWordsOnly = false, deckId = VocabularyDecks.ESSENTIAL_600_ID)
        assertTrue(day1DueQueue.words.isNotEmpty())
        submitReview(day1DueQueue.words.first(), ReviewQuality.EASY)
        assertEquals(2, prefsRepo.userPreferences.first().currentStreak)
        assertEquals(1, prefsRepo.userPreferences.first().dailyReviewedCount)
    }

    @Test
    fun `quiz generation and markWordsUrgentReview push wrong words directly to due review queue`() = runTest {
        val nowMillis = 1_700_000_000_000L
        val futureMillis = nowMillis + 10 * DAY_MILLIS
        val timeProvider = MutableTimeProvider(currentMillis = nowMillis, currentEpochDay = 100L)

        // 12 already-learned words scheduled in the future
        val words = (1001..1012).map { id ->
            sampleWord(id, "L01 • Contracts", VocabularyDecks.ESSENTIAL_600_ID).copy(
                repetitions = 3,
                intervalDays = 10,
                nextReviewTimestamp = futureMillis,
                lastReviewedTimestamp = nowMillis - DAY_MILLIS
            )
        }
        val vocabRepo = InMemoryVocabularyRepository(words)
        val observeHome = ObserveHomeSummaryUseCase(vocabRepo, timeProvider, Dispatchers.Unconfined)
        val generateQuiz = GenerateQuizUseCase(vocabRepo)
        val markUrgent = MarkWordsUrgentReviewUseCase(vocabRepo, timeProvider)
        val getStudyQueue = GetStudyQueueUseCase(vocabRepo, timeProvider)

        assertEquals(0, observeHome(MutableStateFlow(VocabularyDecks.ALL_ID)).first().reviewDueCount)

        val questions = generateQuiz(topic = null, maxQuestions = 10, deckId = VocabularyDecks.ESSENTIAL_600_ID)
        assertEquals(10, questions.size)
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(1, q.options.count { it.isCorrect })
            assertEquals(4, q.options.map { it.textVi }.distinct().size)
        }

        // User gets 2 questions wrong and taps "Đưa các từ sai vào diện ôn tập gấp"
        val wrongIds = listOf(questions[0].word.id, questions[1].word.id)
        markUrgent(wrongIds)

        val summaryAfter = observeHome(MutableStateFlow(VocabularyDecks.ALL_ID)).first()
        assertEquals(2, summaryAfter.reviewDueCount)

        val urgentQueue = getStudyQueue(topic = null, hardWordsOnly = false, deckId = VocabularyDecks.ESSENTIAL_600_ID)
        assertFalse(urgentQueue.isPracticeMode)
        assertEquals(wrongIds.toSet(), urgentQueue.words.map { it.id }.toSet())
    }

    @Test
    fun `custom vocabulary insertion updates HomeSummary counts and deck list dynamically`() = runTest {
        val nowMillis = 1_700_000_000_000L
        val timeProvider = MutableTimeProvider(currentMillis = nowMillis, currentEpochDay = 100L)
        val vocabRepo = InMemoryVocabularyRepository(
            listOf(sampleWord(1, "Marketing", VocabularyDecks.CORE_775_ID))
        )
        val observeHome = ObserveHomeSummaryUseCase(vocabRepo, timeProvider, Dispatchers.Unconfined)
        val addCustom = AddCustomVocabularyUseCase(vocabRepo, timeProvider)

        val result = addCustom.addSingleWord(
            CustomWordInput(
                word = "benchmark",
                phonetic = "ˈbentʃmɑːrk",
                partOfSpeech = "n.",
                meaningVi = "chuẩn đối chiếu",
                topic = "Chiến lược",
                exampleEn = "Set a benchmark.",
                exampleVi = "Đặt chuẩn đối chiếu."
            )
        )
        assertTrue(result is AddWordResult.Success)

        val allSummary = observeHome(MutableStateFlow(VocabularyDecks.ALL_ID)).first()
        assertEquals(2, allSummary.totalCount)
        assertEquals(1, allSummary.decks.first { it.deckId == VocabularyDecks.CUSTOM_USER_ID }.totalCount)

        val customSummary = observeHome(MutableStateFlow(VocabularyDecks.CUSTOM_USER_ID)).first()
        assertEquals(1, customSummary.totalCount)
        assertEquals(1, customSummary.newWordsCount)
        assertEquals("Chiến lược", customSummary.topics.single().topic)
    }

    @Test
    fun `adaptive UI layout matrix verifies all 11 AVD device profiles Q1 to Q11`() {
        data class DeviceProfile(
            val code: String,
            val pxWidth: Int,
            val pxHeight: Int,
            val densityDpi: Int,
            val expectedExpanded: Boolean,
            val expectedLandscapeShort: Boolean,
            val expectedScreenPadDp: Float,
            val expectedSectionGapDp: Float,
            val expectedGridGapDp: Float,
            val expectedCardInnerPadDp: Float,
            val expectedGridColumns: Int
        )

        val profiles = listOf(
            DeviceProfile("Q1_Phone_Portrait", 1080, 2436, 420, false, false, 16f, 14f, 10f, 16f, 1),
            DeviceProfile("Q2_Phone_Landscape", 2436, 1080, 420, true, true, 28f, 22f, 16f, 22f, 2),
            DeviceProfile("Q3_ZFold7_Cover", 968, 2376, 420, false, false, 16f, 14f, 10f, 16f, 1),
            DeviceProfile("Q4_ZFold7_Inner", 1968, 2184, 374, true, false, 28f, 22f, 16f, 22f, 2),
            DeviceProfile("Q5_OPPOPadSE_7_5", 1600, 2240, 280, true, false, 28f, 22f, 16f, 22f, 2),
            DeviceProfile("Q6_RedmiPad2_Portrait", 1600, 2560, 274, true, false, 28f, 22f, 16f, 22f, 2),
            DeviceProfile("Q7_RedmiPad2_Landscape", 2560, 1600, 274, true, false, 40f, 22f, 16f, 22f, 3),
            DeviceProfile("Q8_XiaomiPad8_Portrait", 2136, 3200, 345, true, false, 28f, 22f, 16f, 22f, 2),
            DeviceProfile("Q9_SplitScreen", 1080, 1218, 420, false, false, 16f, 14f, 10f, 16f, 1),
            DeviceProfile("Q10_ZFold8_Cover", 1248, 1972, 420, false, false, 16f, 14f, 10f, 16f, 1),
            DeviceProfile("Q11_ZFold8_Inner", 1848, 2448, 370, true, false, 28f, 22f, 16f, 22f, 2)
        )

        profiles.forEach { p ->
            val widthDp = (p.pxWidth.toFloat() / (p.densityDpi.toFloat() / 160f)).dp
            val heightDp = (p.pxHeight.toFloat() / (p.densityDpi.toFloat() / 160f)).dp
            val metrics = AppWindowMetrics(widthDp = widthDp, heightDp = heightDp)

            assertEquals("${p.code} isExpandedWidth", p.expectedExpanded, metrics.isExpandedWidth)
            assertEquals("${p.code} isLandscapeShort", p.expectedLandscapeShort, metrics.isLandscapeShort)
            assertEquals("${p.code} screenHorizontalPadding", p.expectedScreenPadDp, screenHorizontalPadding(widthDp).value, 0.01f)
            assertEquals("${p.code} sectionVerticalSpacing", p.expectedSectionGapDp, sectionVerticalSpacing(widthDp).value, 0.01f)
            assertEquals("${p.code} gridItemSpacing", p.expectedGridGapDp, gridItemSpacing(widthDp).value, 0.01f)
            assertEquals("${p.code} cardInnerPadding", p.expectedCardInnerPadDp, cardInnerPadding(widthDp).value, 0.01f)

            val pad = adaptiveHorizontalPadding(widthDp, MainContentMaxWidth)
            val contentWidth = widthDp.value - pad.value * 2f
            val gap = gridItemSpacing(widthDp).value
            val columns = if (widthDp < AdaptiveWideBreakpoint) {
                1
            } else {
                floor((contentWidth + gap) / (AdaptiveCardMinWidth.value + gap)).toInt().coerceAtLeast(1)
            }
            assertEquals("${p.code} grid columns", p.expectedGridColumns, columns)
        }

        // Verify wide Study & Quiz content max widths exceed compact widths
        assertTrue(WideStudyContentMaxWidth > 560.dp)
        assertTrue(WideQuizContentMaxWidth > 580.dp)
        assertNotNull(MainContentMaxWidth)
    }

    companion object {
        private const val DAY_MILLIS = 86_400_000L
    }
}
