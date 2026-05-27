package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Int,
    val beverageType: String, // e.g., "water", "coffee", "tea", "juice", "soda"
    val timestamp: Long = System.currentTimeMillis()
)
