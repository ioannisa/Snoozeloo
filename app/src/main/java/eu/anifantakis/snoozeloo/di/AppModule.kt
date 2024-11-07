package eu.anifantakis.snoozeloo.di

import eu.anifantakis.snoozeloo.MainViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmmaster.AlarmViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    viewModelOf(::MainViewModel)
    viewModelOf(::AlarmViewModel)

}