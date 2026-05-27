package com.example.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import java.util.Calendar

object WaterNotificationHelper {
    private const val CHANNEL_ID = "water_reminders_channel"
    private const val NOTIFICATION_ID = 4012
    const val ALARM_REQUEST_CODE = 9021

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Water Reminder Alerts"
            val descriptionText = "Notifications to remind you to drink water and stay hydrated."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleReminderAlarm(context: Context, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, intervalMinutes)
        }.timeInMillis

        val intervalMs = intervalMinutes * 60 * 1000L
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intervalMs,
            pendingIntent
        )
    }

    fun cancelReminderAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showWaterReminderNotification(context: Context, dailyGoalMl: Int, currentIntakeMl: Int) {
        val titles = listOf(
            "Time for a Sip! 💧",
            "Hydration Status Check! 🌊",
            "Take a Water Break! 🧊",
            "Drink Up! 💦",
            "Stay Healthy, Stay Hydrated! 🌱"
        )
        val title = titles.random()

        val progressPct = if (dailyGoalMl > 0) (currentIntakeMl * 100 / dailyGoalMl) else 0
        val motivationalQuotes = listOf(
            "You are currently at $currentIntakeMl ml out of your $dailyGoalMl ml daily goal ($progressPct%). Keep it up!",
            "Your body is over 60% water. Keep it happy and healthy with another glass!",
            "Drinking water improves focus, mood, and skin health. Let's hydrate!",
            "A glass of water right now is a great gift to your future self!",
            "Every sip counts! Let's get closer to your target of $dailyGoalMl ml."
        )
        val body = motivationalQuotes.random()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using small icon parameter. We will use the system dialogue info drawing for fallback compatibility
        // since the launcher icon can be adaptive XML or raster and not standard resource on some variants.
        // It provides a perfect crisp visual representation of information/reminders.
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Suppress runtime checks
        }
    }
}
