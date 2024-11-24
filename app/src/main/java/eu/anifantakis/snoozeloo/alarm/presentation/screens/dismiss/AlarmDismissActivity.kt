package eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

// Activity that shows when alarm is triggered
class AlarmDismissActivity : ComponentActivity() {
    companion object {
        private const val TAG = "AlarmActivity"
        private const val DEFAULT_VOLUME = 0.5f
    }

    // ViewModel with finish callback for closing the activity
    private val viewModel: AlarmDismissActivityViewModel by viewModel() {
        parametersOf(this::finish)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("AlarmActivity onCreate called")

        setupWindow()
        handleScreenWake()
        setupAlarm()

        // Setup the UI using Jetpack Compose
        setContent {
            val alarmState by viewModel.state.collectAsStateWithLifecycle()

            SnoozelooTheme {
                AlarmDismissScreen(
                    title = alarmState.title,
                    onSnooze = viewModel::snoozeAlarm,
                    onDismiss = viewModel::dismissAlarm
                )
            }
        }

        // Now set the window size after setContent
        setWindowSize()
    }

    // Setup window to show over lockscreen and keep screen on
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

        // Ensure the window background is transparent to display rounded corners
        window.setBackgroundDrawableResource(android.R.color.transparent)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    // Set the window size to 90% width and 50% height
    private fun setWindowSize() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = (displayMetrics.widthPixels * 0.9).toInt()
        val height = (displayMetrics.heightPixels * 0.5).toInt()
        window.setLayout(width, height)
    }

    // Make sure screen turns on and unlocks when alarm triggers
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

    // Get alarm data from intent and start/snooze/dismiss based on action
    private fun setupAlarm() {
        lifecycleScope.launch {
            viewModel.updateAlarmData(
                title = intent?.getStringExtra("TITLE") ?: "Alarm",
                volume = intent?.getFloatExtra("VOLUME", DEFAULT_VOLUME) ?: DEFAULT_VOLUME,
                shouldVibrate = intent?.getBooleanExtra("VIBRATE", true) ?: true,
                ringtoneUri = intent?.getStringExtra("ALARM_URI"),
                alarmId = intent?.getStringExtra("ALARM_ID"),
                dayOfWeek = intent?.getIntExtra("DAY_OF_WEEK", -1) ?: -1,
                hour = intent?.getIntExtra("HOUR", 0) ?: 0,
                minute = intent?.getIntExtra("MINUTE", 0) ?: 0
            )

            when (intent?.action) {
                "DISMISS_ALARM" -> viewModel.dismissAlarm()
                "SNOOZE_ALARM" -> viewModel.snoozeAlarm()
                else -> viewModel.startAlarm()
            }
        }
    }

    // Handle new intents (e.g., when snooze triggers)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.tag(TAG).d("AlarmActivity onNewIntent with action: ${intent.action}")
        setIntent(intent)
        setupAlarm()
    }

    override fun onStop() {
        super.onStop()
        // Prevent the activity from being stopped unless explicitly dismissed
        moveTaskToFront()
    }

    // If needed, also prevent recent apps from closing the alarm
    override fun onPause() {
        super.onPause()
        if (!isFinishing) {
            moveTaskToFront()
        }
    }

    private fun moveTaskToFront() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
    }
}
