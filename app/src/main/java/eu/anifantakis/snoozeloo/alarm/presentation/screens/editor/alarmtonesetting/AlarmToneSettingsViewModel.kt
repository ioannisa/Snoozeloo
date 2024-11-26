package eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.alarmtonesetting

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.RingtoneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Defines possible user actions in the alarm tone settings screen.
 */
sealed interface AlarmToneAction {
    /** Open settings with optionally pre-selected tone */
    data class OnOpenRingtonesSetting(val alarmToneUri: String?) : AlarmToneAction

    /** User selects a ringtone */
    data class OnSelectAlarmTone(val ringtone: AlarmoneItem) : AlarmToneAction

    /** User navigates back */
    data object NavigateBack : AlarmToneAction
}

/**
 * Represents the complete UI state for alarm tone settings.
 * Immutable to optimize Compose recompositions.
 *
 * @property ringtones List of available ringtones
 * @property currentSelectedRingtone Currently selected ringtone
 * @property isLoading Whether ringtones are being loaded
 * @property defaultSystemRingtone System's default alarm tone
 */
@Immutable
data class AlarmToneState(
    val ringtones: List<AlarmoneItem> = emptyList(),
    val currentSelectedRingtone: AlarmoneItem? = null,
    val isLoading: Boolean = false,
    val defaultSystemRingtone: AlarmoneItem? = null
)

/**
 * One-time events emitted by the ViewModel.
 * Used for navigation and user notifications.
 */
sealed interface AlarmToneEvent {
    /** Navigate back with selected ringtone */
    data class OnNavigateBack(var title: String, var uri: Uri?): AlarmToneEvent
}

/**
 * Data model for a single ringtone item.
 * Contains display information and selection state.
 *
 * @property title Display name of the ringtone
 * @property uri URI for accessing the ringtone file
 * @property isSelected Whether this ringtone is currently selected
 */
data class AlarmoneItem(
    val title: String = "",
    val uri: Uri? = null,
    val isSelected: Boolean = false
)

/**
 * ViewModel managing ringtone selection and playback.
 * Handles loading available ringtones, selection state, and preview playback.
 *
 * Key responsibilities:
 * - Loading system and custom ringtones
 * - Managing selection state
 * - Handling ringtone preview playback
 * - Maintaining UI state
 *
 * @property ringtoneRepository Data source for ringtone operations
 */
class AlarmToneSettingViewModel(
    private val ringtoneRepository: RingtoneRepository,
) : ViewModel() {

    // UI state with Compose state holder
    var state by mutableStateOf(AlarmToneState())
        private set

    // Channel for one-time events
    private val eventChannel = Channel<AlarmToneEvent>()
    val events = eventChannel.receiveAsFlow()

    /**
     * Central handler for all UI actions.
     * Routes actions to appropriate handlers and updates state accordingly.
     *
     * @param action The UI action to process
     */
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
                        uri = state.currentSelectedRingtone?.uri
                    ))
                }
            }
        }
    }

    /**
     * Loads all available ringtones and sets initial selection.
     * Updates UI state with loaded ringtones and handles loading state.
     *
     * @param alarmToneUri Optional URI of pre-selected ringtone
     */
    private fun loadRingtones(alarmToneUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                state = state.copy(isLoading = true)

                // Load ringtones from repository
                val defaultSystemAlarmRingtone = ringtoneRepository.getSystemDefaultAlarmRingtone()
                val ringtones = ringtoneRepository.getAllRingtones()

                // Create default ringtone UI item
                val defaultRingtoneItem = defaultSystemAlarmRingtone.let { (title, uri) ->
                    AlarmoneItem(
                        title = title,
                        uri = uri,
                        isSelected = alarmToneUri.isNullOrEmpty()
                    )
                }

                // Map repository ringtones to UI items
                val toneItems = ringtones.map { (title, uri) ->
                    AlarmoneItem(
                        title = title,
                        uri = uri,
                        isSelected = uri.toString() == alarmToneUri
                    )
                }

                // Update state with loaded data
                state = state.copy(
                    ringtones = toneItems,
                    defaultSystemRingtone = defaultRingtoneItem,
                    currentSelectedRingtone = when {
                        !alarmToneUri.isNullOrEmpty() -> toneItems.find { it.uri.toString() == alarmToneUri }
                        else -> defaultRingtoneItem
                    },
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load ringtones")
                state = state.copy(isLoading = false)
            }
        }
    }

    /**
     * Processes ringtone selection.
     * Updates selection state and plays preview of selected tone.
     *
     * @param selectedRingtone The newly selected ringtone
     */
    private fun handleRingtoneSelection(selectedRingtone: AlarmoneItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update selection state for all ringtones
                val updatedRingtones = state.ringtones.map { ringtone ->
                    ringtone.copy(isSelected = ringtone.uri?.toString() == selectedRingtone.uri?.toString())
                }

                // Update UI state
                state = state.copy(
                    ringtones = updatedRingtones,
                    currentSelectedRingtone = selectedRingtone
                )

                // Play preview of selected ringtone
                selectedRingtone.uri?.let { ringtoneRepository.play(it) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update ringtone selection")
            }
        }
    }
}