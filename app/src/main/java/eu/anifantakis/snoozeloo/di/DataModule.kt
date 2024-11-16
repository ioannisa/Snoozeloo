package eu.anifantakis.snoozeloo.di

import eu.anifantakis.lib.securepersist.PersistManager
import eu.anifantakis.snoozeloo.alarm.data.AlarmsRepositoryImpl
import eu.anifantakis.snoozeloo.alarm.data.RingtoneRepositoryImpl
import eu.anifantakis.snoozeloo.alarm.data.datasource.LocalAlarmsDataSourceImpl
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import eu.anifantakis.snoozeloo.alarm.domain.datasource.LocalAlarmsDataSource
import eu.anifantakis.snoozeloo.core.data.AlarmSchedulerImpl
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {

    singleOf(::AlarmsRepositoryImpl).bind<AlarmsRepository>()
    singleOf(::LocalAlarmsDataSourceImpl).bind<LocalAlarmsDataSource>()
    singleOf(::RingtoneRepositoryImpl).bind<RingtoneRepository>()
    singleOf(::AlarmSchedulerImpl).bind<AlarmScheduler>()

    single<PersistManager> {
        PersistManager(androidContext())
    }

}