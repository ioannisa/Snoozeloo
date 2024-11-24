package eu.anifantakis.snoozeloo.core.domain.util

import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
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

    //return Duration.ofSeconds(5L)

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
                else -> return Duration.ZERO
            }) {
            return Duration.between(now, nextAlarmDateTime)
        }
        nextAlarmDateTime = nextAlarmDateTime.plusDays(1)
        daysChecked++
    }

    return Duration.ZERO
}