package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.domain.util.ClockUtils
import eu.anifantakis.snoozeloo.core.domain.util.calculateTimeUntilNextAlarm
import eu.anifantakis.snoozeloo.core.domain.util.formatTimeUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

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
}

sealed interface AlarmEditorScreenEvent {
    data object OnOpenRingtoneSettings: AlarmEditorScreenEvent
    data class OnShowSnackBar(val message: String): AlarmEditorScreenEvent
    data object OnClose: AlarmEditorScreenEvent
}

class AlarmEditViewModel(
    private val repository: AlarmsRepository
): ViewModel() {

    private val _alarmUiState = MutableStateFlow<AlarmUiState?>(null)
    val alarmUiState = _alarmUiState.asStateFlow()

    private val eventChannel = Channel<AlarmEditorScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    private var minuteTickerJob: Job? = null
    private var alarmObserverJob: Job? = null
    private var originalAlarm: Alarm? = null

    init {
        startMinuteTicker()
        observeAlarm()
    }

    private fun observeAlarm() {
        alarmObserverJob?.cancel()
        alarmObserverJob = viewModelScope.launch {
            repository.observeEditedAlarm().collect { alarm ->
                alarm?.let {
                    val hasChanges = originalAlarm != null && originalAlarm != it
                    _alarmUiState.value = AlarmUiState(
                        alarm = it,
                        timeUntilNextAlarm = calculateTimeUntilNextAlarm(
                            it.hour,
                            it.minute,
                            it.selectedDays
                        ).formatTimeUntil(),
                        hasChanges = hasChanges
                    )
                }
            }
        }
    }

    fun onAction(action: AlarmEditorScreenAction) {
        when (action) {
            is AlarmEditorScreenAction.UpdateAlarmDays -> {
                updateAlarmDays(action.days)
            }
            is AlarmEditorScreenAction.UpdateAlarmTime -> {
                updateAlarmTime(action.hour, action.minute)
            }
            is AlarmEditorScreenAction.OpenRingtoneSettings -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmEditorScreenEvent.OnOpenRingtoneSettings)
                }
            }
            is AlarmEditorScreenAction.UpdateAlarmTitle -> {
                _alarmUiState.value?.let { currentState ->
                    val updatedAlarm = currentState.alarm.copy(
                        title = action.title.trim()
                    )
                    repository.updateEditedAlarm(updatedAlarm)
                }
            }

            is AlarmEditorScreenAction.UpdateAlarmVolume -> {
                _alarmUiState.value?.let { currentState ->
                    val updatedAlarm = currentState.alarm.copy(
                        volume = action.volume
                    )
                    repository.updateEditedAlarm(updatedAlarm)
                }
            }

            is AlarmEditorScreenAction.UpdateAlarmVibration -> {
                _alarmUiState.value?.let { currentState ->
                    val updatedAlarm = currentState.alarm.copy(
                        vibrate = action.vibrate
                    )
                    repository.updateEditedAlarm(updatedAlarm)
                }
            }

            is AlarmEditorScreenAction.SaveAlarm -> {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveEditedAlarm()
                    // After saving, update the original alarm to reset change detection
                    _alarmUiState.value  = _alarmUiState.value?.copy(hasChanges = false)
                    originalAlarm = _alarmUiState.value?.alarm
                    eventChannel.send(AlarmEditorScreenEvent.OnClose)
                }
            }

            is AlarmEditorScreenAction.CancelChanges -> {
                originalAlarm?.let { originalAlarm ->
                    val hadTimeChanges = alarmUiState.value?.alarm?.let { currentAlarm ->
                        currentAlarm.hour != originalAlarm.hour || currentAlarm.minute != originalAlarm.minute
                    } ?: false

                    _alarmUiState.value = _alarmUiState.value?.copy(
                        hasChanges = false,
                        alarm = originalAlarm
                    )

                    viewModelScope.launch {
                        if (hadTimeChanges) {
                            delay(400L)
                        }
                        eventChannel.send(AlarmEditorScreenEvent.OnClose)
                    }
                }
            }

            is AlarmEditorScreenAction.ShowDaysValidationError -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmEditorScreenEvent.OnShowSnackBar("All alarms need at least one active day"))
                }
            }
        }
    }

    private fun startMinuteTicker() {
        minuteTickerJob?.cancel()
        minuteTickerJob = viewModelScope.launch {
            ClockUtils.createMinuteTickerFlow()
                .collect {
                    _alarmUiState.update { currentState ->
                        currentState?.let { state ->
                            state.copy(
                                timeUntilNextAlarm = calculateTimeUntilNextAlarm(
                                    state.alarm.hour,
                                    state.alarm.minute,
                                    state.alarm.selectedDays
                                ).formatTimeUntil()
                            )
                        }
                    }
                }
        }
    }

    fun loadAlarm(id: AlarmId) {
        viewModelScope.launch {
            try {
                val alarm = repository.getAlarm(id)
                originalAlarm = alarm // Store original state
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    private fun updateAlarmTime(hour: Int, minute: Int) {
        _alarmUiState.value?.let { currentState ->
            val updatedAlarm = currentState.alarm.copy(
                hour = hour,
                minute = minute
            )
            repository.updateEditedAlarm(updatedAlarm)
        }
    }

    private fun updateAlarmDays(days: DaysOfWeek) {
        _alarmUiState.value?.let { currentState ->
            val updatedAlarm = currentState.alarm.copy(selectedDays = days)
            repository.updateEditedAlarm(updatedAlarm)
        }
    }

    override fun onCleared() {
        super.onCleared()
        minuteTickerJob?.cancel()
        alarmObserverJob?.cancel()

        originalAlarm?.let {
            repository.cleanupCurrentlyEditedAlarm()
        }
    }
}