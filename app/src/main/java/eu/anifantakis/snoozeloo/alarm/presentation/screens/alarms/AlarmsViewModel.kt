package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.lib.securepersist.PersistManager
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import eu.anifantakis.snoozeloo.core.domain.util.ClockUtils
import eu.anifantakis.snoozeloo.core.domain.util.calculateTimeUntilNextAlarm
import eu.anifantakis.snoozeloo.core.domain.util.formatTimeUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Immutable
data class AlarmsScreenState(
    val alarms: List<AlarmUiState> = emptyList(),
    val selectedAlarm: Alarm? = null,
    val use24HourFormat: Boolean = false
)

sealed interface AlarmsScreenAction {
    data object AddNewAlarm : AlarmsScreenAction
    data class EnableAlarmsScreen(val alarm: Alarm, val enabled: Boolean) : AlarmsScreenAction
    data class ChangeAlarmsScreenDays(val alarm: Alarm, val selectedDays: DaysOfWeek) : AlarmsScreenAction
    data class DeleteAlarmsScreen(val alarm: Alarm) : AlarmsScreenAction
    data class SelectAlarmsScreen(val alarm: Alarm): AlarmsScreenAction
    data class ChangeTimeFormat(val use24HourFormat: Boolean): AlarmsScreenAction
    data object ShowDaysValidationError : AlarmsScreenAction
}

sealed interface AlarmsScreenEvent {
    data class OnOpenAlarmEditorFor(val alarm: Alarm): AlarmsScreenEvent
    data class OnShowSnackBar(val message: String): AlarmsScreenEvent
}

class AlarmsViewModel(
    private val repository: AlarmsRepository,
    private val alarmScheduler: AlarmScheduler,
    persistManager: PersistManager,
): ViewModel() {

    var state by mutableStateOf(AlarmsScreenState())
        private set

    private var use24HourFormat by persistManager.dataStorePrefs.preference(false)

    private val eventChannel = Channel<AlarmsScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        initializeViewModel()
    }

    // React on changes in selected alarm by sending an event to UI to open the Alarm editor for that alarm
    private val selectedAlarmEditorJob = snapshotFlow { state.selectedAlarm }
        .filterNotNull()
        .map { alarm ->
            eventChannel.send(AlarmsScreenEvent.OnOpenAlarmEditorFor(alarm))
        }
        .launchIn(viewModelScope)

    private fun initializeViewModel() {
        viewModelScope.launch {
            state = state.copy(use24HourFormat = use24HourFormat)

            // Combine the alarm data flow with the minute ticker
            combine(
                repository.getAlarms(),
                ClockUtils.createMinuteTickerFlow()
            ) { alarms, _ ->
                // The second parameter (_) is ignored as we only need it to trigger updates
                alarms.map { alarm ->
                    AlarmUiState(
                        alarm = alarm,
                        timeUntilNextAlarm = calculateTimeUntilNextAlarm(
                            alarm.hour,
                            alarm.minute,
                            alarm.selectedDays
                        ).formatTimeUntil()
                    )
                }
            }.collect { alarmUiStates ->
                state = state.copy(alarms = alarmUiStates)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        selectedAlarmEditorJob.cancel()
    }

    fun onAction(action: AlarmsScreenAction) {
        when (action) {
            is AlarmsScreenAction.AddNewAlarm -> {
                viewModelScope.launch {
                    val newAlarm = repository.generateNewAlarm()
                    eventChannel.send(AlarmsScreenEvent.OnOpenAlarmEditorFor(newAlarm))
                }
            }

            is AlarmsScreenAction.EnableAlarmsScreen -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val updatedAlarm = action.alarm.copy(isEnabled = action.enabled)
                    repository.upsertAlarm(updatedAlarm)

                    if (action.enabled) {
                        println("ENABLED")
                        alarmScheduler.schedule(updatedAlarm)
                    } else {
                        println("DISABLED")
                        alarmScheduler.cancel(updatedAlarm)
                    }
                }
            }

            is AlarmsScreenAction.ChangeAlarmsScreenDays -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val updatedAlarm = action.alarm.copy(selectedDays = action.selectedDays)
                    repository.upsertAlarm(updatedAlarm)
                }
            }

            is AlarmsScreenAction.DeleteAlarmsScreen -> {
                viewModelScope.launch(Dispatchers.IO) {
                    alarmScheduler.cancel(action.alarm)
                    repository.deleteAlarm(action.alarm.id)
                }
            }

            is AlarmsScreenAction.ShowDaysValidationError -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmsScreenEvent.OnShowSnackBar("All alarms need at least one active day"))
                }
            }

            is AlarmsScreenAction.SelectAlarmsScreen -> {
                state = state.copy(
                    selectedAlarm = state.alarms.first { it.alarm.id == action.alarm.id }.alarm
                )
            }

            is AlarmsScreenAction.ChangeTimeFormat -> {
                viewModelScope.launch(Dispatchers.IO) {
                    use24HourFormat = action.use24HourFormat
                    state = state.copy(use24HourFormat = action.use24HourFormat)
                }
            }
        }
    }
}