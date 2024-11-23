package eu.anifantakis.snoozeloo.core.data.database

import android.content.Context
import android.content.Intent
import eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss.AlarmOccurrenceState

class AlarmSchedulerIntent(packageContext: Context, cls: Class<*>) : Intent(packageContext, cls) {

    companion object {
        const val CATEGORY_ALARM = "eu.anifantakis.snoozeloo.category.SCHEDULED_ALARM"

        private const val EXTRA_ALARM_ID = "ALARM_ID"
        private const val EXTRA_TITLE = "TITLE"
        private const val EXTRA_VOLUME = "VOLUME"
        private const val EXTRA_VIBRATE = "VIBRATE"
        private const val EXTRA_ALARM_URI = "ALARM_URI"
        private const val EXTRA_HOUR = "HOUR"
        private const val EXTRA_MINUTE = "MINUTE"
        private const val EXTRA_IS_SNOOZE = "IS_SNOOZE"
        private const val EXTRA_DAY_OF_WEEK = "DAY_OF_WEEK"
        private const val ACTION = "eu.anifantakis.snoozeloo.ALARM_"

        fun toAlarmOccurrenceState(intent: Intent): AlarmOccurrenceState {
            return AlarmOccurrenceState(
                isPlaying = false,
                title = intent.getStringExtra(EXTRA_TITLE) ?: "",
                volume = intent.getFloatExtra(EXTRA_VOLUME, 1.0f),
                shouldVibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true),
                ringtoneUri = intent.getStringExtra(EXTRA_ALARM_URI) ?: "",
                alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "",
                dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1),
                hour = intent.getIntExtra(EXTRA_HOUR, 0),
                minute = intent.getIntExtra(EXTRA_MINUTE, 0)
            )
        }
    }

    fun setAlarmData(
        alarmOccurrenceState: AlarmOccurrenceState,
        isSnooze: Boolean = false
    ) = apply {
        putExtra(EXTRA_ALARM_ID, alarmOccurrenceState.alarmId)
        putExtra(EXTRA_TITLE, if (isSnooze) "${alarmOccurrenceState.title} (Snoozed)" else alarmOccurrenceState.title)
        putExtra(EXTRA_VOLUME, alarmOccurrenceState.volume)
        putExtra(EXTRA_VIBRATE, alarmOccurrenceState.shouldVibrate)
        putExtra(EXTRA_ALARM_URI, alarmOccurrenceState.ringtoneUri)
        putExtra(EXTRA_HOUR, alarmOccurrenceState.hour)
        putExtra(EXTRA_MINUTE, alarmOccurrenceState.minute)
        putExtra(EXTRA_IS_SNOOZE, isSnooze)
        putExtra(EXTRA_DAY_OF_WEEK, alarmOccurrenceState.dayOfWeek)
        action = "${ACTION}${alarmOccurrenceState.alarmId}_${alarmOccurrenceState.dayOfWeek}"
        addCategory(CATEGORY_ALARM)
    }

    fun toAlarmOccurrenceState(): AlarmOccurrenceState {
        return AlarmOccurrenceState(
            isPlaying = false,
            title = this.getTitle(),
            volume = this.getVolume(),
            shouldVibrate = this.shouldVibrate(),
            ringtoneUri = this.getAlarmUri(),
            alarmId = this.getAlarmId(),
            dayOfWeek = this.getDayOfWeek(),
            hour = this.getHour(),
            minute = this.getMinute()
        )
    }

    fun getAlarmId(): String = getStringExtra(EXTRA_ALARM_ID) ?: ""
    fun getTitle(): String = getStringExtra(EXTRA_TITLE) ?: ""
    fun getVolume(): Float = getFloatExtra(EXTRA_VOLUME, 1.0f)
    fun shouldVibrate(): Boolean = getBooleanExtra(EXTRA_VIBRATE, true)
    fun getAlarmUri(): String = getStringExtra(EXTRA_ALARM_URI) ?: ""
    fun getHour(): Int = getIntExtra(EXTRA_HOUR, 0)
    fun getMinute(): Int = getIntExtra(EXTRA_MINUTE, 0)
    fun isSnooze(): Boolean = getBooleanExtra(EXTRA_IS_SNOOZE, false)
    fun getDayOfWeek(): Int = getIntExtra(EXTRA_DAY_OF_WEEK, -1)
}

