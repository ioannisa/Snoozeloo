package eu.anifantakis.snoozeloo.core.domain.util

import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDateTime

enum class Meridiem {
    AM,
    PM
}

object ClockUtils {
    fun createMinuteTickerFlow(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            val now = LocalDateTime.now()
            val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
            val delayMillis = Duration.between(now, nextMinute).toMillis()
            delay(delayMillis)
        }
    }

    fun get12HourFormatAndMeridiem(hour: Int): Pair<Int, Meridiem> {
        return when {
            hour == 0 -> Pair(12, Meridiem.AM)
            hour < 12 -> Pair(hour, Meridiem.AM)
            hour == 12 -> Pair(12, Meridiem.PM)
            else -> Pair(hour - 12, Meridiem.PM)
        }
    }

    fun shouldShowSleepAdvice(alarmHour: Int, alarmMinute: Int): Boolean {
        // Check if alarm is set between 4 AM and 10 AM
        return alarmHour in 4.. 9 || (alarmHour == 10 && alarmMinute == 0)
    }

    fun isMoreThanEightHoursAway(alarmHour: Int, alarmMinute: Int, daysOfWeek: DaysOfWeek): Boolean {
        val duration = calculateTimeUntilNextAlarm(alarmHour, alarmMinute, daysOfWeek)
        return duration.toHours() >= 8
    }
}