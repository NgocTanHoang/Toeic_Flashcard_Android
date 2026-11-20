package com.toeicflashcard

import android.app.Application
import com.toeicflashcard.core.notification.ReminderNotificationHelper
import com.toeicflashcard.di.AppContainer

class ToeicApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderNotificationHelper.ensureNotificationChannel(this)
    }
}
