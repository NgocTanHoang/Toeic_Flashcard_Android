package com.toeicflashcard.domain.usecase

import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.HomeSummary
import com.toeicflashcard.domain.model.TopicProgress
import com.toeicflashcard.domain.repository.VocabularyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ObserveHomeSummaryUseCase(
    private val vocabularyRepository: VocabularyRepository,
    private val timeProvider: TimeProvider,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    operator fun invoke(): Flow<HomeSummary> =
        invoke(kotlinx.coroutines.flow.flowOf(com.toeicflashcard.domain.model.VocabularyDecks.ALL_ID))

    operator fun invoke(selectedDeckIdFlow: Flow<String>): Flow<HomeSummary> {
        return kotlinx.coroutines.flow.combine(
            vocabularyRepository.observeAllWords(),
            selectedDeckIdFlow
        ) { words, rawSelectedDeckId ->
            computeSummary(words = words, rawSelectedDeckId = rawSelectedDeckId)
        }.flowOn(defaultDispatcher)
    }

    private fun computeSummary(
        words: List<com.toeicflashcard.domain.model.VocabularyWord>,
        rawSelectedDeckId: String
    ): HomeSummary {
        val selectedDeckId = com.toeicflashcard.domain.model.VocabularyDecks.normalizeSelectedDeckId(rawSelectedDeckId)
        val cutoffMillis = timeProvider.endOfTodayMillis()
        val wordsByDeck = words.groupBy { it.deckId }

        val decks = com.toeicflashcard.domain.model.VocabularyDecks.orderedSelectableIds.map { deckId ->
            val deckWords = if (deckId == com.toeicflashcard.domain.model.VocabularyDecks.ALL_ID) {
                words
            } else {
                wordsByDeck[deckId].orEmpty()
            }
            com.toeicflashcard.domain.model.DeckProgress(
                deckId = deckId,
                deckName = com.toeicflashcard.domain.model.VocabularyDecks.displayNameFor(deckId),
                learnedCount = deckWords.count { it.isLearned },
                totalCount = deckWords.size,
                dueCount = deckWords.count { it.isDue(cutoffMillis) },
                topicCount = deckWords.map { it.topic }.distinct().size
            )
        }

        val activeWords = if (selectedDeckId == com.toeicflashcard.domain.model.VocabularyDecks.ALL_ID) {
            words
        } else {
            wordsByDeck[selectedDeckId].orEmpty()
        }

        val dueTodayCount = activeWords.count { it.isDue(cutoffMillis) }
        val reviewDueCount = activeWords.count { it.repetitions > 0 && it.isDue(cutoffMillis) }
        val newWordsCount = activeWords.count { it.repetitions == 0 && !it.isLearned }
        val learnedCount = activeWords.count { it.isLearned }
        val grouped = activeWords.groupBy { it.topic }
        val orderedTopicNames = activeWords.map { it.topic }.distinct()

        val topicProgressList = orderedTopicNames.map { topic ->
            val topicWords = grouped[topic].orEmpty()
            TopicProgress(
                topic = topic,
                totalCount = topicWords.size,
                learnedCount = topicWords.count { it.isLearned },
                dueCount = topicWords.count { it.isDue(cutoffMillis) }
            )
        }

        val continueTopic = topicProgressList
            .filter { it.learnedCount in 1 until it.totalCount }
            .maxByOrNull { progress ->
                grouped[progress.topic].orEmpty().maxOfOrNull { it.lastReviewedTimestamp } ?: 0L
            }

        return HomeSummary(
            dueTodayCount = dueTodayCount,
            reviewDueCount = reviewDueCount,
            newWordsCount = newWordsCount,
            learnedCount = learnedCount,
            totalCount = activeWords.size,
            topics = topicProgressList,
            continueTopic = continueTopic,
            decks = decks,
            selectedDeckId = selectedDeckId,
            hasAnyDatabaseWords = words.isNotEmpty()
        )
    }
}
