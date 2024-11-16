package eu.anifantakis.snoozeloo.alarm.data.mapper

import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.core.data.database.entity.AlarmEntity

fun AlarmEntity.toAlarm(): Alarm {
    val daysOfWeek = DaysOfWeek(
        mo = mo,
        tu = tu,
        we = we,
        th = th,
        fr = fr,
        sa = sa,
        su = su
    )

    return Alarm(
        id = id,
        hour = hour,
        minute = minute,
        isEnabled = enabled,
        title = title,
        selectedDays = daysOfWeek,
        ringtoneTitle = ringtoneTitle,
        ringtoneUri = ringtoneUri,
        volume = volume,
        vibrate = vibrate,
        temporary = temporary
    )
}

fun Alarm.toEntity(): AlarmEntity {
    return AlarmEntity(
        id = id,
        hour = hour,
        minute = minute,
        enabled = isEnabled,
        title = title,
        // Extract individual day values from the map, defaulting to false if not found
        mo = selectedDays.mo,
        tu = selectedDays.tu,
        we = selectedDays.we,
        th = selectedDays.th,
        fr = selectedDays.fr,
        sa = selectedDays.sa,
        su = selectedDays.su,
        ringtoneTitle = ringtoneTitle,
        ringtoneUri = ringtoneUri,
        volume = volume,
        vibrate = vibrate,
        temporary = temporary
    )
}