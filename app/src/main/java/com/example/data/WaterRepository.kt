package com.example.data

import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterDao: WaterDao) {
    fun getAllLogs(): Flow<List<WaterLog>> = waterDao.getAllLogs()

    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WaterLog>> {
        return waterDao.getLogsForDay(startOfDay, endOfDay)
    }

    suspend fun insertLog(log: WaterLog) {
        waterDao.insertLog(log)
    }

    suspend fun deleteLog(log: WaterLog) {
        waterDao.deleteLog(log)
    }

    fun getPreferenceFlow(): Flow<WaterPreference?> = waterDao.getPreferenceFlow()

    suspend fun getPreference(): WaterPreference {
        return waterDao.getPreference() ?: WaterPreference().also {
            waterDao.insertPreference(it)
        }
    }

    suspend fun insertPreference(preference: WaterPreference) {
        waterDao.insertPreference(preference)
    }
}
