package eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import eu.anifantakis.snoozeloo.core.domain.util.ClockUtils
import eu.anifantakis.snoozeloo.core.domain.util.calculateTimeUntilNextAlarm
import eu.anifantakis.snoozeloo.core.domain.util.formatTimeUntil
import eu.anifantakis.snoozeloo.core.presentation.designsystem.toComposeState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * Actions that can be performed in the alarm editor.
 * Each action represents a distinct user interaction or system event.
 */
sealed interface AlarmEditorScreenAction {
    data class UpdateAlarmTime(val hour: Int, val minute: Int): AlarmEditorScreenAction
    data class UpdateAlarmDays(val days: DaysOfWeek): AlarmEditorScreenAction
    data class UpdateAlarmTitle(val title: String): AlarmEditorScreenAction
    data object OpenRingtoneSettings: AlarmEditorScreenAction
    data object SaveAlarm: AlarmEditorScreenAction
    data object CancelChanges: AlarmEditorScreenAction
    data class UpdateAlarmVolume(val volume: Float): AlarmEditorScreenAction
    data class UpdateAlarmVibration(val vibrate: Boolean): AlarmEditorScreenAction
    data object ShowDaysValidationError: AlarmEditorScreenAction
    data class UpdateRingtoneResult(val title: String, val uri: String?): AlarmEditorScreenAction
}

/**
 * One-time events emitted by the ViewModel.
 * Used for navigation and user notifications.
 */
sealed interface AlarmEditorScreenEvent {
    data object OnOpenRingtoneSettings: AlarmEditorScreenEvent
    data class OnShowSnackBar(val message: String): AlarmEditorScreenEvent
    data object OnClose: AlarmEditorScreenEvent
}

/**
 * ViewModel managing alarm editing functionality.
 * Handles state management, user interactions, and alarm persistence.
 *
 * Key features:
 * - Real-time UI updates with minute-based refresh
 * - Change detection for save/cancel operations
 * - Alarm scheduling integration
 *
 * @property alarm Alarm being edited
 * @property repository Data source for alarm operations
 * @property alarmScheduler Handles alarm scheduling with system
 */
class AlarmEditViewModel(
    alarm: Alarm,
    private val repository: AlarmsRepository,
    private val alarmScheduler: AlarmScheduler
): ViewModel() {

    // Current alarm state
    private val _baseAlarmState = MutableStateFlow<Alarm?>(null)

    // Original alarm state for change detection
    private var originalAlarm: Alarm? = null

    // Minute-based ticker for updating time-based UI elements
    private val minuteTicker = ClockUtils.createMinuteTickerFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = System.currentTimeMillis()
        )

    /**
     * Combined UI state that updates every minute.
     * Includes:
     * - Current alarm state
     * - Time until next alarm
     * - Change detection
     */
    val alarmUiState = combine(
        _baseAlarmState,
        minuteTicker
    ) { currentAlarm, _ ->
        currentAlarm?.let { alarm ->
            val timeUntilNextAlarm = calculateTimeUntilNextAlarm(
                alarm.hour,
                alarm.minute,
                alarm.selectedDays
            )

            AlarmUiState(
                alarm = alarm,
                timeUntilNextAlarm = UiText.StringResource(timeUntilNextAlarm.formatTimeUntil()),
                hasChanges = originalAlarm?.let { originalAlarm ->
                    alarm != originalAlarm || alarm.isNewAlarm
                } ?: false
            ) to timeUntilNextAlarm
        }
    }
        .filterNotNull()  // Remove null states
        .filter { (_, duration) -> duration != Duration.ZERO }  // Only valid durations
        .map { (state, _) -> state }  // Keep only the UI state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
        .toComposeState(viewModelScope)

    // Event channel for one-time UI events
    private val eventChannel = Channel<AlarmEditorScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        loadAlarm(alarm)
    }

    /**
     * Updates alarm state safely with null checking.
     */
    private fun updateAlarm(update: (Alarm) -> Alarm) {
        _baseAlarmState.update { currentState ->
            currentState?.let { update(it) }
        }
    }

    /**
     * Central handler for all UI actions.
     * Processes user interactions and updates state accordingly.
     */
    fun onAction(action: AlarmEditorScreenAction) {
        when (action) {
            is AlarmEditorScreenAction.UpdateAlarmDays -> {
                updateAlarm { it.copy(selectedDays = action.days) }
            }
            is AlarmEditorScreenAction.UpdateAlarmTime -> {
                updateAlarm { it.copy(
                    hour = action.hour,
                    minute = action.minute
                )}
            }
            is AlarmEditorScreenAction.UpdateAlarmTitle -> {
                updateAlarm { it.copy(title = action.title.trim()) }
            }
            is AlarmEditorScreenAction.UpdateAlarmVolume -> {
                updateAlarm { it.copy(volume = action.volume) }
            }
            is AlarmEditorScreenAction.UpdateAlarmVibration -> {
                updateAlarm { it.copy(vibrate = action.vibrate) }
            }
            is AlarmEditorScreenAction.UpdateRingtoneResult -> {
                updateAlarm { it.copy(
                    ringtoneTitle = action.title,
                    ringtoneUri = action.uri
                )}
            }
            is AlarmEditorScreenAction.OpenRingtoneSettings -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmEditorScreenEvent.OnOpenRingtoneSettings)
                }
            }
            is AlarmEditorScreenAction.SaveAlarm -> handleSaveAlarm()
            is AlarmEditorScreenAction.CancelChanges -> handleCancelChanges()
            is AlarmEditorScreenAction.ShowDaysValidationError -> {
                viewModelScope.launch {
                    eventChannel.send(
                        AlarmEditorScreenEvent.OnShowSnackBar("All alarms need at least one active day")
                    )
                }
            }
        }
    }

    /**
     * Handles saving alarm changes.
     * Schedules alarm if enabled and persists changes.
     */
    private fun handleSaveAlarm() {
        viewModelScope.launch(Dispatchers.IO) {
            _baseAlarmState.value?.let { currentAlarm ->
                if (currentAlarm.isEnabled) {
                    alarmScheduler.schedule(currentAlarm)
                }

                repository.upsertAlarm(currentAlarm)
                originalAlarm = currentAlarm
                _baseAlarmState.value = currentAlarm

                eventChannel.send(AlarmEditorScreenEvent.OnClose)
            }
        }
    }

    /**
     * Handles canceling changes with animation delay.
     * Restores original alarm state if changes were made.
     */
    private fun handleCancelChanges() {
        originalAlarm?.let { original ->
            val hadTimeChanges = _baseAlarmState.value?.let { currentAlarm ->
                currentAlarm.hour != original.hour || currentAlarm.minute != original.minute
            } ?: false

            _baseAlarmState.value = original

            viewModelScope.launch(Dispatchers.IO) {
                if (hadTimeChanges) {
                    delay(400L) // Allow time for animation
                }
                eventChannel.send(AlarmEditorScreenEvent.OnClose)
            }
        }
    }

    /**
     * Initializes alarm state.
     * Makes a copy to track original state for change detection.
     */
    private fun loadAlarm(alarm: Alarm) {
        originalAlarm = alarm.copy()
        _baseAlarmState.value = alarm.copy()
    }
}