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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.anifantakis.snoozeloo.core.data.AlarmReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

// Holds the current state of the alarm
data class AlarmState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val volume: Float = 0.5f,
    val shouldVibrate: Boolean = true,
    val ringtoneUri: String? = null,
    val alarmId: String? = null
)

class AlarmDismissViewModel(
    application: Application,
    private val onFinish: () -> Unit  // Called when alarm is dismissed to close the screen
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AlarmDismissViewModel"
        private const val DEFAULT_SNOOZE_DURATION_MINUTES = 5L
        private const val NOTIFICATION_ID = 1
    }

    private val _state = MutableStateFlow(AlarmState())
    val state: StateFlow<AlarmState> = _state.asStateFlow()

    // Media components that need to be cleaned up
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // Getters for Android system services
    private val context: Context
        get() = getApplication()

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val vibratorService: Vibrator
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    // Updates alarm data when received from intent
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

    // Starts playing the alarm sound and vibration
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

    // Sets up and starts playing the alarm sound
    private fun startAlarmSound() {
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

            isLooping = true
            setVolume(state.value.volume, state.value.volume)
            prepare()
            start()
        }
    }

    // Starts vibration with a pattern
    private fun startVibration() {
        vibrator = vibratorService

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Vibration pattern: wait, vibrate, pause, vibrate, pause, vibrate
                val timings = longArrayOf(0, 500, 200, 500, 200, 500)
                val amplitudes = intArrayOf(
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE
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

    // Called when user hits snooze button
    fun snoozeAlarm() {
        if (state.value.alarmId != null) {
            scheduleSnoozeAlarm()
        }
        dismissAlarm()
    }

    // Schedules a new alarm for 5 minutes later
    private fun scheduleSnoozeAlarm() {
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", state.value.alarmId)
            putExtra("TITLE", "${state.value.title} (Snoozed)")
            putExtra("VOLUME", state.value.volume)
            putExtra("VIBRATE", state.value.shouldVibrate)
            putExtra("ALARM_URI", state.value.ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() +
                (DEFAULT_SNOOZE_DURATION_MINUTES * 60 * 1000)

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
    }

    // Stops alarm sound, vibration, and closes the screen
    fun dismissAlarm() {
        viewModelScope.launch {
            Timber.tag(TAG).d("Dismissing alarm")

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

            notificationManager.cancel(NOTIFICATION_ID)

            _state.update { it.copy(isPlaying = false) }

            onFinish()
        }
    }

    // Clean up resources when ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        dismissAlarm()
    }
}