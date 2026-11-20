package com.toeicflashcard.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.toeicflashcard.MainActivity
import com.toeicflashcard.R

object ReminderNotificationHelper {

    const val CHANNEL_ID = "toeic_daily_reminder_channel"
    const val NOTIFICATION_ID = 1001

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở ôn tập TOEIC",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo nhắc nhở ôn tập từ vựng TOEIC hàng ngày và duy trì chuỗi Streak."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showDailyReminderNotification(
        context: Context,
        dueCount: Int,
        streakDays: Int,
        remainingGoal: Int
    ) {
        if (!hasNotificationPermission(context)) return
        ensureNotificationChannel(context)

        val message = if (dueCount > 0) {
            "Bạn có $dueCount từ TOEIC cần ôn tập hôm nay để giữ chuỗi $streakDays ngày Streak!"
        } else {
            "Bạn còn $remainingGoal từ nữa để hoàn thành mục tiêu hôm nay và giữ chuỗi $streakDays ngày Streak!"
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle("Đến giờ ôn tập TOEIC rồi!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission may be revoked concurrently by the system
        }
    }
}
