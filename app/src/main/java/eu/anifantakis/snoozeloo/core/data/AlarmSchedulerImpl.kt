package eu.anifantakis.snoozeloo.core.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.core.domain.AlarmScheduler
import eu.anifantakis.snoozeloo.core.domain.util.calculateTimeUntilNextAlarm
import java.time.Duration

// https://www.youtube.com/watch?v=mWb_hEBLIqA
class AlarmSchedulerImpl(
    private val context: Context
): AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(item: Alarm) {
        if (!item.isEnabled || !item.selectedDays.hasAnyDaySelected()) {
            cancel(item)
            return
        }

        val nextAlarmTime = calculateTimeUntilNextAlarm(item.hour, item.minute, item.selectedDays)
        if (nextAlarmTime == Duration.ZERO) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
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
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + nextAlarmTime.toMillis(),
            PendingIntent.getBroadcast(
                context,
                item.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    override fun cancel(item: Alarm) {
        alarmManager.cancel(PendingIntent.getBroadcast(
            context,
            item.hashCode(),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
    }
}