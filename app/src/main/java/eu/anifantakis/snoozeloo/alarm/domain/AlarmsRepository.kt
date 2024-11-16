package eu.anifantakis.snoozeloo.alarm.domain

import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import eu.anifantakis.snoozeloo.core.domain.util.DataError
import eu.anifantakis.snoozeloo.core.domain.util.EmptyDataResult
import kotlinx.coroutines.flow.Flow

interface AlarmsRepository {
    fun getAlarms(): Flow<List<Alarm>>
    suspend fun getAlarm(id: AlarmId): Alarm
    fun observeEditedAlarm(): Flow<Alarm?>
    suspend fun upsertAlarm(alarm: Alarm): EmptyDataResult<DataError>
    suspend fun createNewAlarm(): EmptyDataResult<DataError>
    fun generateNewAlarm(): Alarm
    suspend fun deleteAlarm(id: AlarmId)

    /** Updates the in-memory alarm without persisting */
    fun updateEditedAlarm(alarm: Alarm)

    /** Persists the current edited alarm */
    suspend fun saveEditedAlarm(): EmptyDataResult<DataError>

    fun cleanupCurrentlyEditedAlarm()
}