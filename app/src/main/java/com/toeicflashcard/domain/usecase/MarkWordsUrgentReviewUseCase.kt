package com.toeicflashcard.domain.usecase

import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.repository.VocabularyRepository

class MarkWordsUrgentReviewUseCase(
    private val vocabularyRepository: VocabularyRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(wordIds: List<Int>) {
        if (wordIds.isEmpty()) return
        vocabularyRepository.markWordsForUrgentReview(
            wordIds = wordIds.distinct(),
            nowMillis = timeProvider.nowMillis()
        )
    }
}
