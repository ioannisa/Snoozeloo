package eu.anifantakis.snoozeloo.core.domain

import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmOccurrenceState

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
    fun cancelAlarmOccurrenceByIntentAction(action: String)
    fun scheduleSnooze(alarmOccurrenceState: AlarmOccurrenceState, snoozeDurationMinutes: Long)
    fun rescheduleOccurrenceForNextWeek(alarmOccurrenceState: AlarmOccurrenceState)
}