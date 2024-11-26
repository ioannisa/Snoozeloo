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
import java.time.LocalDateTime
import java.util.UUID

/**
 * Implementation of [AlarmsRepository] that manages alarm data persistence.
 * Handles the coordination of data operations and provides a clean API
 * for alarm management.
 *
 * Key responsibilities:
 * - Managing alarm CRUD operations
 * - Creating new alarms with default settings
 * - Error handling and result transformation
 * - Providing reactive data streams
 *
 * @property localDataSource Data source for local alarm storage
 */
class AlarmsRepositoryImpl(
    private val localDataSource: LocalAlarmsDataSource
) : AlarmsRepository {

    /**
     * Retrieves a reactive stream of all alarms.
     *
     * @return Flow emitting list of alarms, automatically updates on data changes
     */
    override fun getAlarms(): Flow<List<Alarm>> {
        return localDataSource.getAlarms()
    }

    /**
     * Retrieves a specific alarm by ID.
     *
     * @param id Unique identifier of the alarm
     * @return The requested alarm
     * @throws IllegalStateException if alarm not found
     */
    override suspend fun getAlarm(id: AlarmId): Alarm {
        return localDataSource.getAlarm(id = id)
    }

    /**
     * Creates or updates an alarm.
     * Transforms data source results to a standardized empty result type.
     *
     * @param alarm The alarm to save
     * @return Empty result indicating success or failure with error details
     */
    override suspend fun upsertAlarm(alarm: Alarm): EmptyDataResult<DataError> {
        val localResult = localDataSource.upsertAlarm(alarm)
        if (localResult !is DataResult.Success) {
            return localResult.asEmptyDataResult()
        }
        return localResult.asEmptyDataResult()
    }

    /**
     * Generates a new alarm instance with default settings.
     * Creates an alarm scheduled for the current time with all days selected.
     *
     * Default settings:
     * - Current time as initial time
     * - All days selected
     * - Enabled state
     * - Medium volume (0.5)
     * - Vibration enabled
     * - Default system ringtone
     *
     * @return New alarm instance with default configuration
     */
    override fun generateNewAlarm(): Alarm {
        val now = LocalDateTime.now()

        return Alarm(
            id = UUID.randomUUID().toString(),
            hour = now.hour,
            minute = now.minute,
            title = "",
            isEnabled = true,
            ringtoneTitle = "",
            ringtoneUri = null,
            volume = 0.5f,
            vibrate = true,
            isNewAlarm = true,
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
    }

    /**
     * Creates a new alarm with default settings and persists it.
     *
     * Process:
     * 1. Generates new alarm with default settings
     * 2. Persists to local storage
     * 3. Transforms result to empty result type
     *
     * @return Empty result indicating success or failure with error details
     */
    override suspend fun createNewAlarm(): EmptyDataResult<DataError> {
        val emptyAlarm = generateNewAlarm()

        val localResult = localDataSource.upsertAlarm(emptyAlarm)
        if (localResult !is DataResult.Success) {
            return localResult.asEmptyDataResult()
        }
        return localResult.asEmptyDataResult()
    }

    /**
     * Deletes an alarm by ID.
     *
     * @param id ID of the alarm to delete
     */
    override suspend fun deleteAlarm(id: AlarmId) {
        localDataSource.deleteAlarm(id)
    }
}