package eu.anifantakis.snoozeloo.alarm.domain.datasource

import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.core.domain.util.DataError
import eu.anifantakis.snoozeloo.core.domain.util.DataResult
import kotlinx.coroutines.flow.Flow

typealias AlarmId = String

interface LocalAlarmsDataSource {

    fun getAlarms(): Flow<List<Alarm>>
    suspend fun getAlarm(id: AlarmId): Alarm
    suspend fun upsertAlarm(alarm: Alarm): DataResult<AlarmId, DataError.Local>
    suspend fun upsertAlarms(alarms: List<Alarm>): DataResult<List<AlarmId>, DataError.Local>
    suspend fun deleteAlarm(id: AlarmId)
    suspend fun deleteAllAlarms()

}