package com.toeicflashcard.domain.usecase

import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.scheduler.Sm2Scheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddCustomVocabularyUseCaseTest {

    private class FakeTimeProvider(private val now: Long) : TimeProvider {
        override fun nowMillis(): Long = now
        override fun endOfTodayMillis(): Long = now + 1_000L
        override fun todayEpochDay(): Long = 20_000L
        override fun epochDayForMillis(timestampMillis: Long): Long = 20_000L
    }

    private class MutableFakeVocabularyRepository(
        initialWords: List<VocabularyWord>
    ) : VocabularyRepository {
        val stored = initialWords.toMutableList()
        var seededCalled = false

        override suspend fun ensureSeeded() {
            seededCalled = true
        }

        override fun observeAllWords(): Flow<List<VocabularyWord>> = flowOf(stored.toList())
        override suspend fun getAllWords(): List<VocabularyWord> = stored.toList()
        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> = stored.filter { it.isDue(cutoffMillis) }
        override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> = stored.filter { it.topic == topic }
        override suspend fun getMaxWordId(): Int? = stored.maxOfOrNull { it.id }

        override suspend fun insertCustomWord(word: VocabularyWord): Boolean {
            if (stored.any { it.id == word.id }) return false
            stored += word
            return true
        }

        override suspend fun insertCustomWords(words: List<VocabularyWord>): Int {
            var count = 0
            words.forEach { w ->
                if (insertCustomWord(w)) count++
            }
            return count
        }

        override suspend fun updateWord(word: VocabularyWord) = Unit
        override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) = Unit
    }

    private fun seedWord(id: Int, word: String) = VocabularyWord(
        id = id,
        topic = "Business & Office",
        word = word,
        phonetic = "/$word/",
        partOfSpeech = "n.",
        meaningVi = "nghĩa $word",
        exampleEn = "Example $word",
        exampleVi = "Ví dụ $word"
    )

    @Test
    fun `addSingleWord allocates ID above 3000, assigns custom_user deck, initializes SM-2 due today, and rejects blank or duplicate`() = runTest {
        val now = 1_700_000_000_000L
        val repo = MutableFakeVocabularyRepository(listOf(seedWord(775, "agenda")))
        val useCase = AddCustomVocabularyUseCase(repo, FakeTimeProvider(now))

        val blankWord = useCase.addSingleWord(CustomWordInput(word = "  ", meaningVi = "nghĩa"))
        assertTrue(blankWord is AddWordResult.ValidationError)

        val blankMeaning = useCase.addSingleWord(CustomWordInput(word = "milestone", meaningVi = ""))
        assertTrue(blankMeaning is AddWordResult.ValidationError)

        val duplicateExisting = useCase.addSingleWord(CustomWordInput(word = "  AgEnDa ", meaningVi = "chương trình"))
        assertTrue(duplicateExisting is AddWordResult.DuplicateWord)

        val success = useCase.addSingleWord(
            CustomWordInput(
                word = "  milestone  ",
                phonetic = " /ˈmaɪlstoʊn/ ",
                partOfSpeech = "n.",
                meaningVi = " cột mốc ",
                topic = "   "
            )
        )
        assertTrue(success is AddWordResult.Success)
        val added = (success as AddWordResult.Success).addedWord
        assertTrue(repo.seededCalled)
        assertEquals(3001, added.id)
        assertEquals(VocabularyDecks.CUSTOM_USER_ID, added.deckId)
        assertEquals(VocabularyDecks.CUSTOM_USER_NAME, added.deckName)
        assertEquals("milestone", added.word)
        assertEquals("cột mốc", added.meaningVi)
        assertEquals(AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC, added.topic)
        assertEquals(0, added.repetitions)
        assertEquals(0, added.intervalDays)
        assertEquals(Sm2Scheduler.DEFAULT_EASE_FACTOR, added.easeFactor, 0.0001f)
        assertEquals(now, added.nextReviewTimestamp)
        assertTrue(added.isDue(now))
    }

    @Test
    fun `importFromJson strips BOM, deduplicates DB and intra-file duplicates, skips invalid rows, and increments IDs`() = runTest {
        val now = 1_700_000_000_000L
        val repo = MutableFakeVocabularyRepository(listOf(seedWord(775, "agenda")))
        val useCase = AddCustomVocabularyUseCase(repo, FakeTimeProvider(now))

        val payload = "\uFEFF" + """
            [
              { "word": "Agenda", "meaningVi": "trùng với DB" },
              { "word": "synergy", "phonetic": "/ˈsɪnərdʒi/", "partOfSpeech": "n.", "meaningVi": "hợp lực", "topic": "Chiến lược" },
              { "word": "  SYNERGY ", "meaningVi": "trùng trong file" },
              { "word": "missingMeaning", "meaningVi": "   " },
              { "word": "leverage", "partOfSpeech": "v.", "meaningVi": "tận dụng" }
            ]
        """.trimIndent()

        val result = useCase.importFromJson(payload)
        assertNull(result.errorMessageVi)
        assertEquals(2, result.addedCount)
        assertEquals(2, result.skippedDuplicateCount)
        assertEquals(1, result.skippedInvalidCount)

        val insertedIds = repo.stored.drop(1).map { it.id }
        assertEquals(listOf(3001, 3002), insertedIds)
        assertTrue(repo.stored.drop(1).all { it.deckId == VocabularyDecks.CUSTOM_USER_ID })
        assertEquals("Chiến lược", repo.stored[1].topic)
        assertEquals(AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC, repo.stored[2].topic)
    }

    @Test
    fun `importFromJson returns readable Vietnamese error on malformed JSON`() = runTest {
        val repo = MutableFakeVocabularyRepository(emptyList())
        val useCase = AddCustomVocabularyUseCase(repo, FakeTimeProvider(1_000L))

        val bad = useCase.importFromJson("{not-an-array")
        assertEquals(0, bad.addedCount)
        assertNotNull(bad.errorMessageVi)
    }
}
