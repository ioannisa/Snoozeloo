package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Immutable
data class AlarmsState(
    val alarms: List<Alarm> = emptyList(),
    val selectedAlarm: Alarm? = null,
)

sealed interface AlarmUiEvent {
    data object OnAddAlarm : AlarmUiEvent
    data class OnAlarmEnabled(val alarm: Alarm, val enabled: Boolean) : AlarmUiEvent
    data class OnAlarmDaysChanged(val alarm: Alarm, val selectedDays: DaysOfWeek) : AlarmUiEvent
    data class OnAlarmDeleted(val alarm: Alarm) : AlarmUiEvent
    data class OnOpenAlarmEditor(val alarmId: String): AlarmUiEvent
}

class AlarmViewModel(
    private val repository: AlarmsRepository
): ViewModel() {

    var state by mutableStateOf(AlarmsState())
        private set

    private val eventChannel = Channel<AlarmUiEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        loadAlarms()
    }

    private fun loadAlarms() {
        viewModelScope.launch {
            repository.getAlarms()
                .collect { alarms ->
                    state = state.copy(alarms = alarms)
                }
        }
    }

    fun onEvent(event: AlarmUiEvent) {
        when (event) {
            is AlarmUiEvent.OnAddAlarm -> {
                viewModelScope.launch {
                    repository.createNewAlarm()
                }
            }
            is AlarmUiEvent.OnAlarmEnabled -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val updatedAlarm = event.alarm.copy(isEnabled = event.enabled)
                    repository.upsertAlarm(updatedAlarm)
                }
            }
            is AlarmUiEvent.OnAlarmDaysChanged -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val updatedAlarm = event.alarm.copy(selectedDays = event.selectedDays)
                    repository.upsertAlarm(updatedAlarm)
                }
            }
            is AlarmUiEvent.OnAlarmDeleted -> {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.deleteAlarm(event.alarm.id)
                }
            }
            is AlarmUiEvent.OnOpenAlarmEditor -> {
                viewModelScope.launch {
                    eventChannel.send(event)
                    state = state.copy(selectedAlarm = state.alarms.firstOrNull { it.id == event.alarmId })
                }
            }
        }
    }
}