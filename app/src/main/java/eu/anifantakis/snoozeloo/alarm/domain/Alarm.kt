package eu.anifantakis.snoozeloo.alarm.domain

data class Alarm(
    val id: String,
    val hour: Int,
    val minute: Int,
    val title: String,
    val isEnabled: Boolean,
    val selectedDays: DaysOfWeek,
    val ringtoneTitle: String = "",
    val ringtoneUri: String? = "",
    val volume: Float = 0.5f,
    val vibrate: Boolean = true,
    val temporary: Boolean = true
)

data class DaysOfWeek(
    val mo: Boolean = false,
    val tu: Boolean = false,
    val we: Boolean = false,
    val th: Boolean = false,
    val fr: Boolean = false,
    val sa: Boolean = false,
    val su: Boolean = false
) {
    fun hasAnyDaySelected(): Boolean =
        listOf(mo, tu, we, th, fr, sa, su).any { it }
}
