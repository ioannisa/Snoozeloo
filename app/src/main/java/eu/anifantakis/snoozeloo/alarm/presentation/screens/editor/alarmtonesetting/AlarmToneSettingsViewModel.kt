package eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.alarmtonesetting

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface AlarmToneAction {
    data class OnOpenRingtonesSetting(val alarmToneUri: String?) : AlarmToneAction
    data class OnSelectAlarmTone(val ringtone: AlarmoneItem) : AlarmToneAction
    data object NavigateBack : AlarmToneAction
}

@Immutable
data class AlarmToneState(
    val ringtones: List<AlarmoneItem> = emptyList(),
    val currentSelectedRingtone: AlarmoneItem? = null,
    val isLoading: Boolean = false
)

sealed interface AlarmToneEvent {
    data class OnNavigateBack(var title: String, var uri: Uri?): AlarmToneEvent
}

data class AlarmoneItem(
    val title: String = "",
    val uri: Uri? = null,
    val isSelected: Boolean = false
)

class AlarmToneSettingViewModel(
    private val ringtoneRepository: RingtoneRepository,
) : ViewModel() {

    var state by mutableStateOf(AlarmToneState())
        private set

    fun getSelectedAlarmTone(): AlarmoneItem? {
        return state.ringtones.find { it.isSelected }
    }

    private val eventChannel = Channel<AlarmToneEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: AlarmToneAction) {
        when (action) {
            is AlarmToneAction.OnOpenRingtonesSetting -> {
                loadRingtones(action.alarmToneUri)
            }
            is AlarmToneAction.OnSelectAlarmTone -> {
                handleRingtoneSelection(action.ringtone)
            }
            AlarmToneAction.NavigateBack -> {
                viewModelScope.launch {
                    ringtoneRepository.stopPlaying()
                    eventChannel.send(AlarmToneEvent.OnNavigateBack(
                        title = state.currentSelectedRingtone?.title ?: "",
                        uri = state.currentSelectedRingtone?.uri)
                    )
                }
            }
        }
    }

    private fun loadRingtones(alarmToneUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                state = state.copy(isLoading = true)
                val defaultSystemAlarmRingtone = ringtoneRepository.getSystemDefaultAlarmRingtone()
                val ringtones = ringtoneRepository.getAllRingtones()

                val toneItems = ringtones.map { (title, uri) ->
                    AlarmoneItem(
                        title = title,
                        uri = uri,
                        isSelected = when {
                            // If alarmToneUri is null or empty and this is the default ringtone
                            alarmToneUri.isNullOrEmpty() && uri == defaultSystemAlarmRingtone.second -> true
                            // Otherwise match by URI
                            uri.toString() == alarmToneUri -> true
                            // Not selected
                            else -> false
                        }
                    )
                }

                state = state.copy(
                    ringtones = toneItems,
                    isLoading = false
                )

                // If no ringtone is selected, select the default system alarm ringtone
                if (toneItems.none { it.isSelected }) {
                    defaultSystemAlarmRingtone?.let { (title, uri) ->
                        handleRingtoneSelection(AlarmoneItem(
                            title = title,
                            uri = uri,
                            isSelected = true
                        ))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load ringtones")
                state = state.copy(isLoading = false)
            }
        }
    }

    private fun handleRingtoneSelection(selectedRingtone: AlarmoneItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update local state with new selection
                val updatedRingtones = state.ringtones.map { ringtone ->
                    ringtone.copy(isSelected = ringtone.uri?.toString() == selectedRingtone.uri?.toString())
                }

                // Also update the "Silent" option's selection state
                if (selectedRingtone.uri == null) {
                    state = state.copy(
                        ringtones = updatedRingtones,
                        currentSelectedRingtone = selectedRingtone
                    )
                } else {
                    state = state.copy(
                        ringtones = updatedRingtones,
                        currentSelectedRingtone = selectedRingtone
                    )
                }

                // Play the selected ringtone
                selectedRingtone.uri?.let { ringtoneRepository.play(it) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update ringtone selection")
            }
        }
    }
}