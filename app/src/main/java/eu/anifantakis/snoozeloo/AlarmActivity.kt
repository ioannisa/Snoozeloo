package eu.anifantakis.snoozeloo

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmDismissScreen
import eu.anifantakis.snoozeloo.core.data.AlarmReceiver
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import timber.log.Timber

class AlarmActivity : ComponentActivity() {
    companion object {
        private const val TAG = "AlarmActivity"
        private const val DEFAULT_VOLUME = 0.5f
        private const val DEFAULT_SNOOZE_DURATION_MINUTES = 5L
        private const val NOTIFICATION_ID = 1
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("AlarmActivity onCreate called")

        setupWindow()
        handleScreenWake()

        val alarmData = extractAlarmData()

        setContent {
            SnoozelooTheme {
                AlarmDismissScreen(
                    title = alarmData.title,
                    onSnooze = { snoozeAlarm() },
                    onDismiss = { dismissAlarm() }
                )
            }
        }

        when (intent?.action) {
            "DISMISS_ALARM" -> dismissAlarm()
            "SNOOZE_ALARM" -> snoozeAlarm()
            else -> startAlarm(alarmData)
        }
    }

    private data class AlarmData(
        val title: String,
        val volume: Float,
        val shouldVibrate: Boolean,
        val ringtoneUri: String?,
        val alarmId: String?
    )

    private fun extractAlarmData() = AlarmData(
        title = intent?.getStringExtra("TITLE") ?: "Alarm",
        volume = intent?.getFloatExtra("VOLUME", DEFAULT_VOLUME) ?: DEFAULT_VOLUME,
        shouldVibrate = intent?.getBooleanExtra("VIBRATE", true) ?: true,
        ringtoneUri = intent?.getStringExtra("ALARM_URI"),
        alarmId = intent?.getStringExtra("ALARM_ID")
    )

    private fun setupWindow() {
        if (Settings.canDrawOverlays(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION")
                window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes.format = PixelFormat.TRANSLUCENT
    }

    private fun handleScreenWake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun startAlarm(alarmData: AlarmData) {
        try {
            Timber.tag(TAG).d("Starting alarm with data: $alarmData")
            startAlarmSound(alarmData.volume, alarmData.ringtoneUri)
            if (alarmData.shouldVibrate) {
                startVibration()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error starting alarm")
        }
    }

    private fun startAlarmSound(volume: Float, ringtoneUri: String?) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            setDataSource(
                this@AlarmActivity,
                if (!ringtoneUri.isNullOrEmpty()) {
                    Uri.parse(ringtoneUri)
                } else {
                    Settings.System.DEFAULT_ALARM_ALERT_URI
                }
            )

            isLooping = true
            setVolume(volume, volume)
            prepare()
            start()
        }
    }

    private fun getVibratorService(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun startVibration() {
        vibrator = getVibratorService()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
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

    private fun snoozeAlarm() {
        val alarmData = extractAlarmData()
        if (alarmData.alarmId != null) {
            scheduleSnoozeAlarm(alarmData)
        }
        dismissAlarm()
    }

    private fun scheduleSnoozeAlarm(alarmData: AlarmData) {
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmData.alarmId)
            putExtra("TITLE", "${alarmData.title} (Snoozed)")
            putExtra("VOLUME", alarmData.volume)
            putExtra("VIBRATE", alarmData.shouldVibrate)
            putExtra("ALARM_URI", alarmData.ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val triggerTime = System.currentTimeMillis() +
                (DEFAULT_SNOOZE_DURATION_MINUTES * 1 * 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                android.app.AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun dismissAlarm() {
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

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.tag(TAG).d("AlarmActivity onNewIntent with action: ${intent.action}")

        setIntent(intent)

        when (intent.action) {
            "DISMISS_ALARM" -> dismissAlarm()
            "SNOOZE_ALARM" -> snoozeAlarm()
            else -> {
                val alarmData = extractAlarmData()
                startAlarm(alarmData)
            }
        }
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("AlarmActivity onDestroy")
        dismissAlarm()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(TAG).d("AlarmActivity onPause")
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(TAG).d("AlarmActivity onResume")
    }
}