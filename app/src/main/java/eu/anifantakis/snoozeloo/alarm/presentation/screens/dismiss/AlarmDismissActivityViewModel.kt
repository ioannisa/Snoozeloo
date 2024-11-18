package eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
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
import eu.anifantakis.snoozeloo.core.data.AlarmReceiver
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
    val alarmId: String? = null
)

/**
 * ViewModel for the alarm dismissal screen
 * Handles alarm playback, vibration, snoozing, and dismissal
 *
 * @param application Application context needed for system services
 * @param onFinish Callback to close the screen when alarm is dismissed
 */
class AlarmDismissActivityViewModel(
    application: Application,
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

    /**
     * Lazy getters for Android system services to ensure they're only retrieved when needed
     */
    private val context: Context
        get() = getApplication()

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Gets the appropriate vibrator service based on Android version
     * Handles the API level differences in vibrator service acquisition
     */
    private val vibratorService: Vibrator
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    /**
     * Updates the alarm state with data received from the launching intent
     */
    fun updateAlarmData(
        title: String,
        volume: Float,
        shouldVibrate: Boolean,
        ringtoneUri: String?,
        alarmId: String?
    ) {
        _state.update {
            it.copy(
                title = title,
                volume = volume,
                shouldVibrate = shouldVibrate,
                ringtoneUri = ringtoneUri,
                alarmId = alarmId
            )
        }
    }

    /**
     * Initiates alarm playback and vibration if enabled
     * Runs in coroutine scope to handle potential long-running operations
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
     * Configures and starts the MediaPlayer for alarm sound
     * Sets up audio attributes, data source, and playback parameters
     */
    private fun startAlarmSound() {
        mediaPlayer = MediaPlayer().apply {
            // Configure for alarm audio stream
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            // Set ringtone source, falling back to system default if none specified
            setDataSource(
                context,
                if (!state.value.ringtoneUri.isNullOrEmpty()) {
                    Uri.parse(state.value.ringtoneUri)
                } else {
                    Settings.System.DEFAULT_ALARM_ALERT_URI
                }
            )

            // Configure playback settings
            isLooping = true
            setVolume(state.value.volume, state.value.volume)
            prepare()
            start()
        }
    }

    /**
     * Starts device vibration with a specific pattern
     * Handles different Android API levels for vibration implementation
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
     * Schedules a new alarm and dismisses the current one
     */
    fun snoozeAlarm() {

        println("Snoozing alarm 1")
        if (state.value.alarmId != null) {
            println("Snoozing alarm 2")
            scheduleSnoozeAlarm()
        }
        dismissAlarm()
    }

    /**
     * Schedules a new alarm for 5 minutes later
     * Creates a PendingIntent with the current alarm's parameters
     */
    private fun scheduleSnoozeAlarm() {
        println("Snoozing alarm 3")

        val title = if (state.value.title.contains("(Snoozed)"))
            state.value.title
        else
            "${state.value.title} (Snoozed)"

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", state.value.alarmId)
            putExtra("TITLE", title)
            putExtra("VOLUME", state.value.volume)
            putExtra("VIBRATE", state.value.shouldVibrate)
            putExtra("ALARM_URI", state.value.ringtoneUri)
        }
        println("Snoozing alarm 4")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        println("Snoozing alarm 5")

        val triggerTime = System.currentTimeMillis() +
                (DEFAULT_SNOOZE_DURATION_MINUTES * 60 * 1000)

        println("Snoozing alarm 6  at $triggerTime")

        // Schedule alarm using appropriate API based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        println("Snoozing alarm 7")
    }

    /**
     * Stops all alarm components and cleans up resources
     * Called when alarm is dismissed or snoozed
     */
    fun dismissAlarm() {
        println("Snoozing alarm 8")
        viewModelScope.launch {
            Timber.tag(TAG).d("Dismissing alarm")

            // Stop and release MediaPlayer
            mediaPlayer?.apply {
                try {
                    if (isPlaying) stop()
                    release()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error stopping media player")
                }
            }
            mediaPlayer = null

            // Stop vibration
            vibrator?.cancel()
            vibrator = null

            // Remove notification
            notificationManager.cancel(NOTIFICATION_ID)

            // Update state and close screen
            _state.update { it.copy(isPlaying = false) }
            onFinish()
        }
    }

    /**
     * Cleanup when ViewModel is destroyed
     * Ensures all resources are properly released
     */
    override fun onCleared() {
        super.onCleared()
        dismissAlarm()
    }
}