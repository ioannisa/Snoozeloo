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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmDismissActivity
import eu.anifantakis.snoozeloo.core.data.database.AlarmSchedulerIntent
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Handles alarm events and manages the alarm lifecycle including scheduling, UI presentation, and notifications.
 *
 * Key responsibilities:
 * - Schedules next alarm occurrence
 * - Launches full-screen alarm activity
 * - Creates and manages alarm notifications
 */
class AlarmReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1
        private const val TAG = "AlarmReceiver"
    }

    private val alarmScheduler: AlarmScheduler by inject()

    /**
     * Processes incoming alarm events and manages the alarm response flow.
     *
     * @param context The Context in which the receiver is running
     * @param intent The Intent being received, containing alarm details
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

        // find out if the screen was turned off at the time of broadcast
        // we need that because we show full screen overlay if the screen is off
        // otherwise we show the compact version of the overlay (makes ui better I think)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wasScreenOff = !powerManager.isInteractive

        Timber.tag(TAG).d("AlarmReceiver: onReceive triggered with action: ${intent.action}")

        // If we are in snooze alarm, then we cannot reschedule for next week's occurrence of this alarm,
        // we reschedule only on main alarm
        val isSnooze = intent.getBooleanExtra("IS_SNOOZE", false)
        if (!isSnooze) {
            alarmScheduler.cancelAlarmOccurrenceByIntentAction(intent.action.toString())

            val alarmState = AlarmSchedulerIntent.toAlarmOccurrenceState(intent)
            alarmScheduler.rescheduleOccurrenceForNextWeek(alarmState)
        }

        // launch alarm activity and handle notification
        startAlarmActivity(context, intent, wasScreenOff, isSnooze)

        // if we allow for notifications, also show the notification
        // however notification showing is not vital, as the overlay screen doesn't depend on notification existence
        handleNotification(context, intent)
    }

    /**
     * Launches the alarm dismissal activity with appropriate flags and extras.
     */
    private fun startAlarmActivity(context: Context, intent: Intent, wasScreenOff: Boolean, isSnooze: Boolean) {
        try {
            val fullScreenIntent = Intent(context, AlarmDismissActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(AlarmDismissActivity.EXTRA_SCREEN_WAS_OFF, wasScreenOff)
                putExtra(AlarmDismissActivity.EXTRA_IS_SNOOZE, isSnooze)
                putExtras(intent)
            }
            context.startActivity(fullScreenIntent)
            Timber.tag(TAG).d("Started AlarmActivity successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start AlarmActivity")
            handleNotification(context, intent)
        }
    }

    /**
     * Creates and displays the alarm notification if permissions are granted.
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
     * Checks for notification permission on Android 13 and above.
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED.also {
                Timber.tag(TAG).d("Notification permission check result: $it")
            }
        } else {
            true
        }
    }

    /**
     * Creates a high-priority notification channel for Android O and above.
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
                    setBypassDnd(true)
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
     * Displays the alarm notification with full-screen intent and action buttons.
     */
    private fun showAlarmNotification(context: Context, intent: Intent) {
        try {
            val fullScreenPendingIntent = createFullScreenPendingIntent(context, intent)
            val dismissPendingIntent = createActionPendingIntent(context, intent, "DISMISS_ALARM", 1)
            val snoozePendingIntent = createActionPendingIntent(context, intent, "SNOOZE_ALARM", 2)

            val alarmTitle = intent.getStringExtra("TITLE") ?: getString(context, R.string.default_alarm_title)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(getString(context, R.string.app_name))
                .setContentText(alarmTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.dismiss_button_text), dismissPendingIntent)
                .addAction(android.R.drawable.ic_popup_sync,
                    context.getString(R.string.snooze_button_text), snoozePendingIntent)
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
     */
    private fun createFullScreenPendingIntent(context: Context, intent: Intent): PendingIntent {
        val fullScreenIntent = Intent(context, AlarmDismissActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtras(intent)
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Creates a PendingIntent for notification actions.
     *
     * @param action The action identifier ("DISMISS_ALARM" or "SNOOZE_ALARM")
     * @param requestCode Unique identifier for the PendingIntent
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