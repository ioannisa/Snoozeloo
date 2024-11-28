package eu.anifantakis.snoozeloo.alarm.domain

import android.os.Parcelable
import eu.anifantakis.navhelper.serialization.StringSanitizer
import eu.anifantakis.snoozeloo.alarm.domain.datasource.AlarmId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Alarm(
    val id: AlarmId,
    val hour: Int,
    val minute: Int,
    val title: String,
    val isEnabled: Boolean,
    val selectedDays: DaysOfWeek,
    @Serializable(with = StringSanitizer::class)
    val ringtoneTitle: String = "",
    @Serializable(with = StringSanitizer::class)
    val ringtoneUri: String? = "",
    val volume: Float = 0.5f,
    val vibrate: Boolean = true,
    val isNewAlarm: Boolean = true
) : Parcelable

@Serializable
@Parcelize
data class DaysOfWeek(
    val mo: Boolean = false,
    val tu: Boolean = false,
    val we: Boolean = false,
    val th: Boolean = false,
    val fr: Boolean = false,
    val sa: Boolean = false,
    val su: Boolean = false
) : Parcelable {
    fun hasAnyDaySelected(): Boolean =
        listOf(mo, tu, we, th, fr, sa, su).any { it }
}
