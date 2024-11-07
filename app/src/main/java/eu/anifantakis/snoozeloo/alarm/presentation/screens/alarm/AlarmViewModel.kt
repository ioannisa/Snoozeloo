package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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

        snapshotFlow { state.selectedAlarm }
            .map { alarm ->
                eventChannel.send(AlarmUiEvent.OnOpenAlarmEditor(alarm?.id ?: ""))
            }
            .launchIn(viewModelScope)
    }


    private fun loadAlarms() {
        viewModelScope.launch {
            repository.getAlarms()
                .collect{

                    Log.d("ALARMS_IS", "${it}")

                    state = state.copy(alarms = it)
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
                // Immediately update the repository with the new days
                viewModelScope.launch(Dispatchers.IO) {
                    val updatedAlarm = event.alarm.copy(selectedDays = event.selectedDays)
                    repository.upsertAlarm(updatedAlarm)
                    println("Days updated in repository: ${event.selectedDays}")
                }
            }
            is AlarmUiEvent.OnAlarmDeleted -> {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.deleteAlarm(event.alarm.id)
                }
            }
            is AlarmUiEvent.OnOpenAlarmEditor -> {
                val alarm = state.alarms.firstOrNull { it.id == event.alarmId }
                state = state.copy(selectedAlarm = alarm)
            }
        }
    }
}