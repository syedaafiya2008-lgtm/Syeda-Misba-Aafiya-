package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val database = WaterDatabase.getDatabase(context)
        val repository = WaterRepository(database.waterDao())

        CoroutineScope(Dispatchers.IO).launch {
            val pref = repository.getPreference()
            if (!pref.isNotificationEnabled) {
                WaterNotificationHelper.cancelReminderAlarm(context)
                return@launch
            }

            // Compute current start/end day timestamps
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfDay = cal.timeInMillis

            // Fetch current daily progress
            val logsForDay = repository.getLogsForDay(startOfDay, endOfDay).first()
            val totalToday = logsForDay.sumOf { it.amountMl }

            WaterNotificationHelper.showWaterReminderNotification(
                context = context,
                dailyGoalMl = pref.dailyGoalMl,
                currentIntakeMl = totalToday
            )
        }
    }
}
