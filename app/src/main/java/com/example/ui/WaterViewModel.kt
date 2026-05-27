package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WaterLog
import com.example.data.WaterPreference
import com.example.data.WaterRepository
import com.example.notification.WaterNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class WaterViewModel(
    private val application: Application,
    private val repository: WaterRepository
) : AndroidViewModel(application) {

    private fun getTodayStartEnd(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    private val _startEnd = MutableStateFlow(getTodayStartEnd())

    fun refreshTodayBounds() {
        _startEnd.value = getTodayStartEnd()
    }

    val preferences: StateFlow<WaterPreference> = repository.getPreferenceFlow()
        .combine(_startEnd) { pref, _ ->
            pref ?: WaterPreference()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterPreference()
        )

    val logsToday: StateFlow<List<WaterLog>> = combine(repository.getAllLogs(), _startEnd) { allLogs, bounds ->
        allLogs.filter { it.timestamp in bounds.first..bounds.second }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun logWater(amountMl: Int, beverageType: String) {
        viewModelScope.launch {
            repository.insertLog(WaterLog(amountMl = amountMl, beverageType = beverageType))
            refreshTodayBounds()
        }
    }

    fun deleteLog(log: WaterLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
            refreshTodayBounds()
        }
    }

    fun updateDailyGoal(goalMl: Int) {
        viewModelScope.launch {
            val currentPref = repository.getPreference()
            val updated = currentPref.copy(dailyGoalMl = goalMl)
            repository.insertPreference(updated)
            
            if (updated.isNotificationEnabled) {
                WaterNotificationHelper.scheduleReminderAlarm(application, updated.notificationIntervalMinutes)
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val currentPref = repository.getPreference()
            val updated = currentPref.copy(isNotificationEnabled = enabled)
            repository.insertPreference(updated)

            if (enabled) {
                WaterNotificationHelper.scheduleReminderAlarm(application, updated.notificationIntervalMinutes)
            } else {
                WaterNotificationHelper.cancelReminderAlarm(application)
            }
        }
    }

    fun updateNotificationInterval(minutes: Int) {
        viewModelScope.launch {
            val currentPref = repository.getPreference()
            val updated = currentPref.copy(notificationIntervalMinutes = minutes)
            repository.insertPreference(updated)

            if (updated.isNotificationEnabled) {
                WaterNotificationHelper.cancelReminderAlarm(application)
                WaterNotificationHelper.scheduleReminderAlarm(application, minutes)
            }
        }
    }

    fun triggerInstantNotification() {
        viewModelScope.launch {
            val pref = repository.getPreference()
            val todayLogs = logsToday.value
            val totalToday = todayLogs.sumOf { it.amountMl }
            WaterNotificationHelper.showWaterReminderNotification(
                context = application,
                dailyGoalMl = pref.dailyGoalMl,
                currentIntakeMl = totalToday
            )
        }
    }

    class Factory(
        private val application: Application,
        private val repository: WaterRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WaterViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
