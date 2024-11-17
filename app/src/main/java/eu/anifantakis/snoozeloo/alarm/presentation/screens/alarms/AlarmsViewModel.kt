package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Immutable
data class AlarmsScreenState(
    val alarms: List<AlarmUiState> = emptyList(),
    val selectedAlarm: Alarm? = null,
    val use24HourFormat: Boolean = false
)

sealed interface AlarmsScreenAction {
    data object AddAlarmsScreen : AlarmsScreenAction
    data class EnableAlarmsScreen(val alarm: Alarm, val enabled: Boolean) : AlarmsScreenAction
    data class ChangeAlarmsScreenDays(val alarm: Alarm, val selectedDays: DaysOfWeek) : AlarmsScreenAction
    data class DeleteAlarmsScreen(val alarm: Alarm) : AlarmsScreenAction
    data class SelectAlarmsScreen(val alarm: Alarm): AlarmsScreenAction
    data class ChangeTimeFormat(val use24HourFormat: Boolean): AlarmsScreenAction
    data object ShowDaysValidationError : AlarmsScreenAction
}

sealed interface AlarmsScreenEvent {
    data class OnSelectAlarms(val alarmId: String): AlarmsScreenEvent
    data class OnShowSnackBar(val message: String): AlarmsScreenEvent
}

class AlarmsViewModel(
    private val repository: AlarmsRepository,
    private val persistManager: PersistManager,
    private val alarmScheduler: AlarmScheduler
): ViewModel() {

    var state by mutableStateOf(AlarmsScreenState())
        private set

    private val eventChannel = Channel<AlarmsScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    private var minuteTickerJob: Job? = null

    private var isInitialLoad = true
    private var currentAlarmCount = 0

    init {
        loadAlarms()
        startMinuteTicker()
    }

    private fun loadAlarms() {
        viewModelScope.launch {
            state = state.copy(use24HourFormat = persistManager.dataStorePrefs.get(key = "use24HourFormat", defaultValue = false, encrypted = false))

            repository.getAlarms()
                .collect { alarms ->
                    // Only check for new alarms if it's not the initial load
                    if (!isInitialLoad && alarms.size > currentAlarmCount) {
                        // Get the most recently added alarm (it will be the one that's not in our current state)
                        val newAlarm = alarms.firstOrNull { alarm ->
                            state.alarms.none { it.alarm.id == alarm.id }
                        }

                        // If we found the new alarm, select it to open the editor
                        newAlarm?.let {
                            onAction(AlarmsScreenAction.SelectAlarmsScreen(it))
                        }
                    }

                    currentAlarmCount = alarms.size
                    isInitialLoad = false

                    state = state.copy(
                        alarms = alarms.map { alarm ->
                            AlarmUiState(
                                alarm = alarm,
                                timeUntilNextAlarm =
                                calculateTimeUntilNextAlarm(
                                    alarm.hour,
                                    alarm.minute,
                                    alarm.selectedDays
                                ).formatTimeUntil()
                            )
                        }
                    )
                }
        }
    }

    private fun startMinuteTicker() {
        minuteTickerJob?.cancel()
        minuteTickerJob = viewModelScope.launch {
            ClockUtils.createMinuteTickerFlow()
                .collect {
                    // Update time until next alarm for all alarms
                    state = state.copy(
                        alarms = state.alarms.map { uiState ->
                            uiState.copy(
                                timeUntilNextAlarm =
                                    calculateTimeUntilNextAlarm(
                                        uiState.alarm.hour,
                                        uiState.alarm.minute,
                                        uiState.alarm.selectedDays
                                    ).formatTimeUntil()
                            )
                        }
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        minuteTickerJob?.cancel()
    }

    fun onAction(action: AlarmsScreenAction) {
        when (action) {
            is AlarmsScreenAction.AddAlarmsScreen -> {
                viewModelScope.launch {
                    repository.createNewAlarm()
                }
            }
            is AlarmsScreenAction.EnableAlarmsScreen -> {
                println("SWITCHING")

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
                viewModelScope.launch {
                    state = state.copy(
                        selectedAlarm = state.alarms.firstOrNull{ it.alarm.id == action.alarm.id }?.alarm
                    )

                    eventChannel.send(AlarmsScreenEvent.OnSelectAlarms(action.alarm.id))
                }
            }
            is AlarmsScreenAction.ChangeTimeFormat -> {
                state = state.copy(use24HourFormat = action.use24HourFormat)
                viewModelScope.launch(Dispatchers.IO) {
                    persistManager.dataStorePrefs.put(
                        key = "use24HourFormat",
                        value =  action.use24HourFormat,
                        encrypted = false
                    )
                }
            }
        }
    }
}