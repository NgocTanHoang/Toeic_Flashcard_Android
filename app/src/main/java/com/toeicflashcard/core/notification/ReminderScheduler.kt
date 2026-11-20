package com.toeicflashcard.core.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.toeicflashcard.data.worker.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

interface ReminderScheduler {
    fun scheduleDailyReminder(hour: Int, minute: Int)
    fun cancelDailyReminder()
}

class WorkManagerReminderScheduler(context: Context) : ReminderScheduler {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun scheduleDailyReminder(hour: Int, minute: Int) {
        val clampedHour = hour.coerceIn(0, 23)
        val clampedMinute = minute.coerceIn(0, 59)
        val initialDelayMillis = computeInitialDelayMillis(clampedHour, clampedMinute)

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24L, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancelDailyReminder() {
        workManager.cancelUniqueWork(REMINDER_WORK_NAME)
    }

    private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    companion object {
        const val REMINDER_WORK_NAME = "toeic_daily_reminder_work"
    }
}
