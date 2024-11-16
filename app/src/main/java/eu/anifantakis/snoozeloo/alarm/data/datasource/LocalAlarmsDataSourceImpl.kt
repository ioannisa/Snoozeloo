package eu.anifantakis.snoozeloo.alarm.data.datasource

import android.database.sqlite.SQLiteFullException
import eu.anifantakis.snoozeloo.alarm.data.mapper.toAlarm
import eu.anifantakis.snoozeloo.alarm.data.mapper.toEntity
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import eu.anifantakis.snoozeloo.alarm.domain.datasource.LocalAlarmsDataSource
import eu.anifantakis.snoozeloo.core.data.database.dao.AlarmDao
import eu.anifantakis.snoozeloo.core.domain.util.DataError
import eu.anifantakis.snoozeloo.core.domain.util.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalAlarmsDataSourceImpl(
    private val alarmDao: AlarmDao
): LocalAlarmsDataSource {
    override fun getAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAlarms()
            .map { alarmEntities ->
                alarmEntities.map { it.toAlarm() }
            }
    }

    override suspend fun getAlarm(id: AlarmId): Alarm {
        return alarmDao.getAlarm(id = id)
            .toAlarm()
    }

    override suspend fun upsertAlarm(alarm: Alarm): DataResult<AlarmId, DataError.Local> {
        return try {
            val entity = alarm.toEntity()
            alarmDao.upsertAlarm(entity)
            DataResult.Success(entity.id)
        } catch (e: SQLiteFullException) {
            DataResult.Failure(DataError.Local.DISK_FULL)
        }
    }

    override suspend fun upsertAlarms(alarms: List<Alarm>): DataResult<List<AlarmId>, DataError.Local> {
        return try {
            val entities = alarms.map { it.toEntity() }
            alarmDao.upsertAlarms(entities)
            DataResult.Success(entities.map { it.id })
        } catch (e: SQLiteFullException) {
            DataResult.Failure(DataError.Local.DISK_FULL)
        }
    }

    override suspend fun deleteAlarm(id: AlarmId) {
        alarmDao.deleteAlarm(id)
    }

    override suspend fun deleteAllAlarms() {
        alarmDao.deleteAllAlarms()
    }
}