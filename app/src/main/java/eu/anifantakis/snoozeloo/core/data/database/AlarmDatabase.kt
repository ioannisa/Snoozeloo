package eu.anifantakis.snoozeloo.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import eu.anifantakis.snoozeloo.core.data.database.dao.AlarmDao
import eu.anifantakis.snoozeloo.core.data.database.entity.AlarmEntity

@Database(
    entities = [AlarmEntity::class],
    version = 1
)
abstract class AlarmDatabase: RoomDatabase() {

    abstract val alarmDao: AlarmDao

}