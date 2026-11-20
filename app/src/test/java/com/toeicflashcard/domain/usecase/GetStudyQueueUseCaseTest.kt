package com.toeicflashcard.domain.usecase

import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetStudyQueueUseCaseTest {

    private class FakeTimeProvider(private val now: Long) : TimeProvider {
        override fun nowMillis(): Long = now
        override fun endOfTodayMillis(): Long = now
        override fun todayEpochDay(): Long = 10L
        override fun epochDayForMillis(timestampMillis: Long): Long = 10L
    }

    private class FakeVocabularyRepository(
        private val words: List<VocabularyWord>,
        private val cachedWords: List<VocabularyWord>?
    ) : VocabularyRepository {
        override suspend fun ensureSeeded() = Unit
        override fun getCachedAllWords(): List<VocabularyWord>? = cachedWords
        override fun observeAllWords(): Flow<List<VocabularyWord>> = flowOf(words)
        override suspend fun getAllWords(): List<VocabularyWord> = words
        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> =
            words.filter { it.isDue(cutoffMillis) }
                .sortedWith(compareBy<VocabularyWord> { it.nextReviewTimestamp }.thenBy { it.id })
        override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> =
            words.filter { it.topic == topic }
                .sortedWith(compareBy<VocabularyWord> { it.nextReviewTimestamp }.thenBy { it.id })
        override suspend fun updateWord(word: VocabularyWord) = Unit
        override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) = Unit
    }

    private fun word(
        id: Int,
        topic: String,
        nextReviewTimestamp: Long,
        easeFactor: Float = 2.5f,
        lastReviewedTimestamp: Long = 0L,
        deckId: String = VocabularyDecks.CORE_775_ID
    ) = VocabularyWord(
        id = id,
        topic = topic,
        word = "word_$id",
        phonetic = "/w$id/",
        partOfSpeech = "n.",
        meaningVi = "nghĩa $id",
        exampleEn = "Example $id",
        exampleVi = "Ví dụ $id",
        easeFactor = easeFactor,
        lastReviewedTimestamp = lastReviewedTimestamp,
        nextReviewTimestamp = nextReviewTimestamp,
        deckId = deckId,
        deckName = VocabularyDecks.displayNameFor(deckId)
    )

    @Test
    fun `getCachedIfAvailable returns null when cache is absent or empty`() {
        val nullRepo = FakeVocabularyRepository(emptyList(), null)
        val emptyRepo = FakeVocabularyRepository(emptyList(), emptyList())
        val timeProvider = FakeTimeProvider(1_000L)

        assertNull(GetStudyQueueUseCase(nullRepo, timeProvider).getCachedIfAvailable("Business & Office"))
        assertNull(GetStudyQueueUseCase(emptyRepo, timeProvider).getCachedIfAvailable("Business & Office"))
    }

    @Test
    fun `getCachedIfAvailable matches invoke for topic due, topic practice, daily due, and hardWordsOnly`() = runTest {
        val sample = listOf(
            word(id = 4, topic = "Finance & Banking", nextReviewTimestamp = 5_000L, easeFactor = 1.4f, lastReviewedTimestamp = 500L),
            word(id = 2, topic = "Finance & Banking", nextReviewTimestamp = 3_000L, easeFactor = 2.5f, lastReviewedTimestamp = 500L),
            word(id = 1, topic = "Business & Office", nextReviewTimestamp = 900L, easeFactor = 2.0f, lastReviewedTimestamp = 500L),
            word(id = 3, topic = "Business & Office", nextReviewTimestamp = 800L, easeFactor = 2.6f, lastReviewedTimestamp = 500L),
            word(id = 5, topic = "Marketing", nextReviewTimestamp = 950L, easeFactor = 1.7f, lastReviewedTimestamp = 500L)
        )
        val useCase = GetStudyQueueUseCase(
            vocabularyRepository = FakeVocabularyRepository(words = sample, cachedWords = sample),
            timeProvider = FakeTimeProvider(now = 1_000L)
        )

        val queries = listOf(
            "Business & Office" to false,
            "Finance & Banking" to false,
            null to false,
            null to true
        )

        for ((topic, hardOnly) in queries) {
            val cached = useCase.getCachedIfAvailable(topic = topic, hardWordsOnly = hardOnly)
            val suspended = useCase(topic = topic, hardWordsOnly = hardOnly)
            assertNotNull(cached)
            assertEquals(suspended, cached)
        }
    }

    @Test
    fun `deckId filters cached and suspended queues to requested curriculum deck`() = runTest {
        val sample = listOf(
            word(1, "Business & Office", 900L, easeFactor = 1.6f, lastReviewedTimestamp = 500L, deckId = VocabularyDecks.CORE_775_ID),
            word(1001, "L01 • Contracts", 800L, easeFactor = 1.5f, lastReviewedTimestamp = 500L, deckId = VocabularyDecks.ESSENTIAL_600_ID),
            word(2001, "Part 1 • Hành động & Việc làm", 700L, easeFactor = 1.9f, lastReviewedTimestamp = 500L, deckId = VocabularyDecks.LEVEL_450_800_ID)
        )
        val useCase = GetStudyQueueUseCase(
            vocabularyRepository = FakeVocabularyRepository(words = sample, cachedWords = sample),
            timeProvider = FakeTimeProvider(now = 1_000L)
        )

        val due600Suspended = useCase(topic = null, hardWordsOnly = false, deckId = VocabularyDecks.ESSENTIAL_600_ID)
        val due600Cached = useCase.getCachedIfAvailable(topic = null, hardWordsOnly = false, deckId = VocabularyDecks.ESSENTIAL_600_ID)
        assertEquals(listOf(1001), due600Suspended.words.map { it.id })
        assertEquals(due600Suspended, due600Cached)

        val hard800Suspended = useCase(topic = null, hardWordsOnly = true, deckId = VocabularyDecks.LEVEL_450_800_ID)
        val hard800Cached = useCase.getCachedIfAvailable(topic = null, hardWordsOnly = true, deckId = VocabularyDecks.LEVEL_450_800_ID)
        assertTrue(hard800Suspended.words.all { it.deckId == VocabularyDecks.LEVEL_450_800_ID })
        assertEquals(listOf(2001), hard800Suspended.words.map { it.id })
        assertEquals(hard800Suspended, hard800Cached)

        val allDecks = useCase(topic = null, hardWordsOnly = false, deckId = VocabularyDecks.ALL_ID)
        assertEquals(listOf(2001, 1001, 1), allDecks.words.map { it.id })
    }
}
