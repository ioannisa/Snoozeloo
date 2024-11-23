package eu.anifantakis.snoozeloo.core.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmState
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import timber.log.Timber
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Implementation of AlarmScheduler that handles scheduling and canceling alarms using Android's AlarmManager.
 * Each alarm can have multiple occurrences (one per selected day of week).
 * Each day's occurrence uses a unique requestCode to allow independent scheduling/cancellation.
 */
class AlarmSchedulerImpl(
    private val context: Context
): AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private fun alarmScheduleAt(nextAlarmMillis: Long, intent: Intent, requestCode: Int = 0) {
        try {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextAlarmMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to schedule alarm for intent: ${intent.action}")
        }
    }

    /**
     * Schedules an alarm for each selected day of the week.
     * Cancels all existing occurrences first to prevent duplicates.
     */
    override fun schedule(alarm: Alarm) {
        // First cancel all existing occurrences for this alarm
        cancelAllOccurrences(alarm)

        if (!alarm.isEnabled || !alarm.selectedDays.hasAnyDaySelected()) {
            Timber.tag(TAG).d("Alarm ${alarm.id} not scheduled: enabled=${alarm.isEnabled}, hasSelectedDays=${alarm.selectedDays.hasAnyDaySelected()}")
            return
        }

        // Schedule for each selected day
        with(alarm.selectedDays) {
            if (mo) scheduleForDay(alarm, DayOfWeek.MONDAY)
            if (tu) scheduleForDay(alarm, DayOfWeek.TUESDAY)
            if (we) scheduleForDay(alarm, DayOfWeek.WEDNESDAY)
            if (th) scheduleForDay(alarm, DayOfWeek.THURSDAY)
            if (fr) scheduleForDay(alarm, DayOfWeek.FRIDAY)
            if (sa) scheduleForDay(alarm, DayOfWeek.SATURDAY)
            if (su) scheduleForDay(alarm, DayOfWeek.SUNDAY)
        }
    }

    /**
     * Schedules a single occurrence of the alarm for a specific day.
     */
    private fun scheduleForDay(
        item: Alarm,
        dayOfWeek: DayOfWeek,

    ) {
        val nextAlarmTime = calculateTimeUntilAlarmForDay(item.hour, item.minute, dayOfWeek)
        if (nextAlarmTime == Duration.ZERO) {
            Timber.tag(TAG).d("No valid next alarm time for alarm ${item.id} on $dayOfWeek")
            return
        }

        val alarmOccurrence = AlarmState(
            title = item.title,
            volume = item.volume,
            shouldVibrate = item.vibrate,
            ringtoneUri = item.ringtoneUri,
            alarmId = item.id,
            dayOfWeek = dayOfWeek.value, // the occurrence of that alarm in a given day of the week
            hour = item.hour,
            minute = item.minute
        )

        val intent = createAlarmIntent(alarmOccurrence)

        val nextAlarmMillis = System.currentTimeMillis() + nextAlarmTime.toMillis()
        alarmScheduleAt(nextAlarmMillis, intent)
        Timber.tag(TAG).d("Scheduled alarm ${item.id}_${dayOfWeek.value} for $dayOfWeek at ${nextAlarmTime.toMillis()}ms from now")
    }

    /**
     * Cancels all occurrences of an alarm across all days of the week.
     */
    private fun cancelAllOccurrences(item: Alarm) {
        // Cancel for each day of the week, regardless of whether it was selected
        DayOfWeek.entries.forEach { dayOfWeek ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                // Recreate the unique action
                action = "eu.anifantakis.snoozeloo.ALARM_${item.id}_${dayOfWeek.value}"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Timber.tag(TAG).d("Cancelled alarm ${item.id} for $dayOfWeek")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to cancel alarm ${item.id} for $dayOfWeek")
            }
        }
    }

    /**
     * Public cancel method that ensures all occurrences are cancelled.
     */
    override fun cancel(alarm: Alarm) {
        cancelAllOccurrences(alarm)
    }

    /**
     * Calculates the duration until the next alarm for a specific day of the week
     * @param hour The hour of the alarm (0-23)
     * @param minute The minute of the alarm (0-59)
     * @param targetDay The specific day of week to calculate for
     * @return Duration until the next occurrence of the alarm on the specified day
     */
    private fun calculateTimeUntilAlarmForDay(
        hour: Int,
        minute: Int,
        targetDay: DayOfWeek,
    ): Duration {
        val now = LocalDateTime.now()
        val alarmTime = LocalTime.of(hour, minute)
        var nextAlarmDateTime = LocalDateTime.of(now.toLocalDate(), alarmTime)

        val daysUntilTargetDay = ((targetDay.value - now.dayOfWeek.value + 7) % 7)
        nextAlarmDateTime = nextAlarmDateTime.plusDays(daysUntilTargetDay.toLong())

        if (nextAlarmDateTime.isBefore(now)) {
            nextAlarmDateTime = nextAlarmDateTime.plusWeeks(1)
        }

        return Duration.between(now, nextAlarmDateTime)
    }

    override fun scheduleSnooze(alarmState: AlarmState, snoozeDurationMinutes: Long) {
        val snoozeIntent = createAlarmIntent(alarmState, isSnooze = true)
        val snoozeMillis = System.currentTimeMillis() + snoozeDurationMinutes * 60 * 1000

        alarmScheduleAt(snoozeMillis, snoozeIntent, generateUniqueRequestCode())
        Timber.tag(TAG).d("Scheduled snooze alarm for ${alarmState.title} at $snoozeMillis")
    }

    override fun scheduleNextWeekOccurrence(alarmState: AlarmState) {
        val dayOfWeek = alarmState.dayOfWeek?.let { DayOfWeek.of(it) } ?: return

        val nextWeekTriggerTime = calculateTimeUntilAlarmForDay(
            alarmState.hour,
            alarmState.minute,
            dayOfWeek
        ).toMillis()

        val intent = createAlarmIntent(alarmState)

        alarmScheduleAt(nextWeekTriggerTime, intent)
        Timber.tag(TAG).d("Rescheduled alarm for next week on $dayOfWeek at $nextWeekTriggerTime")
    }

    private fun createAlarmIntent(alarmState: AlarmState, isSnooze: Boolean = false): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmState.alarmId)
            putExtra("TITLE", if (isSnooze) "${alarmState.title} (Snoozed)" else alarmState.title)
            putExtra("VOLUME", alarmState.volume)
            putExtra("VIBRATE", alarmState.shouldVibrate)
            putExtra("ALARM_URI", alarmState.ringtoneUri)
            putExtra("HOUR", alarmState.hour)
            putExtra("MINUTE", alarmState.minute)
            putExtra("DAY_OF_WEEK", alarmState.dayOfWeek)
            action = "eu.anifantakis.snoozeloo.ALARM_${alarmState.alarmId}_${alarmState.dayOfWeek}"
        }
    }

    private fun generateUniqueRequestCode(): Int {
        return System.currentTimeMillis().toInt()
    }

    companion object {
        private const val TAG = "AlarmSchedulerImpl"
    }
}



