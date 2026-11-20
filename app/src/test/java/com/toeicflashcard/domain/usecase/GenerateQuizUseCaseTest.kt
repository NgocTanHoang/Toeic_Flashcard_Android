package com.toeicflashcard.domain.usecase

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
import kotlin.random.Random

class GenerateQuizUseCaseTest {

    private class FakeVocabularyRepository(
        private val words: List<VocabularyWord>,
        private val cachedWords: List<VocabularyWord>? = null
    ) : VocabularyRepository {
        override suspend fun ensureSeeded() = Unit
        override fun getCachedAllWords(): List<VocabularyWord>? = cachedWords
        override fun observeAllWords(): Flow<List<VocabularyWord>> = flowOf(words)
        override suspend fun getAllWords(): List<VocabularyWord> = words
        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> = words
        override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> =
            words.filter { it.topic == topic }
        override suspend fun updateWord(word: VocabularyWord) = Unit
        override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) = Unit
    }

    private val sampleWords: List<VocabularyWord> = (1..30).map { index ->
        val topic = when {
            index <= 10 -> "Business & Office"
            index <= 20 -> "Finance & Banking"
            else -> "Travel & Logistics"
        }
        VocabularyWord(
            id = index,
            topic = topic,
            word = "word_$index",
            phonetic = "/wɜːrd$index/",
            partOfSpeech = "n.",
            meaningVi = "nghĩa tiếng Việt $index",
            exampleEn = "Example sentence $index.",
            exampleVi = "Câu ví dụ $index."
        )
    }

    @Test
    fun `generate quiz across all topics returns 10 questions with 4 unique options and 1 correct answer`() = runTest {
        val useCase = GenerateQuizUseCase(
            vocabularyRepository = FakeVocabularyRepository(sampleWords),
            random = Random(42)
        )

        val questions = useCase(topic = null)

        assertEquals(10, questions.size)
        assertEquals(10, questions.map { it.word.id }.toSet().size)

        questions.forEach { question ->
            assertEquals(4, question.options.size)
            assertEquals(1, question.options.count { it.isCorrect })
            assertEquals(4, question.options.map { it.textVi }.toSet().size)
            val correctOption = question.options.first { it.isCorrect }
            assertEquals(question.word.meaningVi, correctOption.textVi)
        }
    }

    @Test
    fun `generate quiz for a single topic filters questions to that topic`() = runTest {
        val useCase = GenerateQuizUseCase(
            vocabularyRepository = FakeVocabularyRepository(sampleWords),
            random = Random(7)
        )

        val questions = useCase(topic = "Finance & Banking")

        assertEquals(10, questions.size)
        assertTrue(questions.all { it.word.topic == "Finance & Banking" })
        questions.forEach { question ->
            assertEquals(4, question.options.size)
            assertEquals(1, question.options.count { it.isCorrect })
            assertEquals(4, question.options.map { it.textVi }.toSet().size)
        }
    }

    @Test
    fun `generate quiz respects custom maxQuestions limit`() = runTest {
        val useCase = GenerateQuizUseCase(
            vocabularyRepository = FakeVocabularyRepository(sampleWords),
            random = Random(99)
        )

        val questions = useCase(topic = null, maxQuestions = 5)
        assertEquals(5, questions.size)
    }

    @Test
    fun `generateFromCacheIfAvailable returns null when cache is absent and filtered questions when warm`() {
        val coldUseCase = GenerateQuizUseCase(
            vocabularyRepository = FakeVocabularyRepository(words = sampleWords, cachedWords = null),
            random = Random(7)
        )
        assertNull(coldUseCase.generateFromCacheIfAvailable(topic = "Finance & Banking"))

        val warmUseCase = GenerateQuizUseCase(
            vocabularyRepository = FakeVocabularyRepository(words = emptyList(), cachedWords = sampleWords),
            random = Random(7)
        )
        val questions = warmUseCase.generateFromCacheIfAvailable(topic = "Finance & Banking")
        assertNotNull(questions)
        assertEquals(10, questions!!.size)
        assertTrue(questions.all { it.word.topic == "Finance & Banking" })
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(1, q.options.count { it.isCorrect })
            assertEquals(4, q.options.map { it.textVi }.toSet().size)
        }
    }

    @Test
    fun `generate quiz filters questions by deckId across cached and suspended calls`() = runTest {
        val multiDeckWords = sampleWords.mapIndexed { idx, w ->
            val deckId = if (idx < 15) VocabularyDecks.ESSENTIAL_600_ID else VocabularyDecks.LEVEL_450_800_ID
            w.copy(deckId = deckId, deckName = VocabularyDecks.displayNameFor(deckId))
        }
        val useCase = GenerateQuizUseCase(
            vocabularyRepository = FakeVocabularyRepository(words = multiDeckWords, cachedWords = multiDeckWords),
            random = Random(12)
        )

        val deckSuspended = useCase(topic = null, maxQuestions = 10, deckId = VocabularyDecks.ESSENTIAL_600_ID)
        val deckCached = useCase.generateFromCacheIfAvailable(topic = null, maxQuestions = 10, deckId = VocabularyDecks.ESSENTIAL_600_ID)
        assertEquals(10, deckSuspended.size)
        assertTrue(deckSuspended.all { it.word.deckId == VocabularyDecks.ESSENTIAL_600_ID })
        assertNotNull(deckCached)
        assertTrue(deckCached!!.all { it.word.deckId == VocabularyDecks.ESSENTIAL_600_ID })
    }
}
