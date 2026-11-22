package com.toeicflashcard.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.toeicflashcard.ToeicApplication
import com.toeicflashcard.core.notification.ReminderNotificationHelper
import kotlinx.coroutines.flow.first

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ToeicApplication ?: return Result.failure()
        val container = app.container

        container.vocabularyRepository.ensureSeeded()
        val dueWords = container.vocabularyRepository.getDueWords(
            cutoffMillis = container.timeProvider.nowMillis()
        )
        val prefs = container.userPreferencesRepository.userPreferences.first()
        if (!prefs.reminderEnabled) return Result.success()
        val effectiveStreak = container.streakCalculator.effectiveStateForDisplay(
            stored = prefs.toStreakState(),
            todayEpochDay = container.timeProvider.todayEpochDay()
        )
        val remainingGoal = (prefs.dailyGoal - effectiveStreak.dailyReviewedCount).coerceAtLeast(0)

        if (dueWords.isNotEmpty() || remainingGoal > 0) {
            ReminderNotificationHelper.showDailyReminderNotification(
                context = applicationContext,
                dueCount = dueWords.size,
                streakDays = effectiveStreak.currentStreak,
                remainingGoal = remainingGoal
            )
        }

        return Result.success()
    }
}
