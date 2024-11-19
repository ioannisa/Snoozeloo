package eu.anifantakis.snoozeloo.alarm.presentation.screens

import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UiText


data class AlarmUiState(
    val alarm: Alarm,
    val timeUntilNextAlarm: UiText,
    val hasChanges: Boolean = false
)