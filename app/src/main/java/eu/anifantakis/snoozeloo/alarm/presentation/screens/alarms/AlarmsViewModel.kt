package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.lib.securepersist.PersistManager
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import eu.anifantakis.snoozeloo.core.domain.util.ClockUtils
import eu.anifantakis.snoozeloo.core.domain.util.calculateTimeUntilNextAlarm
import eu.anifantakis.snoozeloo.core.domain.util.formatTimeUntil
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the alarms screen.
 *
 * @property alarms List of alarm UI states to be displayed
 * @property selectedAlarm Currently selected alarm for editing, if any
 * @property use24HourFormat Whether to use 24-hour time format
 */
@Immutable
data class AlarmsScreenState(
    val alarms: List<AlarmUiState> = emptyList(),
    val selectedAlarm: Alarm? = null,
    val use24HourFormat: Boolean = false
)

/**
 * Sealed interface defining all possible actions that can be performed on the alarms screen.
 * Each action represents a unique user interaction or system event that requires processing.
 */
sealed interface AlarmsScreenAction {
    data object AddNewAlarm : AlarmsScreenAction
    data class EnableAlarmsScreen(val alarm: Alarm, val enabled: Boolean) : AlarmsScreenAction
    data class ChangeAlarmsScreenDays(val alarm: Alarm, val selectedDays: DaysOfWeek) : AlarmsScreenAction
    data class DeleteAlarmsScreen(val alarm: Alarm) : AlarmsScreenAction
    data class SelectAlarmsScreen(val alarm: Alarm): AlarmsScreenAction
    data class ChangeTimeFormat(val use24HourFormat: Boolean): AlarmsScreenAction
    data object ShowDaysValidationError : AlarmsScreenAction
}

/**
 * Sealed interface defining one-time events that should be handled by the UI.
 * These events typically represent navigation or user notifications.
 */
sealed interface AlarmsScreenEvent {
    data class OnOpenAlarmEditorFor(val alarm: Alarm): AlarmsScreenEvent
    data class OnShowSnackBar(val message: UiText): AlarmsScreenEvent
}

/**
 * ViewModel responsible for managing the alarm list screen state and user interactions.
 * Handles alarm creation, modification, deletion, and time format preferences.
 *
 * @property repository Repository for alarm data operations
 * @property alarmScheduler Scheduler for managing alarm notifications
 * @property persistManager Manager for persistent preferences
 */
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

    // React on changes in selected alarm by sending an event to UI to open the Alarm editor
    private val selectedAlarmEditorJob = snapshotFlow { state.selectedAlarm }
        .filterNotNull()
        .map { alarm ->
            eventChannel.send(AlarmsScreenEvent.OnOpenAlarmEditorFor(alarm))
        }
        .launchIn(viewModelScope)

    /**
     * Initializes the ViewModel by setting up state observers and data flows.
     * Combines alarm data with minute updates to maintain real-time "time until" calculations.
     *
     * Flow process:
     * 1. Sets initial time format from preferences
     * 2. Combines alarm repository data with minute ticker
     * 3. Transforms alarms into UI states with "time until" calculations
     * 4. Updates the UI state with transformed alarm states
     */
    private fun initializeViewModel() {
        viewModelScope.launch {
            state = state.copy(use24HourFormat = use24HourFormat)

            combine(
                repository.getAlarms(),
                ClockUtils.createMinuteTickerFlow()
            ) { alarms, _ ->
                alarms.map { alarm ->
                    AlarmUiState(
                        alarm = alarm,
                        timeUntilNextAlarm = UiText.StringResource(
                            calculateTimeUntilNextAlarm(
                                alarm.hour,
                                alarm.minute,
                                alarm.selectedDays
                            ).formatTimeUntil())
                    )
                }
            }.collect { alarmUiStates ->
                state = state.copy(alarms = alarmUiStates)
            }
        }
    }

    /**
     * Cleans up resources when ViewModel is destroyed.
     * Cancels the selectedAlarmEditorJob to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        selectedAlarmEditorJob.cancel()
    }

    /**
     * Handles all UI actions performed on the alarm list screen.
     * Processes various actions like adding, enabling/disabling, modifying, and deleting alarms.
     *
     * @param action The action to be processed, defined by [AlarmsScreenAction]
     */
    fun onAction(action: AlarmsScreenAction) {
        when (action) {

            // Creates and opens editor for new alarm
            is AlarmsScreenAction.AddNewAlarm -> {
                viewModelScope.launch {
                    val newAlarm = repository.generateNewAlarm()
                    eventChannel.send(AlarmsScreenEvent.OnOpenAlarmEditorFor(newAlarm))
                }
            }

            // Enables/disables alarm and schedules/cancels it
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

            // Updates selected days for an alarm
            is AlarmsScreenAction.ChangeAlarmsScreenDays -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val updatedAlarm = action.alarm.copy(selectedDays = action.selectedDays)
                    repository.upsertAlarm(updatedAlarm)
                }
            }

            // Deletes alarm and cancels its scheduling
            is AlarmsScreenAction.DeleteAlarmsScreen -> {
                viewModelScope.launch(Dispatchers.IO) {
                    alarmScheduler.cancel(action.alarm)
                    repository.deleteAlarm(action.alarm.id)
                }
            }

            // Shows error when no days are selected
            is AlarmsScreenAction.ShowDaysValidationError -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmsScreenEvent.OnShowSnackBar(UiText.StringResource(R.string.error_need_at_least_one_day)))
                }
            }

            // Sets selected alarm for editing
            is AlarmsScreenAction.SelectAlarmsScreen -> {
                state = state.copy(
                    selectedAlarm = state.alarms.first { it.alarm.id == action.alarm.id }.alarm
                )
            }

            // Updates time format preference
            is AlarmsScreenAction.ChangeTimeFormat -> {
                viewModelScope.launch(Dispatchers.IO) {
                    use24HourFormat = action.use24HourFormat
                    state = state.copy(use24HourFormat = action.use24HourFormat)
                }
            }
        }
    }
}
