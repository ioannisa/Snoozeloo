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
import eu.anifantakis.snoozeloo.AlarmActivity
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        Timber.tag(TAG).d("AlarmReceiver: onReceive triggered with action: ${intent.action}")

        try {
            startAlarmActivity(context, intent)
            handleNotification(context, intent)
            rescheduleNextAlarm(context, intent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in AlarmReceiver")
        }
    }

    private fun startAlarmActivity(context: Context, intent: Intent) {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtras(intent)
        }
        context.startActivity(fullScreenIntent)
        Timber.tag(TAG).d("Started AlarmActivity")
    }

    private fun handleNotification(context: Context, intent: Intent) {
        if (!hasNotificationPermission(context)) {
            Timber.tag(TAG).d("Notification permission not granted")
            return
        }

        createNotificationChannel(context)
        showAlarmNotification(context, intent)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }
    }

    private fun showAlarmNotification(context: Context, intent: Intent) {
        val fullScreenPendingIntent = createFullScreenPendingIntent(context, intent)
        val dismissPendingIntent = createActionPendingIntent(context, intent, "DISMISS_ALARM", 1)
        val snoozePendingIntent = createActionPendingIntent(context, intent, "SNOOZE_ALARM", 2)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(intent.getStringExtra("TITLE") ?: "Alarm")
            .setContentText("Wake up!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze", snoozePendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createFullScreenPendingIntent(context: Context, intent: Intent): PendingIntent {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtras(intent)
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createActionPendingIntent(
        context: Context,
        intent: Intent,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val actionIntent = Intent(context, AlarmActivity::class.java).apply {
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

    private fun rescheduleNextAlarm(context: Context, intent: Intent) {
        val days = DaysOfWeek(
            mo = intent.getBooleanExtra("MO", false),
            tu = intent.getBooleanExtra("TU", false),
            we = intent.getBooleanExtra("WE", false),
            th = intent.getBooleanExtra("TH", false),
            fr = intent.getBooleanExtra("FR", false),
            sa = intent.getBooleanExtra("SA", false),
            su = intent.getBooleanExtra("SU", false)
        )

        val alarmId = intent.getStringExtra("ALARM_ID") ?: return

        val alarm = Alarm(
            id = alarmId,
            hour = intent.getIntExtra("HOUR", 0),
            minute = intent.getIntExtra("MINUTE", 0),
            title = intent.getStringExtra("TITLE") ?: "",
            isEnabled = true,
            selectedDays = days,
            ringtoneTitle = intent.getStringExtra("ALARM_TITLE") ?: "",
            ringtoneUri = intent.getStringExtra("ALARM_URI"),
            volume = intent.getFloatExtra("VOLUME", 0.5f),
            vibrate = intent.getBooleanExtra("VIBRATE", true)
        )

        AlarmSchedulerImpl(context).schedule(alarm)
    }
}