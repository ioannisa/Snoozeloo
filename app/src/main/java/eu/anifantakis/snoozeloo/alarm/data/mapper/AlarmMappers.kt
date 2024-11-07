package eu.anifantakis.snoozeloo.alarm.data.mapper

import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.Meridiem
import eu.anifantakis.snoozeloo.core.data.database.entity.AlarmEntity


fun AlarmEntity.toAlarm(): Alarm {
    // Helper function to convert 24hr format to 12hr format and determine meridiem
    fun get12HourFormatAndMeridiem(hour: Int): Pair<Int, Meridiem> {
        return when {
            hour == 0 -> Pair(12, Meridiem.AM)
            hour < 12 -> Pair(hour, Meridiem.AM)
            hour == 12 -> Pair(12, Meridiem.PM)
            else -> Pair(hour - 12, Meridiem.PM)
        }
    }

    // Convert hour and minute to formatted time string
    val (twelveHour, meridiem) = get12HourFormatAndMeridiem(hour)
    val timeString = String.format("%d:%02d", twelveHour, minute)

    // Create days of week map
    val daysMap = mapOf(
        "mo" to mo,
        "tu" to tu,
        "we" to we,
        "th" to th,
        "fr" to fr,
        "sa" to sa,
        "su" to su
    )

    // Calculate time until alarm and suggested sleep time
    val timeUntilAlarm = calculateTimeUntilAlarm(hour, minute)
    val suggestedSleepTime = calculateSuggestedSleepTime(hour, minute)

    return Alarm(
        id = id,
        time = timeString,
        meridiem = meridiem,
        isEnabled = enabled,
        selectedDays = daysMap,
        timeUntilAlarm = timeUntilAlarm,
        suggestedSleepTime = suggestedSleepTime
    )
}

fun Alarm.toEntity(): AlarmEntity {
    // Helper function to convert 12hr format and meridiem to 24hr format
    fun get24HourFormat(timeStr: String, meridiem: Meridiem): Pair<Int, Int> {
        val (hourStr, minuteStr) = timeStr.split(":")
        var hour = hourStr.toInt()
        val minute = minuteStr.toInt()

        hour = when {
            meridiem == Meridiem.AM && hour == 12 -> 0
            meridiem == Meridiem.AM -> hour
            meridiem == Meridiem.PM && hour == 12 -> 12
            meridiem == Meridiem.PM -> hour + 12
            else -> hour
        }

        return Pair(hour, minute)
    }

    // Convert time string and meridiem back to 24-hour format
    val (hour, minute) = get24HourFormat(time, meridiem)

    return AlarmEntity(
        id = id,
        hour = hour,
        minute = minute,
        enabled = isEnabled,
        // Extract individual day values from the map, defaulting to false if not found
        mo = selectedDays["mo"] ?: false,
        tu = selectedDays["tu"] ?: false,
        we = selectedDays["we"] ?: false,
        th = selectedDays["th"] ?: false,
        fr = selectedDays["fr"] ?: false,
        sa = selectedDays["sa"] ?: false,
        su = selectedDays["su"] ?: false,
        // Assuming these fields exist in AlarmEntity - adjust as needed
        title = "",  // You might want to derive this from somewhere
        ringtone = "", // Default value
        volume = 0f,  // Default value
        vibrate = false // Default value
    )
}

// Helper functions from before
private fun calculateTimeUntilAlarm(hour: Int, minute: Int): String {
    // Implement your time calculation logic here
    return "Alarm in ${hour}h ${minute}min"
}

private fun calculateSuggestedSleepTime(hour: Int, minute: Int): String {
    // Implement your sleep time calculation logic here
    val sleepHour = if (hour < 8) hour + 16 else hour - 8
    return String.format("%02d:%02d", sleepHour, minute)
}