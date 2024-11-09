package eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm.AlarmUiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RingtoneSettingsViewModel(
    private val repository: RingtoneRepository
    //todo: add the data store
): ViewModel() {

    var state by mutableStateOf(RingtoneState())
        private set

    init {
        onAction(RingtoneAction.OnOpenRingtonesSetting)
    }

    private val eventChannel = Channel<AlarmUiEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: RingtoneAction) {
        when (action) {
            is RingtoneAction.OnOpenRingtonesSetting -> {
                val ringtones = repository.getDefaultRingtones()
                state = state.copy(ringtones = ringtones)
            }
            is RingtoneAction.OnSelectRingtone -> {
                //todo: add selected ringtone to database
                state = state.copy(selectedRingtone = action.ringtone)
                viewModelScope.launch {
                    action.ringtone?.second?.let {
                        repository.play(it)
                    }
                }
            }

            RingtoneAction.OnNavigateBack -> {
                //todo: handle navigation
                /*viewModelScope.launch {
                    eventChannel.send(AlarmUiEvent.OnNavigateBack)
                }*/
            }
        }
    }
}
