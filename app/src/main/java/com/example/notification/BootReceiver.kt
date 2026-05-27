package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val database = WaterDatabase.getDatabase(context)
            val repository = WaterRepository(database.waterDao())

            CoroutineScope(Dispatchers.IO).launch {
                val pref = repository.getPreference()
                if (pref.isNotificationEnabled) {
                    WaterNotificationHelper.scheduleReminderAlarm(context, pref.notificationIntervalMinutes)
                }
            }
        }
    }
}
