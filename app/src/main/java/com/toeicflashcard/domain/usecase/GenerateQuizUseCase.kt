package com.toeicflashcard.domain.usecase

import com.toeicflashcard.domain.model.QuizOption
import com.toeicflashcard.domain.model.QuizQuestion
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.VocabularyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class GenerateQuizUseCase(
    private val vocabularyRepository: VocabularyRepository,
    private val random: Random = Random.Default,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    /**
     * Synchronously builds quiz questions from the in-memory word cache when available,
     * eliminating initial loading spinner frames on navigation entry.
     */
    fun generateFromCacheIfAvailable(
        topic: String? = null,
        maxQuestions: Int = DEFAULT_QUIZ_SIZE,
        deckId: String? = null
    ): List<QuizQuestion>? {
        val cached = vocabularyRepository.getCachedAllWords()
        if (cached.isNullOrEmpty()) return null
        return buildQuestions(
            allWords = cached,
            topic = topic,
            maxQuestions = maxQuestions,
            deckId = deckId
        )
    }

    suspend operator fun invoke(
        topic: String? = null,
        maxQuestions: Int = DEFAULT_QUIZ_SIZE,
        deckId: String? = null
    ): List<QuizQuestion> = withContext(defaultDispatcher) {
        vocabularyRepository.ensureSeeded()
        val allWords = vocabularyRepository.getAllWords()
        buildQuestions(
            allWords = allWords,
            topic = topic,
            maxQuestions = maxQuestions,
            deckId = deckId
        )
    }

    private fun buildQuestions(
        allWords: List<VocabularyWord>,
        topic: String?,
        maxQuestions: Int,
        deckId: String? = null
    ): List<QuizQuestion> {
        if (allWords.isEmpty()) return emptyList()

        val activeDeckId = normalizeActiveDeckId(deckId)
        val deckPool = if (activeDeckId == null) {
            allWords
        } else {
            allWords.filter { it.deckId == activeDeckId }
        }
        val targetPool = if (topic.isNullOrBlank()) {
            deckPool
        } else {
            deckPool.filter { it.topic == topic }
        }
        if (targetPool.isEmpty()) return emptyList()

        val selectedWords = targetPool
            .shuffled(random)
            .take(maxQuestions.coerceAtLeast(1))

        return selectedWords.map { word ->
            val distractors = allWords
                .filter { it.id != word.id && it.meaningVi != word.meaningVi }
                .distinctBy { it.meaningVi }
                .shuffled(random)
                .take(DISTRACTOR_COUNT)
                .map { QuizOption(textVi = it.meaningVi, isCorrect = false) }

            val options = (listOf(QuizOption(textVi = word.meaningVi, isCorrect = true)) + distractors)
                .shuffled(random)

            QuizQuestion(
                word = word,
                options = options
            )
        }
    }

    private fun normalizeActiveDeckId(deckId: String?): String? {
        if (deckId.isNullOrBlank()) return null
        val normalized = com.toeicflashcard.domain.model.VocabularyDecks.normalizeSelectedDeckId(deckId)
        return if (normalized == com.toeicflashcard.domain.model.VocabularyDecks.ALL_ID) null else normalized
    }

    companion object {
        const val DEFAULT_QUIZ_SIZE = 10
        private const val DISTRACTOR_COUNT = 3
    }
}
