package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.lib.securepersist.PersistManager
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import eu.anifantakis.snoozeloo.core.domain.util.*
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the alarms list screen.
 *
 * @property alarms List of alarms with their UI representation
 * @property selectedAlarm Currently selected alarm for editing (null if none selected)
 * @property use24HourFormat User preference for time format display
 */
@Immutable
data class AlarmsScreenState(
    val alarms: List<AlarmUiState> = emptyList(),
    val selectedAlarm: Alarm? = null,
    val use24HourFormat: Boolean = false
)

/**
 * Actions that can be triggered by user interactions or system events.
 * Each action represents a distinct operation on the alarm list.
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
 * One-time events that should trigger UI updates or navigation.
 */
sealed interface AlarmsScreenEvent {
    data class OnOpenAlarmEditorFor(val alarm: Alarm): AlarmsScreenEvent
    data class OnShowSnackBar(val message: UiText): AlarmsScreenEvent
}

/**
 * ViewModel for the alarms list screen, handling alarm management and UI state.
 *
 * Key responsibilities:
 * - Maintains and updates the list of alarms
 * - Handles user interactions through actions
 * - Manages time format preferences
 * - Coordinates with AlarmScheduler for alarm notifications
 *
 * @property repository Data source for alarms
 * @property alarmScheduler Manages alarm scheduling with the system
 * @property persistManager Handles persistent preferences storage
 */
class AlarmsViewModel(
    private val repository: AlarmsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val persistManager: PersistManager,
): ViewModel() {

    // UI state holder
    var state by mutableStateOf(AlarmsScreenState())
        private set

    // Persisted time format preference
    private var use24HourFormat by persistManager.dataStorePrefs.preference(false, encrypted = true)

    // Channel for one-time UI events
    private val eventChannel = Channel<AlarmsScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        initializeViewModel()
    }

    // Observes selected alarm changes and triggers editor navigation
    private val selectedAlarmEditorJob = snapshotFlow { state.selectedAlarm }
        .filterNotNull()
        .map { alarm ->
            eventChannel.send(AlarmsScreenEvent.OnOpenAlarmEditorFor(alarm))
            state = state.copy(selectedAlarm = null)
        }
        .launchIn(viewModelScope)

    /**
     * Sets up initial state and data flows.
     * Combines alarm data with minute updates to maintain accurate "time until" displays.
     */
    private fun initializeViewModel() {
        viewModelScope.launch {
            state = state.copy(use24HourFormat = use24HourFormat)

            // Combine alarm data with minute ticker for real-time updates
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

    override fun onCleared() {
        super.onCleared()
        selectedAlarmEditorJob.cancel()
    }

    /**
     * Processes UI actions, updating state and triggering side effects as needed.
     * Handles alarm CRUD operations, scheduling, and preference changes.
     *
     * @param action The UI action to process
     */
    fun onAction(action: AlarmsScreenAction) {
        when (action) {
            // Create and open editor for new alarm
            is AlarmsScreenAction.AddNewAlarm -> {
                viewModelScope.launch {
                    val newAlarm = repository.generateNewAlarm()
                    eventChannel.send(AlarmsScreenEvent.OnOpenAlarmEditorFor(newAlarm))
                }
            }

            // Toggle alarm enabled state and update scheduling
            is AlarmsScreenAction.EnableAlarmsScreen -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val updatedAlarm = action.alarm.copy(isEnabled = action.enabled)
                    repository.upsertAlarm(updatedAlarm)

                    if (action.enabled) {
                        alarmScheduler.schedule(updatedAlarm)
                    } else {
                        alarmScheduler.cancel(updatedAlarm)
                    }
                }
            }

            // Update alarm days and persist changes
            is AlarmsScreenAction.ChangeAlarmsScreenDays -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val updatedAlarm = action.alarm.copy(selectedDays = action.selectedDays)
                    repository.upsertAlarm(updatedAlarm)
                }
            }

            // Remove alarm and cancel its scheduling
            is AlarmsScreenAction.DeleteAlarmsScreen -> {
                viewModelScope.launch(Dispatchers.IO) {
                    alarmScheduler.cancel(action.alarm)
                    repository.deleteAlarm(action.alarm.id)
                }
            }

            // Show validation error for day selection
            is AlarmsScreenAction.ShowDaysValidationError -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmsScreenEvent.OnShowSnackBar(UiText.StringResource(R.string.error_need_at_least_one_day)))
                }
            }

            // Select alarm for editing
            is AlarmsScreenAction.SelectAlarmsScreen -> {
                state = state.copy(
                    selectedAlarm = state.alarms.first { it.alarm.id == action.alarm.id }.alarm.copy(isNewAlarm = false)
                )
            }

            // Update time format preference
            is AlarmsScreenAction.ChangeTimeFormat -> {
                viewModelScope.launch(Dispatchers.IO) {
                    use24HourFormat = action.use24HourFormat
                    state = state.copy(use24HourFormat = action.use24HourFormat)
                }
            }
        }
    }
}