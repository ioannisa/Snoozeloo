package eu.anifantakis.snoozeloo.alarm.data

import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import eu.anifantakis.snoozeloo.alarm.domain.datasource.LocalAlarmsDataSource
import eu.anifantakis.snoozeloo.core.domain.util.DataError
import eu.anifantakis.snoozeloo.core.domain.util.DataResult
import eu.anifantakis.snoozeloo.core.domain.util.EmptyDataResult
import eu.anifantakis.snoozeloo.core.domain.util.asEmptyDataResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AlarmsRepositoryImpl(
    private val localDataSource: LocalAlarmsDataSource
) : AlarmsRepository {

    override fun getAlarms(): Flow<List<Alarm>> {
        return localDataSource.getAlarms()
    }

    override suspend fun getAlarm(id: AlarmId): Alarm {
        return localDataSource.getAlarm(id = id)
    }

    override suspend fun upsertAlarm(alarm: Alarm): EmptyDataResult<DataError> {
        val localResult = localDataSource.upsertAlarm(alarm)
        if (localResult !is DataResult.Success) {
            return localResult.asEmptyDataResult()
        }
        return localResult.asEmptyDataResult()
    }

    override suspend fun createNewAlarm(): EmptyDataResult<DataError> {
        val emptyAlarm = Alarm(
            id = UUID.randomUUID().toString(),
            hour = 0,
            minute = 0,
            title = "",
            isEnabled = false,
            ringtoneTitle = "",
            ringtoneUri = null,
            volume = 0.5f,
            vibrate = true,
            temporary = true,
            selectedDays = DaysOfWeek(
                mo = true,
                tu = true,
                we = true,
                th = true,
                fr = true,
                sa = true,
                su = true
            )
        )

        val localResult = localDataSource.upsertAlarm(emptyAlarm)
        if (localResult !is DataResult.Success) {
            return localResult.asEmptyDataResult()
        }
        return localResult.asEmptyDataResult()
    }

    override suspend fun deleteAlarm(id: AlarmId) {
        localDataSource.deleteAlarm(id)
    }
}