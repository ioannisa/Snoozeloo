package eu.anifantakis.snoozeloo.core.domain.util

import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UiText
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

fun DaysOfWeek.toEnabledDaysList(): List<DayOfWeek> = buildList {
    if (mo) add(DayOfWeek.MONDAY)
    if (tu) add(DayOfWeek.TUESDAY)
    if (we) add(DayOfWeek.WEDNESDAY)
    if (th) add(DayOfWeek.THURSDAY)
    if (fr) add(DayOfWeek.FRIDAY)
    if (sa) add(DayOfWeek.SATURDAY)
    if (su) add(DayOfWeek.SUNDAY)
}

fun calculateTimeUntilNextAlarm(
    hour: Int,
    minute: Int,
    daysOfWeek: DaysOfWeek
): Duration {
    // If no days are selected, return zero duration
    if (!daysOfWeek.hasAnyDaySelected()) {
        return Duration.ZERO
    }

    val now = LocalDateTime.now()
    val alarmTime = LocalTime.of(hour, minute)

    // Start with today
    var nextAlarmDateTime = LocalDateTime.of(now.toLocalDate(), alarmTime)

    // If today's alarm has passed, start checking from tomorrow
    if (nextAlarmDateTime.isBefore(now)) {
        nextAlarmDateTime = nextAlarmDateTime.plusDays(1)
    }

    // Find the next enabled day (checking up to 7 days)
    var daysChecked = 0
    while (daysChecked < 7) {
        val dayOfWeek = nextAlarmDateTime.dayOfWeek
        if (when (dayOfWeek) {
                DayOfWeek.MONDAY -> daysOfWeek.mo
                DayOfWeek.TUESDAY -> daysOfWeek.tu
                DayOfWeek.WEDNESDAY -> daysOfWeek.we
                DayOfWeek.THURSDAY -> daysOfWeek.th
                DayOfWeek.FRIDAY -> daysOfWeek.fr
                DayOfWeek.SATURDAY -> daysOfWeek.sa
                DayOfWeek.SUNDAY -> daysOfWeek.su
            }) {
            return Duration.between(now, nextAlarmDateTime)
        }
        nextAlarmDateTime = nextAlarmDateTime.plusDays(1)
        daysChecked++
    }

    return Duration.ZERO
}

// Extension function to format Duration as a readable string
fun Duration.formatTimeUntil(): UiText.StringResource {
    val totalMinutes = toMinutes()
    val days = totalMinutes / (24 * 60)
    val remainingMinutes = totalMinutes % (24 * 60)
    val hours = remainingMinutes / 60
    val minutes = remainingMinutes % 60

    return when {
        // Less than an hour cases
        days == 0L && hours == 0L && minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_less_than_a_minute)
        days == 0L && hours == 0L ->
            UiText.StringResource(R.string.alarm_in_x_minutes, arrayOf(minutes.toInt()))

        // Hours only cases (no days)
        days == 0L && hours == 1L && minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_1_hour)
        days == 0L && hours == 1L ->
            UiText.StringResource(R.string.alarm_in_1_hour_and_x_minutes, arrayOf(minutes.toInt()))
        days == 0L && minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_x_hours, arrayOf(hours.toInt()))
        days == 0L ->
            UiText.StringResource(R.string.alarm_in_x_hours_and_x_minutes, arrayOf(hours.toInt(), minutes.toInt()))

        // Single day cases
        days == 1L && hours == 0L && minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_1_day)
        days == 1L && hours == 1L && minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_1_day_and_1_hour)
        days == 1L && hours == 0L ->
            UiText.StringResource(R.string.alarm_in_1_day_and_x_minutes, arrayOf(minutes.toInt()))
        days == 1L && minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_1_day_and_x_hours, arrayOf(hours.toInt()))
        days == 1L && hours == 1L ->
            UiText.StringResource(R.string.alarm_in_1_day_1_hour_and_x_minutes, arrayOf(minutes.toInt()))
        days == 1L ->
            UiText.StringResource(R.string.alarm_in_1_day_x_hours_and_x_minutes, arrayOf(hours.toInt(), minutes.toInt()))

        // Multiple days cases
        hours == 0L && minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_x_days, arrayOf(days.toInt()))
        hours == 1L && minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_x_days_and_1_hour, arrayOf(days.toInt()))
        hours == 0L ->
            UiText.StringResource(R.string.alarm_in_x_days_and_x_minutes, arrayOf(days.toInt(), minutes.toInt()))
        minutes == 0L ->
            UiText.StringResource(R.string.alarm_in_x_days_and_x_hours, arrayOf(days.toInt(), hours.toInt()))
        hours == 1L ->
            UiText.StringResource(R.string.alarm_in_x_days_1_hour_and_x_minutes, arrayOf(days.toInt(), minutes.toInt()))
        else ->
            UiText.StringResource(R.string.alarm_in_x_days_x_hours_and_x_minutes, arrayOf(days.toInt(), hours.toInt(), minutes.toInt()))
    }
}