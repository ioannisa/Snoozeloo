package eu.anifantakis.snoozeloo.alarm.domain


//data class Alarm(
//    val id: String,
//    val hour: Int,
//    val minute: Int,
//    val enabled: Boolean,
//    val title: String,
//    val ringtone: String,
//    val volume: Float,
//    val vibrate: Boolean,
//    val days: DaysOfWeek
//)
//
//data class DaysOfWeek(
//    val mo: Boolean = false,
//    val tu: Boolean = false,
//    val we: Boolean = false,
//    val th: Boolean = false,
//    val fr: Boolean = false,
//    val sa: Boolean = false,
//    val su: Boolean = false
//)


//typealias DaysOfWeek = Map<String, Boolean>

enum class Meridiem {
    AM,
    PM
}

data class Alarm(
    val id: String,
    val hour: Int,
    val minute: Int,
    val meridiem: Meridiem,
    val isEnabled: Boolean,
    val selectedDays: DaysOfWeek,
    val timeUntilAlarm: String,
    val suggestedSleepTime: String
)

data class DaysOfWeek(
    val mo: Boolean = false,
    val tu: Boolean = false,
    val we: Boolean = false,
    val th: Boolean = false,
    val fr: Boolean = false,
    val sa: Boolean = false,
    val su: Boolean = false
)
