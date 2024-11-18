package eu.anifantakis.snoozeloo.core.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import timber.log.Timber
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

private const val TAG = "AlarmSchedulerImpl"

/**
 * Implementation of AlarmScheduler that handles scheduling and canceling alarms using Android's AlarmManager.
 * Each alarm can have multiple occurrences (one per selected day of week).
 * Each day's occurrence uses a unique requestCode to allow independent scheduling/cancellation.
 */
class AlarmSchedulerImpl(
    private val context: Context
): AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Generates a unique request code for each alarm and day combination.
     * This ensures each day's alarm can be managed independently.
     */
    private fun generateRequestCode(alarmId: String, dayOfWeek: DayOfWeek): Int {
        return "${alarmId}_${dayOfWeek.value}".hashCode()
    }

    /**
     * Schedules an alarm for each selected day of the week.
     * Cancels all existing occurrences first to prevent duplicates.
     */
    override fun schedule(item: Alarm) {
        // First cancel all existing occurrences for this alarm
        cancelAllOccurrences(item)

        if (!item.isEnabled || !item.selectedDays.hasAnyDaySelected()) {
            Timber.tag(TAG).d("Alarm ${item.id} not scheduled: enabled=${item.isEnabled}, hasSelectedDays=${item.selectedDays.hasAnyDaySelected()}")
            return
        }

        // Schedule for each selected day
        with(item.selectedDays) {
            if (mo) scheduleForDay(item, DayOfWeek.MONDAY)
            if (tu) scheduleForDay(item, DayOfWeek.TUESDAY)
            if (we) scheduleForDay(item, DayOfWeek.WEDNESDAY)
            if (th) scheduleForDay(item, DayOfWeek.THURSDAY)
            if (fr) scheduleForDay(item, DayOfWeek.FRIDAY)
            if (sa) scheduleForDay(item, DayOfWeek.SATURDAY)
            if (su) scheduleForDay(item, DayOfWeek.SUNDAY)
        }
    }

    /**
     * Schedules a single occurrence of the alarm for a specific day.
     */
    private fun scheduleForDay(item: Alarm, dayOfWeek: DayOfWeek) {
        val nextAlarmTime = calculateTimeUntilAlarmForDay(item.hour, item.minute, dayOfWeek)
        if (nextAlarmTime == Duration.ZERO) {
            Timber.tag(TAG).d("No valid next alarm time for alarm ${item.id} on $dayOfWeek")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", item.id)
            putExtra("TITLE", item.title)
            putExtra("VOLUME", item.volume)
            putExtra("VIBRATE", item.vibrate)
            putExtra("ALARM_TITLE", item.ringtoneTitle)
            putExtra("ALARM_URI", item.ringtoneUri)
            putExtra("HOUR", item.hour)
            putExtra("MINUTE", item.minute)
            putExtra("MO", item.selectedDays.mo)
            putExtra("TU", item.selectedDays.tu)
            putExtra("WE", item.selectedDays.we)
            putExtra("TH", item.selectedDays.th)
            putExtra("FR", item.selectedDays.fr)
            putExtra("SA", item.selectedDays.sa)
            putExtra("SU", item.selectedDays.su)
            putExtra("DAY_OF_WEEK", dayOfWeek.value) // Store which day this occurrence is for
        }

        val requestCode = generateRequestCode(item.id, dayOfWeek)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + nextAlarmTime.toMillis(),
                pendingIntent
            )
            Timber.tag(TAG).d("Scheduled alarm ${item.id} for $dayOfWeek (code: $requestCode) at ${nextAlarmTime.toMillis()}ms from now")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to schedule alarm ${item.id} for $dayOfWeek")
        }
    }

    /**
     * Cancels all occurrences of an alarm across all days of the week.
     */
    private fun cancelAllOccurrences(item: Alarm) {
        // Cancel for each day of the week, regardless of whether it was selected
        DayOfWeek.values().forEach { dayOfWeek ->
            val requestCode = generateRequestCode(item.id, dayOfWeek)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Timber.tag(TAG).d("Cancelled alarm ${item.id} for $dayOfWeek (code: $requestCode)")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to cancel alarm ${item.id} for $dayOfWeek")
            }
        }
    }

    /**
     * Public cancel method that ensures all occurrences are cancelled.
     */
    override fun cancel(item: Alarm) {
        cancelAllOccurrences(item)
    }

    /**
     * Calculates the duration until the next alarm for a specific day of the week
     * @param hour The hour of the alarm (0-23)
     * @param minute The minute of the alarm (0-59)
     * @param targetDay The specific day of week to calculate for
     * @return Duration until the next occurrence of the alarm on the specified day
     */
    fun calculateTimeUntilAlarmForDay(
        hour: Int,
        minute: Int,
        targetDay: DayOfWeek
    ): Duration {
        val now = LocalDateTime.now()
        val alarmTime = LocalTime.of(hour, minute)
        var nextAlarmDateTime = LocalDateTime.of(now.toLocalDate(), alarmTime)

        // If we're calculating for today and the time has already passed,
        // or if we're calculating for a future day,
        // keep adding days until we reach the target day
        while (nextAlarmDateTime.dayOfWeek != targetDay ||
            (nextAlarmDateTime.dayOfWeek == targetDay && nextAlarmDateTime.isBefore(now))) {
            nextAlarmDateTime = nextAlarmDateTime.plusDays(1)
        }

        return Duration.between(now, nextAlarmDateTime)
    }
}