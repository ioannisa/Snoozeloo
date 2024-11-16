package eu.anifantakis.snoozeloo.di

import androidx.room.Room
import eu.anifantakis.snoozeloo.core.data.database.AlarmDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dbModule = module {

    single {
        Room.databaseBuilder(
            androidApplication(),
            AlarmDatabase::class.java,
            "app_database.db"
        ).build()
    }

    single { get<AlarmDatabase>().alarmDao }

}