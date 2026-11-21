package com.toeicflashcard.presentation.vault

import com.toeicflashcard.core.audio.TtsFeedback
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.UserPreferences
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.streak.StreakCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyVaultViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeVaultRepository(
        private val words: List<VocabularyWord>
    ) : VocabularyRepository {
        override suspend fun ensureSeeded() = Unit
        override fun observeAllWords(): Flow<List<VocabularyWord>> = flowOf(words)
        override suspend fun getAllWords(): List<VocabularyWord> = words
        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> = words
        override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> = words
        override suspend fun updateWord(word: VocabularyWord) = Unit
        override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) = Unit
    }

    private class FakePrefsRepository(
        initialDeckId: String = VocabularyDecks.ALL_ID
    ) : UserPreferencesRepository {
        private val state = MutableStateFlow(UserPreferences(selectedDeckId = initialDeckId))
        override val userPreferences: Flow<UserPreferences> = state
        override suspend fun setDailyGoal(goal: Int) = Unit
        override suspend fun setSelectedDeckId(deckId: String) {
            state.value = state.value.copy(selectedDeckId = deckId)
        }
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setDarkTone(tone: DarkTone, forceDarkMode: Boolean) = Unit
        override suspend fun setSpeechRate(rate: Float) = Unit
        override suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) = Unit
        override suspend fun recordStudyReview(todayEpochDay: Long, calculator: StreakCalculator) = Unit
        override fun previewGlassOpacity(opacity: Float?) = Unit
        override suspend fun setGlassOpacity(opacity: Float) = Unit
        override suspend fun resetGlassOpacity() = Unit
    }

    private fun word(
        id: Int,
        word: String,
        meaningVi: String,
        topic: String,
        deckId: String,
        repetitions: Int = 0,
        easeFactor: Float = 2.5f,
        lastReviewedTimestamp: Long = 0L
    ) = VocabularyWord(
        id = id,
        topic = topic,
        word = word,
        phonetic = "/$word/",
        partOfSpeech = "n.",
        meaningVi = meaningVi,
        exampleEn = "Example $word",
        exampleVi = "Ví dụ $word",
        repetitions = repetitions,
        easeFactor = easeFactor,
        lastReviewedTimestamp = lastReviewedTimestamp,
        deckId = deckId,
        deckName = VocabularyDecks.displayNameFor(deckId)
    )

    @Test
    fun `filters by deck, VaultFilter, and multi-field search query accurately`() = runTest {
        val sample = listOf(
            word(1, "invoice", "hóa đơn", "Finance & Banking", VocabularyDecks.CORE_775_ID, repetitions = 2, easeFactor = 2.6f, lastReviewedTimestamp = 100L),
            word(2, "audit", "kiểm toán", "Finance & Banking", VocabularyDecks.CORE_775_ID, repetitions = 0, easeFactor = 1.9f, lastReviewedTimestamp = 100L),
            word(3, "shipment", "lô hàng", "Travel & Logistics", VocabularyDecks.CORE_775_ID, repetitions = 0, easeFactor = 2.5f, lastReviewedTimestamp = 0L),
            word(1001, "contract", "hợp đồng", "L01 • Contracts", VocabularyDecks.ESSENTIAL_600_ID, repetitions = 1, easeFactor = 2.5f, lastReviewedTimestamp = 100L)
        )

        val prefsRepo = FakePrefsRepository(VocabularyDecks.CORE_775_ID)
        val viewModel = VocabularyVaultViewModel(
            vocabularyRepository = FakeVaultRepository(sample),
            userPreferencesRepository = prefsRepo,
            createTtsManager = { null },
            defaultDispatcher = testDispatcher
        )

        val collector = backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Scoped to CORE_775_ID (3 words)
        assertEquals(3, viewModel.uiState.value.totalCount)
        assertEquals(1, viewModel.uiState.value.counts[VaultFilter.FORGOTTEN])
        assertEquals(2, viewModel.uiState.value.counts[VaultFilter.LEARNING])
        assertEquals(1, viewModel.uiState.value.counts[VaultFilter.LEARNED])

        // Filter FORGOTTEN -> audit
        viewModel.selectFilter(VaultFilter.FORGOTTEN)
        advanceUntilIdle()
        assertEquals(listOf("audit"), viewModel.uiState.value.words.map { it.word })

        // Filter ALL + Vietnamese meaning search
        viewModel.selectFilter(VaultFilter.ALL)
        viewModel.updateSearchQuery("hóa đơn")
        advanceUntilIdle()
        assertEquals(listOf("invoice"), viewModel.uiState.value.words.map { it.word })

        // Empty result state
        viewModel.updateSearchQuery("khong_ton_tai_xyz")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.words.isEmpty())

        // Switch deck to ALL
        viewModel.updateSearchQuery("")
        prefsRepo.setSelectedDeckId(VocabularyDecks.ALL_ID)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.totalCount)

        collector.cancel()
    }
}
