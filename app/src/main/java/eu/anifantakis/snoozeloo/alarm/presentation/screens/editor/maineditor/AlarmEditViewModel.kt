package eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import eu.anifantakis.snoozeloo.core.domain.util.ClockUtils
import eu.anifantakis.snoozeloo.core.domain.util.calculateTimeUntilNextAlarm
import eu.anifantakis.snoozeloo.core.domain.util.formatTimeUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    data class UpdateRingtoneResult(val title: String, val uri: String?) : AlarmEditorScreenAction
}

sealed interface AlarmEditorScreenEvent {
    data object OnOpenRingtoneSettings: AlarmEditorScreenEvent
    data class OnShowSnackBar(val message: String): AlarmEditorScreenEvent
    data object OnClose: AlarmEditorScreenEvent
}

class AlarmEditViewModel(
    alarmId: String,
    private val repository: AlarmsRepository,
    private val alarmScheduler: AlarmScheduler
): ViewModel() {

    private val _alarmUiState = MutableStateFlow<AlarmUiState?>(null)
    val alarmUiState = _alarmUiState.asStateFlow()

    private val eventChannel = Channel<AlarmEditorScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    private var minuteTickerJob: Job? = null
    private var alarmObserverJob: Job? = null

    private var originalAlarm: Alarm? = null

    init {
        loadAlarm(alarmId)
        startMinuteTicker()
    }

    private fun updateStateAndCheckChanges(update: (AlarmUiState) -> AlarmUiState) {
        _alarmUiState.update { currentState ->
            currentState?.let { state ->
                val updatedState = update(state)
                // Compare the updated alarm with original alarm and set hasChanges accordingly
                updatedState.copy(
                    timeUntilNextAlarm = calculateTimeUntilNextAlarm(
                        state.alarm.hour,
                        state.alarm.minute,
                        state.alarm.selectedDays
                    ).formatTimeUntil(),
                    hasChanges = originalAlarm?.let { original ->
                        original != updatedState.alarm || updatedState.alarm.temporary
                    } ?: false
                )
            }
        }
    }

    fun onAction(action: AlarmEditorScreenAction) {

        when (action) {
            is AlarmEditorScreenAction.UpdateAlarmDays -> {
                updateStateAndCheckChanges { state ->
                    state.copy(alarm = state.alarm.copy(
                        selectedDays = action.days
                    ))
                }
            }
            is AlarmEditorScreenAction.UpdateAlarmTime -> {
                updateStateAndCheckChanges { state ->
                    state.copy(
                        alarm = state.alarm.copy(
                            hour = action.hour,
                            minute = action.minute,
                        )
                    )
                }
            }
            is AlarmEditorScreenAction.OpenRingtoneSettings -> {
                viewModelScope.launch {
                    eventChannel.send(AlarmEditorScreenEvent.OnOpenRingtoneSettings)
                }
            }
            is AlarmEditorScreenAction.UpdateAlarmTitle -> {
                updateStateAndCheckChanges { state ->
                    state.copy(alarm = state.alarm.copy(
                        title = action.title.trim()
                    ))
                }
            }
            is AlarmEditorScreenAction.UpdateAlarmVolume -> {
                updateStateAndCheckChanges { state ->
                    state.copy(alarm = state.alarm.copy(
                        volume = action.volume
                    ))
                }
            }
            is AlarmEditorScreenAction.UpdateAlarmVibration -> {
                updateStateAndCheckChanges { state ->
                    state.copy(alarm = state.alarm.copy(
                        vibrate = action.vibrate
                    ))
                }
            }
            is AlarmEditorScreenAction.SaveAlarm -> {
                viewModelScope.launch(Dispatchers.IO) {
                    _alarmUiState.value?.let { currentState ->
                        // Handle temporary alarm
                        val updatedAlarm = if (currentState.alarm.temporary) {
                            currentState.alarm.copy(
                                temporary = false,
                                isEnabled = true
                            )
                        } else {
                            currentState.alarm
                        }

                        // Schedule if not temporary
                        if (!updatedAlarm.temporary) {
                            alarmScheduler.schedule(updatedAlarm)
                        }

                        // Save alarm
                        repository.upsertAlarm(updatedAlarm)

                        // Update original alarm to reset change detection
                        originalAlarm = updatedAlarm
                        _alarmUiState.update { it?.copy(
                            alarm = updatedAlarm,
                            hasChanges = false
                        )}

                        eventChannel.send(AlarmEditorScreenEvent.OnClose)
                    }
                }
            }
            is AlarmEditorScreenAction.CancelChanges -> {
                originalAlarm?.let { originalAlarm ->
                    val hadTimeChanges = alarmUiState.value?.alarm?.let { currentAlarm ->
                        currentAlarm.hour != originalAlarm.hour || currentAlarm.minute != originalAlarm.minute
                    } ?: false

                    _alarmUiState.update { state ->
                        state?.copy(
                            alarm = originalAlarm,
                            hasChanges = false
                        )
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        _alarmUiState.value?.let { currentState ->
                            if (currentState.alarm.temporary) {
                                repository.deleteAlarm(currentState.alarm.id)
                            }
                        }

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
            is AlarmEditorScreenAction.UpdateRingtoneResult -> {
                updateStateAndCheckChanges { state ->
                    val updatedState = state.copy(
                        alarm = state.alarm.copy(
                            ringtoneTitle = action.title,
                            ringtoneUri = action.uri
                        )
                    )
                    updatedState
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

    private var loaded = false

    fun loadAlarm(id: AlarmId) {
        if (loaded) return

        loaded = true
        viewModelScope.launch {
            try {
                val alarmDeferred = async { repository.getAlarm(id) }
                val loadedAlarm = alarmDeferred.await()
                originalAlarm = loadedAlarm

                val fetchedAlarm = loadedAlarm.copy()
                _alarmUiState.value = AlarmUiState(
                    alarm = fetchedAlarm,
                    timeUntilNextAlarm = calculateTimeUntilNextAlarm(
                        fetchedAlarm.hour,
                        fetchedAlarm.minute,
                        fetchedAlarm.selectedDays
                    ).formatTimeUntil(),
                    hasChanges = fetchedAlarm.temporary
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        minuteTickerJob?.cancel()
        alarmObserverJob?.cancel()
    }
}