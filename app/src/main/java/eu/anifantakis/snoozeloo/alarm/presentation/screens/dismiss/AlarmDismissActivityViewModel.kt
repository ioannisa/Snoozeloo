package eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Represents the state of an actively triggered alarm.
 * Contains all necessary data for alarm playback and management.
 *
 * @property isPlaying Whether alarm audio/vibration is active
 * @property title Displayed alarm title
 * @property volume Audio volume (0.0-1.0)
 * @property shouldVibrate Whether vibration is enabled
 * @property ringtoneUri Custom ringtone URI (null for default)
 * @property alarmId Unique identifier for the alarm
 * @property dayOfWeek Day of week this occurrence is for (1-7)
 * @property hour Hour in 24h format (0-23)
 * @property minute Minute (0-59)
 */
@Immutable
data class AlarmOccurrenceState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val volume: Float = 0.5f,
    val shouldVibrate: Boolean = true,
    val ringtoneUri: String? = null,
    val alarmId: AlarmId? = null,
    val dayOfWeek: Int? = null,
    val hour: Int = 0,
    val minute: Int = 0
)

/**
 * ViewModel responsible for managing alarm playback and interactions.
 * Handles audio playback, vibration, volume management, and alarm scheduling.
 *
 * Key responsibilities:
 * - Controls alarm audio playback with volume management
 * - Manages device vibration patterns
 * - Handles snooze and dismiss actions
 * - Preserves and restores audio settings
 *
 * @property application Application context for system service access
 * @property alarmScheduler For scheduling future alarm occurrences
 * @property onFinish Callback to notify activity when alarm is finished
 */
class AlarmDismissActivityViewModel(
    application: Application,
    private val alarmScheduler: AlarmScheduler,
    private val onFinish: () -> Unit
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AlarmDismissViewModel"
        private const val DEFAULT_SNOOZE_DURATION_MINUTES = 5L
        private const val NOTIFICATION_ID = 1
    }

    // UI state management
    private val _state = MutableStateFlow(AlarmOccurrenceState())
    val state: StateFlow<AlarmOccurrenceState> = _state.asStateFlow()

    // Media components requiring lifecycle management
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var previousAlarmVolume: Int? = null // Stored to restore after alarm

    // Lazy system service accessors
    private val context: Context
        get() = getApplication()

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val vibratorService: Vibrator
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    init {
        // Store current volume to restore later
        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    }

    /**
     * Updates alarm state with data from launching intent.
     * Called when alarm is triggered or when receiving new intent.
     */
    fun updateAlarmData(
        title: String,
        volume: Float,
        shouldVibrate: Boolean,
        ringtoneUri: String?,
        alarmId: AlarmId?,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        _state.update {
            it.copy(
                title = title,
                volume = volume,
                shouldVibrate = shouldVibrate,
                ringtoneUri = ringtoneUri,
                alarmId = alarmId,
                dayOfWeek = if (dayOfWeek != -1) dayOfWeek else null,
                hour = hour,
                minute = minute
            )
        }
    }

    /**
     * Initiates alarm playback including audio and optional vibration.
     */
    fun startAlarm() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Starting alarm with state: ${state.value}")
                startAlarmSound()
                if (state.value.shouldVibrate) {
                    startVibration()
                }
                _state.update { it.copy(isPlaying = true) }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error starting alarm")
            }
        }
    }

    /**
     * Configures and starts MediaPlayer for alarm sound.
     * Manages system volume and audio attributes for proper alarm behavior.
     */
    private fun startAlarmSound() {
        try {
            // Configure system volume for alarm stream
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val volumeLevel = (maxVolume * state.value.volume).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeLevel, 0)

            mediaPlayer = MediaPlayer().apply {
                // Set up audio attributes for alarm behavior
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // Use custom ringtone if available, otherwise system default
                setDataSource(
                    context,
                    if (!state.value.ringtoneUri.isNullOrEmpty()) {
                        Uri.parse(state.value.ringtoneUri)
                    } else {
                        Settings.System.DEFAULT_ALARM_ALERT_URI
                    }
                )

                setVolume(1.0f, 1.0f)  // Full volume as we control via AudioManager
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error starting alarm sound: ${e.message}")
        }
    }

    /**
     * Configures and starts device vibration with specific pattern.
     * Handles different API levels for vibration effects.
     */
    private fun startVibration() {
        vibrator = vibratorService

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Vibration pattern: [delay, vibrate, pause] repeated
                val timings = longArrayOf(0, 500, 200, 500, 200, 500)
                val amplitudes = intArrayOf(
                    0, VibrationEffect.DEFAULT_AMPLITUDE,
                    0, VibrationEffect.DEFAULT_AMPLITUDE,
                    0, VibrationEffect.DEFAULT_AMPLITUDE
                )
                val vibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, 0)

                vibrator?.run {
                    cancel()
                    vibrate(vibrationEffect)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error creating vibration effect")
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), 0)
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), 0)
        }
    }

    /**
     * Handles snooze action by scheduling new alarm and dismissing current one.
     */
    fun snoozeAlarm() {
        if (state.value.alarmId != null) {
            alarmScheduler.scheduleSnooze(state.value, DEFAULT_SNOOZE_DURATION_MINUTES)
        }
        dismissAlarm()
    }

    /**
     * Stops alarm playback, restores audio settings, and cleans up resources.
     */
    fun dismissAlarm() {
        viewModelScope.launch {
            Timber.tag(TAG).d("Dismissing alarm")

            // Restore previous alarm volume
            previousAlarmVolume?.let { volume ->
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
            }

            stopAlarmPlayback()
            notificationManager.cancel(NOTIFICATION_ID)

            // Schedule next occurrence if this was a regular alarm
            if (state.value.alarmId != null && state.value.dayOfWeek != null) {
                alarmScheduler.rescheduleOccurrenceForNextWeek(state.value)
            }

            _state.update { it.copy(isPlaying = false) }
            onFinish()
        }
    }

    /**
     * Stops and cleans up media playback and vibration resources.
     */
    private fun stopAlarmPlayback() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
                release()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error stopping media player")
            }
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null
    }

    override fun onCleared() {
        super.onCleared()
        dismissAlarm()
    }
}