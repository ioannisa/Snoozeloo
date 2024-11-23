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
import eu.anifantakis.snoozeloo.core.data.database.AlarmSchedulerIntent
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * BroadcastReceiver responsible for handling alarm events.
 * This receiver is triggered when an alarm time is reached and handles:
 * 1. Scheduling the next alarm occurrence
 * 2. Showing the full-screen alarm activity
 * 3. Creating and showing notifications
 */
class AlarmReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        // Notification channel ID for Android O and above
        const val CHANNEL_ID = "alarm_channel"
        // Unique identifier for the alarm notification
        const val NOTIFICATION_ID = 1
        // Tag for logging
        private const val TAG = "AlarmReceiver"
    }

    private val alarmScheduler: AlarmScheduler by inject()

    /**
     * Called when the alarm is triggered via device boot or incoming alarm
     * Handles the complete alarm flow including rescheduling, UI, and notifications.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Timber.tag(TAG).e("Received null context or intent")
            return
        }

        if (!intent.hasCategory(AlarmSchedulerIntent.CATEGORY_ALARM)) {
            Timber.e("AlarmReceiver", "Received intent without alarm category")
            return
        }

        Timber.tag(TAG).d("AlarmReceiver: onReceive triggered with action: ${intent.action}")

        if (!intent.getBooleanExtra("IS_SNOOZE", false)) {
            alarmScheduler.cancelAlarmOccurrenceByIntentAction(intent.action.toString())

            val alarmState = AlarmSchedulerIntent.toAlarmOccurrenceState(intent)
            alarmScheduler.rescheduleOccurrenceForNextWeek(alarmState)
        }

        startAlarmActivity(context, intent)
        handleNotification(context, intent)
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
}