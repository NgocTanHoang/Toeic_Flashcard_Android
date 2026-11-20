package com.toeicflashcard.domain.usecase

import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.scheduler.Sm2Scheduler
import com.toeicflashcard.domain.streak.StreakCalculator

class SubmitReviewUseCase(
    private val vocabularyRepository: VocabularyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val scheduler: Sm2Scheduler,
    private val streakCalculator: StreakCalculator,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        word: VocabularyWord,
        quality: ReviewQuality
    ): VocabularyWord {
        val nowMillis = timeProvider.nowMillis()
        val result = scheduler.calculateForWord(
            word = word,
            quality = quality,
            nowMillis = nowMillis
        )
        val updatedWord = word.copy(
            repetitions = result.repetitions,
            intervalDays = result.intervalDays,
            easeFactor = result.easeFactor,
            nextReviewTimestamp = result.nextReviewTimestamp,
            lastReviewedTimestamp = nowMillis
        )
        vocabularyRepository.updateWord(updatedWord)
        userPreferencesRepository.recordStudyReview(
            todayEpochDay = timeProvider.todayEpochDay(),
            calculator = streakCalculator
        )
        return updatedWord
    }
}
