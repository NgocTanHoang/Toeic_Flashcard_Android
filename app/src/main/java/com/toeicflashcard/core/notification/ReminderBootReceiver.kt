package com.toeicflashcard.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.toeicflashcard.ToeicApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        val app = context.applicationContext as? ToeicApplication ?: return
        val container = app.container
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val restored = ReminderBootRestorer.restoreIfEnabled(
                    userPreferencesRepository = container.userPreferencesRepository,
                    reminderScheduler = container.reminderScheduler
                )
                Log.i(TAG, "Handled $action: restored=$restored")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to restore reminder schedule on $action", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderBootReceiver"
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
    }
}
