package eu.anifantakis.snoozeloo.di

import eu.anifantakis.snoozeloo.alarm.data.AlarmsRepositoryImpl
import eu.anifantakis.snoozeloo.alarm.data.RingtoneRepositoryImpl
import eu.anifantakis.snoozeloo.alarm.data.datasource.LocalAlarmsDataSourceImpl
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import eu.anifantakis.snoozeloo.alarm.domain.datasource.LocalAlarmsDataSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {

    singleOf(::AlarmsRepositoryImpl).bind<AlarmsRepository>()
    singleOf(::LocalAlarmsDataSourceImpl).bind<LocalAlarmsDataSource>()
    singleOf(::RingtoneRepositoryImpl).bind<RingtoneRepository>()

}