package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_preferences")
data class WaterPreference(
    @PrimaryKey val id: Int = 1, // Singleton row for settings
    val dailyGoalMl: Int = 2000,
    val isNotificationEnabled: Boolean = true,
    val notificationIntervalMinutes: Int = 120 // e.g., every 2 hours
)
