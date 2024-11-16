package eu.anifantakis.snoozeloo.core.domain

import eu.anifantakis.snoozeloo.alarm.domain.Alarm

interface AlarmScheduler {
    fun schedule(item: Alarm)
    fun cancel(item: Alarm)
}