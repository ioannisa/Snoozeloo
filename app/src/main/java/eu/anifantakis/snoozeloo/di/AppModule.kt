package eu.anifantakis.snoozeloo.di

import eu.anifantakis.snoozeloo.MainViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm.AlarmViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmedit.AlarmEditViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting.RingtoneSettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    viewModelOf(::MainViewModel)
    viewModelOf(::AlarmViewModel)
    viewModelOf(::AlarmEditViewModel)
    viewModelOf(::RingtoneSettingsViewModel)

}