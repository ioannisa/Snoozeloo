package eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Data class representing the current state of an active alarm
 */
@Immutable
data class AlarmState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val volume: Float = 0.5f,
    val shouldVibrate: Boolean = true,
    val ringtoneUri: String? = null,
    val alarmId: String? = null,
    val dayOfWeek: Int? = null,
    val hour: Int = 0,
    val minute: Int = 0
)

/**
 * ViewModel for the alarm dismissal screen
 * Handles alarm playback, vibration, snoozing, and dismissal
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
    private val _state = MutableStateFlow(AlarmState())
    val state: StateFlow<AlarmState> = _state.asStateFlow()

    // Media components that need lifecycle management
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var previousAlarmVolume: Int? = null

    /**
     * Lazy getters for Android system services
     */
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
        // Store previous alarm volume
        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    }

    /**
     * Updates the alarm state with data received from the launching intent
     */
    fun updateAlarmData(
        title: String,
        volume: Float,
        shouldVibrate: Boolean,
        ringtoneUri: String?,
        alarmId: String?,
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
     * Initiates alarm playback and vibration if enabled
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
     * Configures and starts the MediaPlayer for alarm sound with proper volume control
     */
    private fun startAlarmSound() {
        try {
            // Get max volume for alarm stream and set initial volume
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val volumeLevel = (maxVolume * state.value.volume).toInt()
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                volumeLevel,
                0 // Flags - 0 means no feedback UI
            )

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                setDataSource(
                    context,
                    if (!state.value.ringtoneUri.isNullOrEmpty()) {
                        Uri.parse(state.value.ringtoneUri)
                    } else {
                        Settings.System.DEFAULT_ALARM_ALERT_URI
                    }
                )

                // Set to full volume since we're controlling via AudioManager
                setVolume(1.0f, 1.0f)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error starting alarm sound: ${e.message}")
        }
    }

    /**
     * Starts device vibration with a specific pattern
     */
    private fun startVibration() {
        vibrator = vibratorService

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Vibration pattern: [wait, vibrate, pause, vibrate, pause, vibrate]
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
     * Handles snooze button press
     */
    fun snoozeAlarm() {
        if (state.value.alarmId != null) {
            alarmScheduler.scheduleSnooze(state.value, DEFAULT_SNOOZE_DURATION_MINUTES)
        }
        dismissAlarm()
    }

    /**
     * Stops all alarm components and cleans up resources
     */
    fun dismissAlarm() {
        viewModelScope.launch {
            Timber.tag(TAG).d("Dismissing alarm")

            // Restore previous volume if available
            previousAlarmVolume?.let { volume ->
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
            }

            stopAlarmPlayback()
            notificationManager.cancel(NOTIFICATION_ID)

            if (state.value.alarmId != null && state.value.dayOfWeek != null) {
                alarmScheduler.scheduleNextWeekOccurrence(state.value)
            }

            _state.update { it.copy(isPlaying = false) }
            onFinish()
        }
    }

    /**
     * Stops and releases media playback and vibration
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

    /**
     * Cleanup when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        dismissAlarm()
    }
}