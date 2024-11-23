package eu.anifantakis.snoozeloo.core.domain

import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmState

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
    fun scheduleSnooze(alarmState: AlarmState, snoozeDurationMinutes: Long)
    fun scheduleNextWeekOccurrence(alarmState: AlarmState)
}