package eu.anifantakis.snoozeloo.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import eu.anifantakis.snoozeloo.core.data.database.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Upsert
    suspend fun upsertAlarm(alarm: AlarmEntity)

    @Upsert
    suspend fun upsertAlarms(alarms: List<AlarmEntity>)

    @Query("SELECT * FROM AlarmEntity")
    fun getAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM AlarmEntity WHERE id = :id")
    suspend fun getAlarm(id: AlarmId): AlarmEntity

    @Query("DELETE FROM AlarmEntity WHERE id = :id")
    fun deleteAlarm(id: AlarmId)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM AlarmEntity")
    fun deleteAllAlarms()

    @Delete
    suspend fun deleteAlarms(alarms: List<AlarmEntity>)
}