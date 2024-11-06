package eu.anifantakis.snoozeloo.core.presentation.designsystem.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AlarmState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.Meridiem

data class AlarmsState(
    val alarms: List<AlarmState> = emptyList()
)

sealed class AlarmUiEvent {
    data object OnAddAlarm : AlarmUiEvent()
    data class OnAlarmEnabled(val alarm: AlarmState, val enabled: Boolean) : AlarmUiEvent()
    data class OnAlarmDaysChanged(val alarm: AlarmState, val selectedDays: Map<String, Boolean>) : AlarmUiEvent()
}

class AlarmViewModel: ViewModel() {

    var state by mutableStateOf(AlarmsState())
        private set

    fun onEvent(event: AlarmUiEvent) {
        when (event) {
            is AlarmUiEvent.OnAddAlarm -> {
                val newAlarm = AlarmState(
                    time = "7:00",
                    meridiem = Meridiem.AM,
                    isEnabled = true,
                    selectedDays = mapOf(
                        "mo" to false,
                        "tu" to false,
                        "we" to false,
                        "th" to false,
                        "fr" to false,
                        "sa" to false,
                        "su" to false
                    ),
                    timeUntilAlarm = "Alarm in 8 hours",
                    suggestedSleepTime = "11:00"
                )
                state = state.copy(
                    alarms = state.alarms + newAlarm
                )
            }
            is AlarmUiEvent.OnAlarmEnabled -> {
                state = state.copy(
                    alarms = state.alarms.map { alarm ->
                        if (alarm == event.alarm) {
                            alarm.copy(isEnabled = event.enabled)
                        } else {
                            alarm
                        }
                    }
                )
            }
            is AlarmUiEvent.OnAlarmDaysChanged -> {
                state = state.copy(
                    alarms = state.alarms.map { alarm ->
                        if (alarm == event.alarm) {
                            alarm.copy(selectedDays = event.selectedDays)
                        } else {
                            alarm
                        }
                    }
                )
            }
        }
    }
}