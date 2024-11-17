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

/**
 * Actions that can be performed in the alarm tone settings screen
 */
sealed interface AlarmToneAction {
    data class OnOpenRingtonesSetting(val alarmToneUri: String?) : AlarmToneAction
    data class OnSelectAlarmTone(val ringtone: AlarmoneItem) : AlarmToneAction
    data object NavigateBack : AlarmToneAction
}

/**
 * Represents the UI state for the alarm tone settings screen
 * Marked as @Immutable to optimize Compose recompositions
 */
@Immutable
data class AlarmToneState(
    val ringtones: List<AlarmoneItem> = emptyList(),
    val currentSelectedRingtone: AlarmoneItem? = null,
    val isLoading: Boolean = false
)

/**
 * One-time events emitted by the ViewModel
 */
sealed interface AlarmToneEvent {
    data class OnNavigateBack(var title: String, var uri: Uri?): AlarmToneEvent
}

/**
 * Data class representing a single ringtone item in the list
 */
data class AlarmoneItem(
    val title: String = "",
    val uri: Uri? = null,
    val isSelected: Boolean = false
)

/**
 * ViewModel for managing alarm tone selection screen
 * Handles loading ringtones, selection, playback, and state management
 */
class AlarmToneSettingViewModel(
    private val ringtoneRepository: RingtoneRepository,
) : ViewModel() {

    // UI state holder using Compose's mutableStateOf for automatic recomposition
    var state by mutableStateOf(AlarmToneState())
        private set

    // Channel for one-time events like navigation
    private val eventChannel = Channel<AlarmToneEvent>()
    val events = eventChannel.receiveAsFlow()

    /**
     * Handles all UI actions for the alarm tone settings screen
     */
    fun onAction(action: AlarmToneAction) {
        when (action) {
            // Load ringtones when settings screen is opened
            is AlarmToneAction.OnOpenRingtonesSetting -> {
                loadRingtones(action.alarmToneUri)
            }
            // Handle ringtone selection and playback
            is AlarmToneAction.OnSelectAlarmTone -> {
                handleRingtoneSelection(action.ringtone)
            }
            // Handle navigation back with selected ringtone
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

    /**
     * Loads all available ringtones and sets initial selection
     * @param alarmToneUri URI of the currently selected ringtone, if any
     */
    private fun loadRingtones(alarmToneUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Show loading state
                state = state.copy(isLoading = true)

                // Get system default and all available ringtones
                val defaultSystemAlarmRingtone = ringtoneRepository.getSystemDefaultAlarmRingtone()
                val ringtones = ringtoneRepository.getAllRingtones()

                // Map ringtones to UI items with selection state
                val toneItems = ringtones.map { (title, uri) ->
                    AlarmoneItem(
                        title = title,
                        uri = uri,
                        isSelected = when {
                            // Select default ringtone if no previous selection
                            alarmToneUri.isNullOrEmpty() && uri == defaultSystemAlarmRingtone.second -> true
                            // Select previously selected ringtone
                            uri.toString() == alarmToneUri -> true
                            // Not selected
                            else -> false
                        }
                    )
                }

                // Update state with loaded ringtones
                state = state.copy(
                    ringtones = toneItems,
                    isLoading = false
                )

                // If no ringtone is selected, default to system alarm tone
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

    /**
     * Handles ringtone selection and playback
     * Updates UI state and plays selected ringtone
     * @param selectedRingtone The ringtone item that was selected
     */
    private fun handleRingtoneSelection(selectedRingtone: AlarmoneItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update selection state for all ringtones
                val updatedRingtones = state.ringtones.map { ringtone ->
                    ringtone.copy(isSelected = ringtone.uri?.toString() == selectedRingtone.uri?.toString())
                }

                // Update state with new selection
                state = state.copy(
                    ringtones = updatedRingtones,
                    currentSelectedRingtone = selectedRingtone
                )

                // Play the selected ringtone if it has a URI
                selectedRingtone.uri?.let { ringtoneRepository.play(it) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update ringtone selection")
            }
        }
    }
}