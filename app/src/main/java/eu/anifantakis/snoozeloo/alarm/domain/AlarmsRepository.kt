package eu.anifantakis.snoozeloo.alarm.domain

import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import eu.anifantakis.snoozeloo.core.domain.util.DataError
import eu.anifantakis.snoozeloo.core.domain.util.EmptyDataResult
import kotlinx.coroutines.flow.Flow

interface AlarmsRepository {
    fun getAlarms(): Flow<List<Alarm>>
    suspend fun getAlarm(id: AlarmId): Alarm
    suspend fun upsertAlarm(alarm: Alarm): EmptyDataResult<DataError>
    suspend fun createNewAlarm(): EmptyDataResult<DataError>
    suspend fun deleteAlarm(id: AlarmId)
}