package eu.anifantakis.snoozeloo.core.domain.util

import eu.anifantakis.snoozeloo.alarm.domain.Alarm

fun Alarm.timeAsString(): String {
    // Convert 24-hour format to 12-hour format
    val displayHour = when (hour) {
        0 -> 12
        in 13..23 -> hour - 12
        else -> hour
    }

    // Pad single digit numbers with leading zero
    return String.format("%02d:%02d", displayHour, minute)
}