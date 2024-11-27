package eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss

import android.annotation.SuppressLint
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * Activity that displays an overlay when an alarm triggers.
 * Features:
 * - Shows as overlay above other apps and lock screen
 * - Adapts size based on screen state (full screen when screen was off)
 * - Prevents accidental dismissal through recent apps
 * - Handles screen wake and keyguard dismissal
 * - Supports snooze and dismiss actions from both UI and notification
 */
class AlarmDismissActivity : ComponentActivity() {
    companion object {
        private const val TAG = "AlarmActivity"
        private const val DEFAULT_VOLUME = 0.5f
        const val EXTRA_SCREEN_WAS_OFF = "SCREEN_WAS_OFF" // Used to determine overlay size
        const val EXTRA_IS_SNOOZE = "IS_SNOOZE" // Used to determine action if we should show "(Snoozed)" next to title
    }

    // ViewModel with callback for activity finish
    private val viewModel: AlarmDismissActivityViewModel by viewModel() {
        parametersOf(::finishOverlay)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindow()
        handleScreenWake()
        setupAlarm()

        val isSnooze = intent?.getBooleanExtra(EXTRA_IS_SNOOZE, false) ?: false
        val wasScreenOff = intent?.getBooleanExtra(EXTRA_SCREEN_WAS_OFF, false) ?: false

        // Set up the UI with Compose
        setContent {
            val alarmState by viewModel.state.collectAsStateWithLifecycle()

            val defaultTitle = stringResource(R.string.default_alarm_title)
            val snoozeIndicator = stringResource(R.string.snoozed)

            // for empty alarm titles, show default alarm title
            val title = alarmState.title.ifBlank { defaultTitle }
            // if this is a snoozed alarm, show the "(Snoozed)"indicator  next to title
            val alarmTitle = if (isSnooze) "$title $snoozeIndicator" else title

            SnoozelooTheme {
                AlarmDismissScreen(
                    title = alarmTitle,
                    isFullScreen = wasScreenOff,
                    onSnooze = viewModel::snoozeAlarm,
                    onDismiss = viewModel::dismissAlarm,
                )
            }
        }

        setWindowSize(wasScreenOff)
    }

    /**
     * Configures window to display as an overlay above other apps.
     * Sets up transparency and flags for proper overlay behavior.
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun setupWindow() {
        // Set window type based on permission and API level
        if (Settings.canDrawOverlays(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION")
                window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
        }

        // Enable transparency for rounded corners
        window.attributes.format = PixelFormat.TRANSLUCENT
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Set window behavior flags
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    /**
     * Adjusts window size based on whether screen was off when alarm triggered.
     * Full screen if screen was off, compact overlay if screen was in use.
     */
    private fun setWindowSize(isFullScreen: Boolean) {
        val compactOverlayWidth = 0.8f  // 80% of screen width for compact mode
        val compactOverlayHeight = 0.45f // 45% of screen height for compact mode

        // Get screen dimensions using appropriate API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val width = (windowMetrics.bounds.width() * (if (isFullScreen) 1.0f else compactOverlayWidth)).toInt()
            val height = (windowMetrics.bounds.height() * (if (isFullScreen) 1.0f else compactOverlayHeight)).toInt()
            window.setLayout(width, height)
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)

            val width = (displayMetrics.widthPixels * (if (isFullScreen) 1.0f else compactOverlayWidth)).toInt()
            val height = (displayMetrics.heightPixels * (if (isFullScreen) 1.0f else compactOverlayHeight)).toInt()
            window.setLayout(width, height)
        }

        // Center the overlay on screen
        window.attributes = window.attributes.apply {
            gravity = android.view.Gravity.CENTER
        }
    }

    /**
     * Ensures screen turns on and keyguard is dismissed when alarm triggers.
     * Handles this differently based on API level.
     */
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

    /**
     * Processes alarm data from intent and initiates appropriate action
     * (start/snooze/dismiss) based on the intent's action.
     */
    private fun setupAlarm() {
        lifecycleScope.launch {
            // Extract alarm data from intent
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

            // Handle action from intent (e.g., from notification)
            when (intent?.action) {
                "DISMISS_ALARM" -> viewModel.dismissAlarm()
                "SNOOZE_ALARM" -> viewModel.snoozeAlarm()
                else -> viewModel.startAlarm()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.tag(TAG).d("AlarmActivity onNewIntent with action: ${intent.action}")
        setIntent(intent)
        setupAlarm()
    }

    /**
     * Prevents activity from being stopped unless explicitly dismissed.
     * Keeps alarm visible even if user tries to switch apps.
     */
    override fun onStop() {
        super.onStop()
        if (!isFinishing) {
            moveTaskToFront()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing) {
            moveTaskToFront()
        }
    }

    private fun moveTaskToFront() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION)
    }

    /**
     * Properly cleans up and finishes the activity when alarm is dismissed.
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun finishOverlay() {
        // Clear flags that might prevent proper dismissal
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }
}