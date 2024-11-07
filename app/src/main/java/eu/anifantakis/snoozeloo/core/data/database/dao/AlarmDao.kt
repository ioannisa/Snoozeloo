package eu.anifantakis.snoozeloo.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import eu.anifantakis.snoozeloo.core.data.database.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Upsert
    suspend fun upsertAlarm(movie: AlarmEntity)

    @Query("SELECT * FROM AlarmEntity ORDER BY id ASC")
    fun getAlarms(): Flow<List<AlarmEntity>>

    @Query("DELETE FROM AlarmEntity WHERE id = :id")
    fun deleteAlarm(id: Int)

    @Delete
    suspend fun deleteAlarm(movie: AlarmEntity)

    @Query("DELETE FROM AlarmEntity")
    fun deleteAllAlarms()

    @Delete
    suspend fun deleteAlarms(movies: List<AlarmEntity>)
}