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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration

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
    data class UpdateRingtoneResult(val title: String, val uri: String?) : AlarmEditorScreenAction
}

sealed interface AlarmEditorScreenEvent {
    data object OnOpenRingtoneSettings: AlarmEditorScreenEvent
    data class OnShowSnackBar(val message: String): AlarmEditorScreenEvent
    data object OnClose: AlarmEditorScreenEvent
}

/**
 * ViewModel responsible for managing the alarm editing screen.
 * Handles alarm creation, modification, and state management.
 *
 * @param alarm The Alarm which is to be edited
 * @param repository Repository for alarm data operations
 * @param alarmScheduler Scheduler for managing alarm timing
 */
class AlarmEditViewModel(
    alarm: Alarm,
    private val repository: AlarmsRepository,
    private val alarmScheduler: AlarmScheduler
): ViewModel() {

    // Base state holding the current alarm data
    private val _baseAlarmState = MutableStateFlow<Alarm?>(null)

    // Original alarm state used to detect changes and handle cancellations
    private var originalAlarm: Alarm? = null

    // Flow that emits a new value every minute, used to update time-based UI elements
    private val minuteTicker = ClockUtils.createMinuteTickerFlow()
        .stateIn(
            scope = viewModelScope,
            // Stop collecting after 5 seconds of no subscribers to handle configuration changes
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = System.currentTimeMillis()
        )

    // Combines base alarm state with minute ticker to create derived UI state
    // Here we also demonstrate how we can turn a StateFlow to State with the extension function toComposeState
    // which is a nice alternative to the "collect { state -> myState = myState.copy(...) } " function we use in the AlarmsViewModel
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

            if (timeUntilNextAlarm != Duration.ZERO) {
                AlarmUiState(
                    alarm = alarm,
                    // Calculate time until next alarm trigger, updates every minute
                    timeUntilNextAlarm = UiText.StringResource( timeUntilNextAlarm.formatTimeUntil()),
                    // Determine if current state differs from original
                    hasChanges = originalAlarm?.let { originalAlarm ->
                        alarm != originalAlarm || alarm.isNewAlarm
                    } ?: false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    ).toComposeState(viewModelScope)

    // Channel for one-time events (navigation, snackbar messages, etc.)
    private val eventChannel = Channel<AlarmEditorScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        loadAlarm(alarm)
    }

    /**
     * Helper function to update the alarm state while maintaining null safety
     */
    private fun updateAlarm(update: (Alarm) -> Alarm) {
        _baseAlarmState.update { currentState ->
            currentState?.let { update(it) }
        }
    }

    /**
     * Handles all UI actions for the alarm editor screen
     */
    fun onAction(action: AlarmEditorScreenAction) {
        when (action) {
            // Update alarm day selection
            is AlarmEditorScreenAction.UpdateAlarmDays -> {
                updateAlarm { it.copy(selectedDays = action.days) }
            }
            // Update alarm time
            is AlarmEditorScreenAction.UpdateAlarmTime -> {
                updateAlarm { it.copy(
                    hour = action.hour,
                    minute = action.minute
                )}
            }
            // Update alarm title, trimming whitespace
            is AlarmEditorScreenAction.UpdateAlarmTitle -> {
                updateAlarm { it.copy(title = action.title.trim()) }
            }
            // Update alarm volume
            is AlarmEditorScreenAction.UpdateAlarmVolume -> {
                updateAlarm { it.copy(volume = action.volume) }
            }
            // Toggle alarm vibration
            is AlarmEditorScreenAction.UpdateAlarmVibration -> {
                updateAlarm { it.copy(vibrate = action.vibrate) }
            }
            // Update alarm ringtone settings
            is AlarmEditorScreenAction.UpdateRingtoneResult -> {
                updateAlarm { it.copy(
                    ringtoneTitle = action.title,
                    ringtoneUri = action.uri
                )}
            }
            // Open ringtone selection screen
            is AlarmEditorScreenAction.OpenRingtoneSettings -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmEditorScreenEvent.OnOpenRingtoneSettings)
                }
            }
            // Save alarm changes
            is AlarmEditorScreenAction.SaveAlarm -> {
                viewModelScope.launch(Dispatchers.IO) {
                    _baseAlarmState.value?.let { currentAlarm ->
                        if (currentAlarm.isEnabled) {
                            alarmScheduler.schedule(currentAlarm)
                        }

                        // Persist alarm to storage
                        repository.upsertAlarm(currentAlarm)

                        // Update state references
                        originalAlarm = currentAlarm
                        _baseAlarmState.value = currentAlarm

                        eventChannel.send(AlarmEditorScreenEvent.OnClose)
                    }
                }
            }
            // Cancel changes and restore original state
            is AlarmEditorScreenAction.CancelChanges -> {
                originalAlarm?.let { original ->
                    // Check if time was modified for animation purposes
                    val hadTimeChanges = _baseAlarmState.value?.let { currentAlarm ->
                        currentAlarm.hour != original.hour || currentAlarm.minute != original.minute
                    } ?: false

                    // Restore original state
                    _baseAlarmState.value = original

                    viewModelScope.launch(Dispatchers.IO) {
                        // Add delay for time change animation
                        if (hadTimeChanges) {
                            delay(400L)
                        }
                        eventChannel.send(AlarmEditorScreenEvent.OnClose)
                    }
                }
            }
            // Show validation error for missing days selection
            is AlarmEditorScreenAction.ShowDaysValidationError -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmEditorScreenEvent.OnShowSnackBar("All alarms need at least one active day"))
                }
            }
        }
    }

    /**
     * Loads Initializes Alarm state with the provided alarm
     * Prevents multiple loads of the same alarm
     */
    private fun loadAlarm(alarm: Alarm) {
        originalAlarm = alarm.copy()
        _baseAlarmState.value = alarm.copy()
        println("ALARM IS ${originalAlarm}")
    }
}