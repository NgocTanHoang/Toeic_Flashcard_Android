package com.toeicflashcard.di

import android.content.Context
import com.toeicflashcard.core.audio.TtsFeedback
import com.toeicflashcard.core.audio.TtsManager
import com.toeicflashcard.core.notification.ReminderScheduler
import com.toeicflashcard.core.notification.WorkManagerReminderScheduler
import com.toeicflashcard.core.time.SystemTimeProvider
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.data.local.database.ToeicDatabase
import com.toeicflashcard.data.local.database.VocabularySeeder
import com.toeicflashcard.data.repository.UserPreferencesRepositoryImpl
import com.toeicflashcard.data.repository.VocabularyRepositoryImpl
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.scheduler.Sm2Scheduler
import com.toeicflashcard.domain.streak.StreakCalculator
import com.toeicflashcard.domain.usecase.AddCustomVocabularyUseCase
import com.toeicflashcard.domain.usecase.GenerateQuizUseCase
import com.toeicflashcard.domain.usecase.GetStudyQueueUseCase
import com.toeicflashcard.domain.usecase.MarkWordsUrgentReviewUseCase
import com.toeicflashcard.domain.usecase.ObserveHomeSummaryUseCase
import com.toeicflashcard.domain.usecase.SubmitReviewUseCase

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val timeProvider: TimeProvider = SystemTimeProvider()

    private val vocabularySeeder: VocabularySeeder by lazy {
        VocabularySeeder(appContext)
    }

    private val database: ToeicDatabase by lazy {
        ToeicDatabase.getInstance(appContext, vocabularySeeder)
    }

    val vocabularyRepository: VocabularyRepository by lazy {
        VocabularyRepositoryImpl(
            vocabularyDao = database.vocabularyDao(),
            vocabularySeeder = vocabularySeeder
        )
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(appContext)
    }

    val sm2Scheduler: Sm2Scheduler by lazy {
        Sm2Scheduler()
    }

    val streakCalculator: StreakCalculator by lazy {
        StreakCalculator()
    }

    val reminderScheduler: ReminderScheduler by lazy {
        WorkManagerReminderScheduler(appContext)
    }

    val observeHomeSummaryUseCase: ObserveHomeSummaryUseCase by lazy {
        ObserveHomeSummaryUseCase(
            vocabularyRepository = vocabularyRepository,
            timeProvider = timeProvider
        )
    }

    val getStudyQueueUseCase: GetStudyQueueUseCase by lazy {
        GetStudyQueueUseCase(
            vocabularyRepository = vocabularyRepository,
            timeProvider = timeProvider
        )
    }

    val submitReviewUseCase: SubmitReviewUseCase by lazy {
        SubmitReviewUseCase(
            vocabularyRepository = vocabularyRepository,
            userPreferencesRepository = userPreferencesRepository,
            scheduler = sm2Scheduler,
            streakCalculator = streakCalculator,
            timeProvider = timeProvider
        )
    }

    val generateQuizUseCase: GenerateQuizUseCase by lazy {
        GenerateQuizUseCase(vocabularyRepository = vocabularyRepository)
    }

    val markWordsUrgentReviewUseCase: MarkWordsUrgentReviewUseCase by lazy {
        MarkWordsUrgentReviewUseCase(
            vocabularyRepository = vocabularyRepository,
            timeProvider = timeProvider
        )
    }

    val addCustomVocabularyUseCase: AddCustomVocabularyUseCase by lazy {
        AddCustomVocabularyUseCase(
            vocabularyRepository = vocabularyRepository,
            timeProvider = timeProvider
        )
    }

    fun createTtsManager(onDeferredFeedback: ((TtsFeedback) -> Unit)? = null): TtsManager {
        return TtsManager(
            context = appContext,
            onDeferredFeedback = onDeferredFeedback
        )
    }
}
