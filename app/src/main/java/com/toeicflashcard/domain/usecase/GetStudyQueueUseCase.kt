package com.toeicflashcard.domain.usecase

import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.VocabularyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StudyQueueResult(
    val words: List<VocabularyWord>,
    val isPracticeMode: Boolean
)

class GetStudyQueueUseCase(
    private val vocabularyRepository: VocabularyRepository,
    private val timeProvider: TimeProvider,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    /**
     * Synchronously computes the initial study queue from the in-memory word cache when warm,
     * allowing [StudyViewModel] to render the first flashcard on Frame 0 during push transitions.
     */
    fun getCachedIfAvailable(
        topic: String?,
        hardWordsOnly: Boolean = false,
        deckId: String? = null
    ): StudyQueueResult? {
        val cached = vocabularyRepository.getCachedAllWords()
        if (cached.isNullOrEmpty()) return null
        val learnedWordKeys = cached
            .asSequence()
            .filter { it.isLearned }
            .map { VocabularyDecks.normalizeWordKey(it.word) }
            .toHashSet()
        val scopedWords = filterByDeck(cached, deckId)
        return buildQueueFromAllWords(
            allWords = scopedWords,
            topic = topic,
            hardWordsOnly = hardWordsOnly,
            learnedWordKeys = learnedWordKeys
        )
    }

    suspend operator fun invoke(
        topic: String?,
        hardWordsOnly: Boolean = false,
        deckId: String? = null
    ): StudyQueueResult = withContext(defaultDispatcher) {
        vocabularyRepository.ensureSeeded()

        val activeDeckId = normalizeActiveDeckId(deckId)
        val allRepositoryWords = vocabularyRepository.getCachedAllWords()
            ?.takeIf { it.isNotEmpty() }
            ?: vocabularyRepository.getAllWords()
        val learnedWordKeys = allRepositoryWords
            .asSequence()
            .filter { it.isLearned }
            .map { VocabularyDecks.normalizeWordKey(it.word) }
            .toHashSet()

        if (hardWordsOnly) {
            val allWords = filterByDeck(allRepositoryWords, activeDeckId)
            return@withContext buildHardWordsQueue(allWords)
        }

        val cutoffMillis = timeProvider.endOfTodayMillis()
        if (topic.isNullOrBlank()) {
            val rawDueWords = vocabularyRepository.getDueWords(cutoffMillis)
            val scopedDueWords = if (activeDeckId == null) {
                rawDueWords
            } else {
                rawDueWords.filter { it.deckId == activeDeckId }
            }
            val deduplicatedDueWords = deduplicateDueWords(
                words = scopedDueWords,
                learnedWordKeys = learnedWordKeys
            ).take(DEFAULT_DAILY_SESSION_LIMIT)
            return@withContext StudyQueueResult(words = deduplicatedDueWords, isPracticeMode = false)
        }

        val topicWords = filterByDeck(vocabularyRepository.getWordsByTopic(topic), activeDeckId)
        buildTopicQueue(
            topicWords = topicWords,
            cutoffMillis = cutoffMillis,
            learnedWordKeys = learnedWordKeys
        )
    }

    private fun normalizeActiveDeckId(deckId: String?): String? {
        if (deckId.isNullOrBlank()) return null
        val normalized = com.toeicflashcard.domain.model.VocabularyDecks.normalizeSelectedDeckId(deckId)
        return if (normalized == com.toeicflashcard.domain.model.VocabularyDecks.ALL_ID) null else normalized
    }

    private fun filterByDeck(
        words: List<com.toeicflashcard.domain.model.VocabularyWord>,
        deckId: String?
    ): List<com.toeicflashcard.domain.model.VocabularyWord> {
        val activeDeckId = normalizeActiveDeckId(deckId) ?: return words
        return words.filter { it.deckId == activeDeckId }
    }

    private fun buildQueueFromAllWords(
        allWords: List<VocabularyWord>,
        topic: String?,
        hardWordsOnly: Boolean,
        learnedWordKeys: Set<String>
    ): StudyQueueResult {
        if (hardWordsOnly) {
            return buildHardWordsQueue(allWords)
        }

        val cutoffMillis = timeProvider.endOfTodayMillis()
        if (topic.isNullOrBlank()) {
            val dueWords = deduplicateDueWords(
                words = allWords
                    .filter { it.isDue(cutoffMillis) }
                    .sortedWith(DEFAULT_ORDER_COMPARATOR),
                learnedWordKeys = learnedWordKeys
            ).take(DEFAULT_DAILY_SESSION_LIMIT)
            return StudyQueueResult(words = dueWords, isPracticeMode = false)
        }

        val topicWords = allWords
            .filter { it.topic == topic }
            .sortedWith(DEFAULT_ORDER_COMPARATOR)
        return buildTopicQueue(
            topicWords = topicWords,
            cutoffMillis = cutoffMillis,
            learnedWordKeys = learnedWordKeys
        )
    }

    private fun buildHardWordsQueue(allWords: List<VocabularyWord>): StudyQueueResult {
        val reviewedOrHard = allWords.filter { it.lastReviewedTimestamp > 0L || it.easeFactor < 2.5f }
        val pool = reviewedOrHard.ifEmpty { allWords }
        val hardWords = pool
            .sortedWith(
                compareBy<VocabularyWord> { it.easeFactor }
                    .thenBy { it.repetitions }
                    .thenBy { it.id }
            )
            .distinctBy { VocabularyDecks.normalizeWordKey(it.word) }
            .take(DEFAULT_DAILY_SESSION_LIMIT)
        return StudyQueueResult(words = hardWords, isPracticeMode = true)
    }

    private fun buildTopicQueue(
        topicWords: List<VocabularyWord>,
        cutoffMillis: Long,
        learnedWordKeys: Set<String>
    ): StudyQueueResult {
        val dueInTopic = deduplicateDueWords(
            words = topicWords.filter { it.isDue(cutoffMillis) },
            learnedWordKeys = learnedWordKeys
        )
        return if (dueInTopic.isNotEmpty()) {
            StudyQueueResult(words = dueInTopic, isPracticeMode = false)
        } else {
            val practiceWords = topicWords
                .sortedWith(DEFAULT_ORDER_COMPARATOR)
                .distinctBy { VocabularyDecks.normalizeWordKey(it.word) }
            StudyQueueResult(words = practiceWords, isPracticeMode = true)
        }
    }

    private fun deduplicateDueWords(
        words: List<VocabularyWord>,
        learnedWordKeys: Set<String>
    ): List<VocabularyWord> {
        return words
            .filterNot { word ->
                // Skip brand-new word if the exact English word is already learned in another deck
                word.repetitions == 0 && VocabularyDecks.normalizeWordKey(word.word) in learnedWordKeys
            }
            .distinctBy { VocabularyDecks.normalizeWordKey(it.word) }
    }

    suspend fun getNextNewWordsQueue(
        deckId: String?,
        limit: Int = DEFAULT_DAILY_SESSION_LIMIT
    ): StudyQueueResult = withContext(defaultDispatcher) {
        vocabularyRepository.ensureSeeded()
        val allRepositoryWords = vocabularyRepository.getCachedAllWords()
            ?.takeIf { it.isNotEmpty() }
            ?: vocabularyRepository.getAllWords()
        val learnedWordKeys = allRepositoryWords
            .asSequence()
            .filter { it.isLearned }
            .map { VocabularyDecks.normalizeWordKey(it.word) }
            .toHashSet()
        val scopedWords = filterByDeck(allRepositoryWords, deckId)
        val newWords = scopedWords
            .asSequence()
            .filter { it.repetitions == 0 && !it.isLearned }
            .filterNot { VocabularyDecks.normalizeWordKey(it.word) in learnedWordKeys }
            .sortedWith(DEFAULT_ORDER_COMPARATOR)
            .distinctBy { VocabularyDecks.normalizeWordKey(it.word) }
            .take(limit.coerceAtLeast(1))
            .toList()
        StudyQueueResult(words = newWords, isPracticeMode = false)
    }

    suspend fun countRemainingNewWords(deckId: String?): Int = withContext(defaultDispatcher) {
        val allRepositoryWords = vocabularyRepository.getCachedAllWords()
            ?.takeIf { it.isNotEmpty() }
            ?: vocabularyRepository.getAllWords()
        val learnedWordKeys = allRepositoryWords
            .asSequence()
            .filter { it.isLearned }
            .map { VocabularyDecks.normalizeWordKey(it.word) }
            .toHashSet()
        val scopedWords = filterByDeck(allRepositoryWords, deckId)
        scopedWords
            .asSequence()
            .filter { it.repetitions == 0 && !it.isLearned }
            .filterNot { VocabularyDecks.normalizeWordKey(it.word) in learnedWordKeys }
            .map { VocabularyDecks.normalizeWordKey(it.word) }
            .distinct()
            .count()
    }

    companion object {
        const val DEFAULT_DAILY_SESSION_LIMIT = 15

        private val DEFAULT_ORDER_COMPARATOR =
            compareBy<VocabularyWord> { it.nextReviewTimestamp }.thenBy { it.id }
    }
}
