package eu.anifantakis.snoozeloo.core.domain.util

import eu.anifantakis.snoozeloo.R
import java.time.Duration

// Extension function to format Duration as a readable string

// We use ResourceString rather than UiText, because we want to avoid linking to a class making use
// of android system from this "domain class".
fun Duration.formatTimeUntil(): Pair<Int, Array<Any>> {
    val totalMinutes = toMinutes()
    val days = totalMinutes / (24 * 60)
    val remainingMinutes = totalMinutes % (24 * 60)
    val hours = remainingMinutes / 60
    val minutes = remainingMinutes % 60

    return when {
        // Less than an hour cases
        days == 0L && hours == 0L && minutes == 0L ->
            Pair(R.string.alarm_in_less_than_a_minute, emptyArray())
        days == 0L && hours == 0L ->
            Pair(R.string.alarm_in_x_minutes, arrayOf(minutes.toInt()))

        // Hours only cases (no days)
        days == 0L && hours == 1L && minutes == 0L ->
            Pair(R.string.alarm_in_1_hour, emptyArray())  // Add emptyArray()
        days == 0L && hours == 1L ->
            Pair(R.string.alarm_in_1_hour_and_x_minutes, arrayOf(minutes.toInt()))
        days == 0L && minutes == 0L ->
            Pair(R.string.alarm_in_x_hours, arrayOf(hours.toInt()))
        days == 0L ->
            Pair(R.string.alarm_in_x_hours_and_x_minutes, arrayOf(hours.toInt(), minutes.toInt()))

        // Single day cases
        days == 1L && hours == 0L && minutes == 0L ->
            Pair(R.string.alarm_in_1_day, emptyArray())  // Add emptyArray()
        days == 1L && hours == 1L && minutes == 0L ->
            Pair(R.string.alarm_in_1_day_and_1_hour, emptyArray())  // Add emptyArray()
        days == 1L && hours == 0L ->
            Pair(R.string.alarm_in_1_day_and_x_minutes, arrayOf(minutes.toInt()))
        days == 1L && minutes == 0L ->
            Pair(R.string.alarm_in_1_day_and_x_hours, arrayOf(hours.toInt()))
        days == 1L && hours == 1L ->
            Pair(R.string.alarm_in_1_day_1_hour_and_x_minutes, arrayOf(minutes.toInt()))
        days == 1L ->
            Pair(R.string.alarm_in_1_day_x_hours_and_x_minutes, arrayOf(hours.toInt(), minutes.toInt()))

        // Multiple days cases
        hours == 0L && minutes == 0L ->
            Pair(R.string.alarm_in_x_days, arrayOf(days.toInt()))
        hours == 1L && minutes == 0L ->
            Pair(R.string.alarm_in_x_days_and_1_hour, arrayOf(days.toInt()))
        hours == 0L ->
            Pair(R.string.alarm_in_x_days_and_x_minutes, arrayOf(days.toInt(), minutes.toInt()))
        minutes == 0L ->
            Pair(R.string.alarm_in_x_days_and_x_hours, arrayOf(days.toInt(), hours.toInt()))
        hours == 1L ->
            Pair(R.string.alarm_in_x_days_1_hour_and_x_minutes, arrayOf(days.toInt(), minutes.toInt()))
        else ->
            Pair(R.string.alarm_in_x_days_x_hours_and_x_minutes, arrayOf(days.toInt(), hours.toInt(), minutes.toInt()))
    }
}