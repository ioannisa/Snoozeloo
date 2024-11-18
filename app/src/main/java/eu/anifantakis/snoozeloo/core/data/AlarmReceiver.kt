package eu.anifantakis.snoozeloo.core.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmDismissActivity
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import timber.log.Timber

/**
 * BroadcastReceiver responsible for handling alarm events.
 * This receiver is triggered when an alarm time is reached and handles:
 * 1. Scheduling the next alarm occurrence
 * 2. Showing the full-screen alarm activity
 * 3. Creating and showing notifications
 */
class AlarmReceiver : BroadcastReceiver() {
    companion object {
        // Notification channel ID for Android O and above
        const val CHANNEL_ID = "alarm_channel"
        // Unique identifier for the alarm notification
        const val NOTIFICATION_ID = 1
        // Tag for logging
        private const val TAG = "AlarmReceiver"
    }

    /**
     * Called when the alarm is triggered.
     * Handles the complete alarm flow including rescheduling, UI, and notifications.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Timber.tag(TAG).e("Received null context or intent")
            return
        }

        Timber.tag(TAG).d("AlarmReceiver: onReceive triggered with action: ${intent.action}")

        try {
            // First reschedule the next alarm before showing UI
            // This ensures we don't miss the next alarm if the app crashes during UI handling
            rescheduleNextAlarm(context, intent)

            startAlarmActivity(context, intent)
            handleNotification(context, intent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in AlarmReceiver")
            // Attempt to reschedule again if we failed earlier
            try {
                rescheduleNextAlarm(context, intent)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to reschedule alarm after error")
            }
        }
    }

    /**
     * Launches the full-screen alarm activity.
     * Uses appropriate flags to ensure proper activity stack handling.
     */
    private fun startAlarmActivity(context: Context, intent: Intent) {
        try {
            val fullScreenIntent = Intent(context, AlarmDismissActivity::class.java).apply {
                // Flags to ensure proper activity launch behavior
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  // Required for launching from background
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Clear any existing instances
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // Reuse existing instance if possible
                putExtras(intent) // Forward all alarm data
            }
            context.startActivity(fullScreenIntent)
            Timber.tag(TAG).d("Started AlarmActivity successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start AlarmActivity")
            // Fallback to notification if activity launch fails
            handleNotification(context, intent)
        }
    }

    /**
     * Handles the creation and display of the alarm notification.
     * Checks permissions and creates notification channel if needed.
     */
    private fun handleNotification(context: Context, intent: Intent) {
        if (!hasNotificationPermission(context)) {
            Timber.tag(TAG).w("Notification permission not granted")
            return
        }

        try {
            createNotificationChannel(context)
            showAlarmNotification(context, intent)
            Timber.tag(TAG).d("Notification handled successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to handle notification")
        }
    }

    /**
     * Checks if the app has permission to show notifications.
     * Required for Android 13 (API 33) and above.
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            Timber.tag(TAG).d("Notification permission check result: $hasPermission")
            hasPermission
        } else {
            Timber.tag(TAG).d("Device API < 33, notification permission not required")
            true
        }
    }

    /**
     * Creates the notification channel for Android O and above.
     * Sets up high-priority channel for alarm notifications.
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alarm Channel",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for Alarm notifications"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    setBypassDnd(true) // Allow alarms to bypass Do Not Disturb
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Timber.tag(TAG).d("Notification channel created successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to create notification channel")
            }
        }
    }

    /**
     * Creates and shows the alarm notification with full-screen intent
     * and action buttons for dismiss and snooze.
     */
    private fun showAlarmNotification(context: Context, intent: Intent) {
        try {
            val fullScreenPendingIntent = createFullScreenPendingIntent(context, intent)
            val dismissPendingIntent = createActionPendingIntent(context, intent, "DISMISS_ALARM", 1)
            val snoozePendingIntent = createActionPendingIntent(context, intent, "SNOOZE_ALARM", 2)

            val alarmTitle = intent.getStringExtra("TITLE") ?: run {
                Timber.tag(TAG).w("No title provided for alarm notification")
                "Alarm"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(alarmTitle)
                .setContentText("Wake up!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true) // Notification persists until explicitly dismissed
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .addAction(android.R.drawable.ic_popup_sync, "Snooze", snoozePendingIntent)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            Timber.tag(TAG).d("Alarm notification shown successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to show alarm notification")
        }
    }

    /**
     * Creates a PendingIntent for the full-screen alarm activity.
     * Uses current timestamp as request code to ensure uniqueness.
     */
    private fun createFullScreenPendingIntent(context: Context, intent: Intent): PendingIntent {
        val fullScreenIntent = Intent(context, AlarmDismissActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtras(intent)
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(), // Unique request code
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Creates a PendingIntent for notification actions (dismiss/snooze).
     * Each action gets a unique request code to prevent conflicts.
     */
    private fun createActionPendingIntent(
        context: Context,
        intent: Intent,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val actionIntent = Intent(context, AlarmDismissActivity::class.java).apply {
            this.action = action
            putExtras(intent)
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Schedules the next occurrence of the alarm based on selected days.
     * This is called immediately when an alarm triggers to ensure the next alarm is set.
     */
    private fun rescheduleNextAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("ALARM_ID")
        if (alarmId == null) {
            Timber.tag(TAG).e("Cannot reschedule: missing alarm ID")
            return
        }

        // Reconstruct the days configuration from intent extras
        val days = DaysOfWeek(
            mo = intent.getBooleanExtra("MO", false),
            tu = intent.getBooleanExtra("TU", false),
            we = intent.getBooleanExtra("WE", false),
            th = intent.getBooleanExtra("TH", false),
            fr = intent.getBooleanExtra("FR", false),
            sa = intent.getBooleanExtra("SA", false),
            su = intent.getBooleanExtra("SU", false)
        )

        if (!days.hasAnyDaySelected()) {
            Timber.tag(TAG).d("No days selected for alarm $alarmId, not rescheduling")
            return
        }

        // Validate time values
        val hour = intent.getIntExtra("HOUR", -1)
        val minute = intent.getIntExtra("MINUTE", -1)

        if (hour == -1 || minute == -1) {
            Timber.tag(TAG).e("Invalid hour/minute for alarm $alarmId: $hour:$minute")
            return
        }

        // Reconstruct the alarm object with all its properties
        val alarm = Alarm(
            id = alarmId,
            hour = hour,
            minute = minute,
            title = intent.getStringExtra("TITLE") ?: "",
            isEnabled = true,
            selectedDays = days,
            ringtoneTitle = intent.getStringExtra("ALARM_TITLE") ?: "",
            ringtoneUri = intent.getStringExtra("ALARM_URI"),
            volume = intent.getFloatExtra("VOLUME", 0.5f),
            vibrate = intent.getBooleanExtra("VIBRATE", true)
        )

        try {
            AlarmSchedulerImpl(context).schedule(alarm)
            Timber.tag(TAG).d("Successfully rescheduled alarm $alarmId for ${alarm.hour}:${alarm.minute}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to reschedule alarm $alarmId")
        }
    }
}