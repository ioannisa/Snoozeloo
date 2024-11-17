package eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.AlarmsRepository
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface RingtoneAction {
    data class OnOpenRingtonesSetting(val alarmId: String) : RingtoneAction
    data class OnSelectRingtone(val ringtone: RingtoneItem) : RingtoneAction
    data object NavigateBack : RingtoneAction
}

@Immutable
data class RingtoneState(
    val currentAlarm: Alarm? = null,
    val ringtones: List<RingtoneItem> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface RingtoneEvent {
    data object OnNavigateBack: RingtoneEvent
}

data class RingtoneItem(
    val title: String = "",
    val uri: Uri? = null,
    val isSelected: Boolean = false
)

class RingtoneSettingsViewModel(
    private val ringtoneRepository: RingtoneRepository,
    private val alarmRepository: AlarmsRepository
) : ViewModel() {

    var state by mutableStateOf(RingtoneState())
        private set

    private val eventChannel = Channel<RingtoneEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: RingtoneAction) {
        when (action) {
            is RingtoneAction.OnOpenRingtonesSetting -> {
                loadRingtones(action.alarmId)
            }
            is RingtoneAction.OnSelectRingtone -> {
                handleRingtoneSelection(action.ringtone)
            }
            RingtoneAction.NavigateBack -> {
                viewModelScope.launch {
                    ringtoneRepository.stopPlaying()
                    eventChannel.send(RingtoneEvent.OnNavigateBack)
                }
            }
        }
    }

    private fun loadRingtones(alarmId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                state = state.copy(isLoading = true)

                val alarm = alarmRepository.getAlarm(alarmId)
                // Changed logic: alarm has no ringtone if either title or URI is empty/null
                val hasNoRingtoneSelected = alarm.ringtoneTitle.isEmpty() || alarm.ringtoneUri.isNullOrEmpty()

                // If we already have ringtones, just update the selection state
                if (state.ringtones.isNotEmpty()) {
                    val defaultAlarmRingtone = if (hasNoRingtoneSelected) {
                        ringtoneRepository.getSystemDefaultAlarmRingtone()
                    } else null

                    val updatedRingtones = state.ringtones.map { ringtone ->
                        ringtone.copy(
                            isSelected = if (hasNoRingtoneSelected) {
                                // Match by URI for default ringtone
                                ringtone.uri == defaultAlarmRingtone?.second
                            } else {
                                // Match by either title or URI for selected ringtone
                                ringtone.title == alarm.ringtoneTitle ||
                                        ringtone.uri?.toString() == alarm.ringtoneUri
                            }
                        )
                    }

                    val updatedAlarm = if (hasNoRingtoneSelected && defaultAlarmRingtone != null) {
                        alarm.copy(
                            ringtoneTitle = defaultAlarmRingtone.first,
                            ringtoneUri = defaultAlarmRingtone.second.toString()
                        ).also {
                            // Immediately save the default ringtone selection
                            alarmRepository.upsertAlarm(it)
                        }
                    } else {
                        alarm
                    }

                    state = state.copy(
                        ringtones = updatedRingtones,
                        currentAlarm = updatedAlarm,
                        isLoading = false
                    )

                    return@launch
                }

                // Load ringtones only if the list is empty
                val ringtones = ringtoneRepository.getDefaultRingtones()
                val defaultAlarmRingtone = if (hasNoRingtoneSelected) {
                    ringtoneRepository.getSystemDefaultAlarmRingtone()
                } else null

                val ringtoneItems = ringtones.map { (title, uri) ->
                    RingtoneItem(
                        title = title,
                        uri = uri,
                        isSelected = if (hasNoRingtoneSelected) {
                            // Match by URI for default ringtone
                            uri == defaultAlarmRingtone?.second
                        } else {
                            // Match by either title or URI for selected ringtone
                            title == alarm.ringtoneTitle ||
                                    uri.toString() == alarm.ringtoneUri
                        }
                    )
                }

                val updatedAlarm = if (hasNoRingtoneSelected && defaultAlarmRingtone != null) {
                    alarm.copy(
                        ringtoneTitle = defaultAlarmRingtone.first,
                        ringtoneUri = defaultAlarmRingtone.second.toString()
                    ).also {
                        // Immediately save the default ringtone selection
                        alarmRepository.upsertAlarm(it)
                    }
                } else {
                    alarm
                }

                state = state.copy(
                    ringtones = ringtoneItems,
                    currentAlarm = updatedAlarm,
                    isLoading = false
                )

            } catch (e: Exception) {
                Timber.e(e, "Failed to load ringtones")
                state = state.copy(isLoading = false)
            }
        }
    }

    private fun handleRingtoneSelection(selectedRingtone: RingtoneItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                state.currentAlarm?.let { currentAlarm ->
                    // Update local state
                    val updatedAlarm = currentAlarm.copy(
                        ringtoneTitle = selectedRingtone.title,
                        ringtoneUri = selectedRingtone.uri?.toString()
                    )
                    state = state.copy(currentAlarm = updatedAlarm)

                    // Update database and play ringtone
                    alarmRepository.upsertAlarm(updatedAlarm)
                    selectedRingtone.uri?.let { ringtoneRepository.play(it) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update ringtone selection")
            }
        }
    }
}