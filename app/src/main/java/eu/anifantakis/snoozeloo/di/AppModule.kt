package eu.anifantakis.snoozeloo.di

import eu.anifantakis.snoozeloo.MainViewModel
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms.AlarmsViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor.AlarmEditViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmDismissActivityViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.alarmtonesetting.AlarmToneSettingViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    viewModelOf(::MainViewModel)
    viewModelOf(::AlarmDismissActivityViewModel)
    viewModelOf(::AlarmsViewModel)

    viewModel { (alarm: Alarm) ->
        AlarmEditViewModel(
            alarm = alarm,
            repository = get(),
            alarmScheduler = get()
        )
    }

    viewModelOf(::AlarmToneSettingViewModel)

}