package com.toeicflashcard.domain.usecase

import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.VocabularyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveHomeSummaryUseCaseTest {

    private class FakeTimeProvider(private val cutoffMillis: Long) : TimeProvider {
        override fun nowMillis(): Long = cutoffMillis
        override fun endOfTodayMillis(): Long = cutoffMillis
        override fun todayEpochDay(): Long = 10L
        override fun epochDayForMillis(timestampMillis: Long): Long = 10L
    }

    private class FakeVocabularyRepository(
        private val words: List<VocabularyWord>
    ) : VocabularyRepository {
        override suspend fun ensureSeeded() = Unit
        override fun getCachedAllWords(): List<VocabularyWord> = words
        override fun observeAllWords(): Flow<List<VocabularyWord>> = flowOf(words)
        override suspend fun getAllWords(): List<VocabularyWord> = words
        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> =
            words.filter { it.isDue(cutoffMillis) }
        override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> =
            words.filter { it.topic == topic }
        override suspend fun updateWord(word: VocabularyWord) = Unit
        override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) = Unit
    }

    private fun word(
        id: Int,
        topic: String,
        repetitions: Int = 0,
        nextReviewTimestamp: Long = 0L,
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
        repetitions = repetitions,
        nextReviewTimestamp = nextReviewTimestamp,
        lastReviewedTimestamp = lastReviewedTimestamp,
        deckId = deckId,
        deckName = VocabularyDecks.displayNameFor(deckId)
    )

    @Test
    fun `separates reviewDueCount from newWordsCount and excludes future scheduled words`() = runTest {
        val cutoff = 1_000L
        val sample = listOf(
            // Studied & due
            word(id = 1, topic = "Marketing", repetitions = 2, nextReviewTimestamp = 800L, lastReviewedTimestamp = 400L),
            word(id = 2, topic = "Marketing", repetitions = 1, nextReviewTimestamp = 1_000L, lastReviewedTimestamp = 500L),
            // Studied & scheduled in the future (neither reviewDue nor new)
            word(id = 3, topic = "Marketing", repetitions = 1, nextReviewTimestamp = 2_000L, lastReviewedTimestamp = 600L),
            // Brand new unstudied words
            word(id = 4, topic = "Marketing", repetitions = 0, nextReviewTimestamp = 0L, lastReviewedTimestamp = 0L),
            word(id = 5, topic = "Finance & Banking", repetitions = 0, nextReviewTimestamp = 0L, lastReviewedTimestamp = 0L)
        )

        val useCase = ObserveHomeSummaryUseCase(
            vocabularyRepository = FakeVocabularyRepository(sample),
            timeProvider = FakeTimeProvider(cutoff),
            defaultDispatcher = Dispatchers.Unconfined
        )

        val summary = useCase(flowOf(VocabularyDecks.ALL_ID)).first()

        assertEquals(4, summary.dueTodayCount)
        assertEquals(2, summary.reviewDueCount)
        assertEquals(2, summary.newWordsCount)
        assertEquals(3, summary.learnedCount)
        assertEquals(5, summary.totalCount)
    }

    @Test
    fun `continueTopic picks most recently reviewed in-progress topic and ignores completed or untouched topics`() = runTest {
        val cutoff = 1_000L
        val sample = listOf(
            // Fully completed topic (2/2 learned, most recent timestamp should be ignored)
            word(id = 1, topic = "Contracts", repetitions = 2, nextReviewTimestamp = 2_000L, lastReviewedTimestamp = 9_999L),
            word(id = 2, topic = "Contracts", repetitions = 1, nextReviewTimestamp = 2_000L, lastReviewedTimestamp = 9_999L),
            // In-progress topic A (older review)
            word(id = 3, topic = "Marketing", repetitions = 1, nextReviewTimestamp = 2_000L, lastReviewedTimestamp = 1_500L),
            word(id = 4, topic = "Marketing", repetitions = 0, nextReviewTimestamp = 0L, lastReviewedTimestamp = 0L),
            // In-progress topic B (more recent review -> should win)
            word(id = 5, topic = "Finance & Banking", repetitions = 1, nextReviewTimestamp = 2_000L, lastReviewedTimestamp = 4_200L),
            word(id = 6, topic = "Finance & Banking", repetitions = 0, nextReviewTimestamp = 0L, lastReviewedTimestamp = 0L),
            // Untouched topic (0/1)
            word(id = 7, topic = "Conferences", repetitions = 0, nextReviewTimestamp = 0L, lastReviewedTimestamp = 0L)
        )

        val useCase = ObserveHomeSummaryUseCase(
            vocabularyRepository = FakeVocabularyRepository(sample),
            timeProvider = FakeTimeProvider(cutoff),
            defaultDispatcher = Dispatchers.Unconfined
        )

        val summary = useCase(flowOf(VocabularyDecks.ALL_ID)).first()
        assertEquals("Finance & Banking", summary.continueTopic?.topic)
        assertEquals(1, summary.continueTopic?.learnedCount)
        assertEquals(2, summary.continueTopic?.totalCount)
    }

    @Test
    fun `continueTopic is null when no topic is partially completed and deck filter scopes counts`() = runTest {
        val cutoff = 1_000L
        val sample = listOf(
            word(id = 1, topic = "Marketing", repetitions = 1, nextReviewTimestamp = 900L, lastReviewedTimestamp = 500L, deckId = VocabularyDecks.CORE_775_ID),
            word(id = 2, topic = "Marketing", repetitions = 1, nextReviewTimestamp = 2_000L, lastReviewedTimestamp = 600L, deckId = VocabularyDecks.CORE_775_ID),
            word(id = 10, topic = "L01 • Contracts", repetitions = 0, nextReviewTimestamp = 0L, lastReviewedTimestamp = 0L, deckId = VocabularyDecks.ESSENTIAL_600_ID)
        )

        val useCase = ObserveHomeSummaryUseCase(
            vocabularyRepository = FakeVocabularyRepository(sample),
            timeProvider = FakeTimeProvider(cutoff),
            defaultDispatcher = Dispatchers.Unconfined
        )

        val coreSummary = useCase(flowOf(VocabularyDecks.CORE_775_ID)).first()
        assertNull(coreSummary.continueTopic)
        assertEquals(1, coreSummary.reviewDueCount)
        assertEquals(0, coreSummary.newWordsCount)

        val essentialSummary = useCase(flowOf(VocabularyDecks.ESSENTIAL_600_ID)).first()
        assertNull(essentialSummary.continueTopic)
        assertEquals(0, essentialSummary.reviewDueCount)
        assertEquals(1, essentialSummary.newWordsCount)
    }
}
