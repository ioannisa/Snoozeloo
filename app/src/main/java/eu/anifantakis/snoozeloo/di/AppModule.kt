package eu.anifantakis.snoozeloo.di

import eu.anifantakis.snoozeloo.MainViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms.AlarmsViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmedit.AlarmEditViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmDismissActivityViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting.AlarmToneSettingViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    viewModelOf(::MainViewModel)
    viewModelOf(::AlarmDismissActivityViewModel)
    viewModelOf(::AlarmsViewModel)
    viewModelOf(::AlarmEditViewModel)
    viewModelOf(::AlarmToneSettingViewModel)

}