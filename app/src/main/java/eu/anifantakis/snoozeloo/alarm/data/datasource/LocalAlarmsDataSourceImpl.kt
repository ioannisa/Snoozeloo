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

/**
 * Implementation of [LocalAlarmsDataSource] that manages alarm data persistence using SQLite.
 * Handles database operations for alarms including CRUD operations and error handling.
 *
 * @property alarmDao Data Access Object for alarm database operations
 */
class LocalAlarmsDataSourceImpl(
    private val alarmDao: AlarmDao
): LocalAlarmsDataSource {

    /**
     * Retrieves all alarms as a Flow.
     * Maps database entities to domain models.
     *
     * @return Flow emitting list of alarms, automatically updates when data changes
     */
    override fun getAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAlarms()
            .map { alarmEntities ->
                alarmEntities.map { it.toAlarm() }
            }
    }

    /**
     * Retrieves a single alarm by its ID.
     *
     * @param id Unique identifier of the alarm
     * @return Alarm domain model
     * @throws IllegalStateException if alarm not found
     */
    override suspend fun getAlarm(id: AlarmId): Alarm {
        return alarmDao.getAlarm(id = id)
            .toAlarm()
    }

    /**
     * Creates or updates an alarm in the database.
     * Handles disk space errors gracefully.
     *
     * @param alarm The alarm to save
     * @return Success with alarm ID or Failure with error details
     */
    override suspend fun upsertAlarm(alarm: Alarm): DataResult<AlarmId, DataError.Local> {
        return try {
            val entity = alarm.toEntity()
            alarmDao.upsertAlarm(entity)
            DataResult.Success(entity.id)
        } catch (e: SQLiteFullException) {
            DataResult.Failure(DataError.Local.DISK_FULL)
        }
    }

    /**
     * Creates or updates multiple alarms in a single transaction.
     * Handles disk space errors gracefully.
     *
     * @param alarms List of alarms to save
     * @return Success with list of alarm IDs or Failure with error details
     */
    override suspend fun upsertAlarms(alarms: List<Alarm>): DataResult<List<AlarmId>, DataError.Local> {
        return try {
            val entities = alarms.map { it.toEntity() }
            alarmDao.upsertAlarms(entities)
            DataResult.Success(entities.map { it.id })
        } catch (e: SQLiteFullException) {
            DataResult.Failure(DataError.Local.DISK_FULL)
        }
    }

    /**
     * Deletes a specific alarm from the database.
     *
     * @param id ID of the alarm to delete
     */
    override suspend fun deleteAlarm(id: AlarmId) {
        alarmDao.deleteAlarm(id)
    }

    /**
     * Deletes all alarms from the database.
     * Use with caution as this operation cannot be undone.
     */
    override suspend fun deleteAllAlarms() {
        alarmDao.deleteAllAlarms()
    }
}