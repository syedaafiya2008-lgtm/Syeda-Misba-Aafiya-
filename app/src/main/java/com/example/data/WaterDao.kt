package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Delete
    suspend fun deleteLog(log: WaterLog)

    @Query("SELECT * FROM water_preferences WHERE id = 1")
    fun getPreferenceFlow(): Flow<WaterPreference?>

    @Query("SELECT * FROM water_preferences WHERE id = 1")
    suspend fun getPreference(): WaterPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: WaterPreference)
}
