package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.domain.util.*
import eu.anifantakis.snoozeloo.core.presentation.designsystem.*
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.*
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import java.util.Locale

/**
 * A composable that displays a single alarm item with its details and controls.
 * Features include time display (12/24h format), enable/disable switch, day selection,
 * and sleep advice when appropriate.
 *
 * @param alarmUiState The UI state for this alarm
 * @param use24HourFormat Whether to display time in 24-hour format
 * @param onAlarmEvent Callback for alarm-related actions
 * @param modifier Optional modifier for the component
 */
@Composable
fun AppAlarmBox(
    alarmUiState: AlarmUiState,
    use24HourFormat: Boolean,
    onAlarmEvent: (AlarmsScreenAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val alarm = alarmUiState.alarm

    // Format time based on 24h preference
    val timeText = remember(alarm.hour, alarm.minute, use24HourFormat) {
        if (use24HourFormat) {
            String.format(Locale.ROOT,"%02d:%02d", alarm.hour, alarm.minute)
        } else {
            val (twelveHourFormat, _) = ClockUtils.get12HourFormatAndMeridiem(alarm.hour)
            String.format(Locale.ROOT, "%d:%02d", twelveHourFormat, alarm.minute)
        }
    }

    // Calculate AM/PM indicator for 12h format
    val (_, meridiem) = remember(alarm.hour) {
        ClockUtils.get12HourFormatAndMeridiem(alarm.hour)
    }

    // State for sleep advice visibility
    var showSleepAdvice by remember { mutableStateOf(false) }

    // Check if sleep advice should be shown
    LaunchedEffect(alarm.hour, alarm.minute) {
        showSleepAdvice = ClockUtils.shouldShowSleepAdvice(alarm.hour, alarm.minute) &&
                ClockUtils.isMoreThanEightHoursAway(alarm.hour, alarm.minute, alarm.selectedDays)
    }

    AppCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(UIConst.paddingSmall)
        ) {
            // Alarm title and enable/disable switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alarm.title.trim() == "") {
                    AppText16(
                        text = stringResource(R.string.default_alarm_title),
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    AppText16(text = alarm.title, fontWeight = FontWeight.W700)
                }
                AppSwitch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { enabled ->
                        onAlarmEvent(AlarmsScreenAction.EnableAlarmsScreen(alarm, enabled))
                    }
                )
            }

            // Time display with AM/PM indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(UIConst.paddingExtraSmall),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // Remove ripple for cleaner look
                ) {
                    onAlarmEvent(AlarmsScreenAction.SelectAlarmsScreen(alarm))
                }
            ) {
                AppText42(timeText)
                if (!use24HourFormat) {
                    AppText24(
                        meridiem.name,
                        fontWeight = FontWeight.W700,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // Time until next alarm trigger
            AppText14(
                text = alarmUiState.timeUntilNextAlarm.asString(),
                color = MaterialTheme.colorScheme.outline
            )

            // Day selection chips
            AppWeeklyChips(
                modifier = Modifier.fillMaxWidth(),
                selectedDays = alarm.selectedDays,
                onError = {
                    onAlarmEvent(AlarmsScreenAction.ShowDaysValidationError)
                },
                onSelectionChanged = { selectedDays ->
                    onAlarmEvent(AlarmsScreenAction.ChangeAlarmsScreenDays(alarm, selectedDays))
                }
            )

            // Animated sleep advice section
            AnimatedVisibility(
                visible = showSleepAdvice && alarm.selectedDays.hasAnyDaySelected(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                // Calculate suggested sleep time
                val suggestedSleepTime = remember(alarm.hour, alarm.minute, use24HourFormat) {
                    val suggestedSleepHour = if (alarm.hour>8) alarm.hour-8 else 24+(alarm.hour-8)
                    val meridiemName = if (suggestedSleepHour<=12 || suggestedSleepHour==24)
                        Meridiem.AM.name else Meridiem.PM.name

                    if (use24HourFormat) {
                        String.format(Locale.ROOT, "%02d:%02d", suggestedSleepHour%24, alarm.minute)
                    } else {
                        val (hour, _) = ClockUtils.get12HourFormatAndMeridiem(suggestedSleepHour)
                        String.format(Locale.ROOT, "%d:%02d %s", hour, alarm.minute, meridiemName)
                    }
                }

                AppText14(
                    text = stringResource(
                        id = R.string.get_eight_hours_of_sleep,
                        suggestedSleepTime
                    ),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Preview composable showing both 12-hour and 24-hour format versions of the alarm box
 * with interactive state for testing enable/disable and day selection.
 */
@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppAlarmBoxPreview() {
    var previewAlarm by remember {
        mutableStateOf(
            Alarm(
                id = "",
                hour = 8,
                minute = 0,
                isEnabled = true,
                title = "Sample Title",
                selectedDays = DaysOfWeek(mo = true)
            )
        )
    }

    val previewUiState by remember(previewAlarm) {
        mutableStateOf(
            AlarmUiState(
                alarm = previewAlarm,
                timeUntilNextAlarm = UiText.StringResource(R.string.alarm_in_1_hour)
            )
        )
    }

    SnoozelooTheme {
        AppBackground {
            Column(
                modifier = Modifier.padding(UIConst.padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preview both time formats
                AppAlarmBox(
                    alarmUiState = previewUiState,
                    use24HourFormat = false,
                    onAlarmEvent = { event ->
                        when (event) {
                            is AlarmsScreenAction.EnableAlarmsScreen -> {
                                previewAlarm = previewAlarm.copy(isEnabled = event.enabled)
                            }
                            is AlarmsScreenAction.ChangeAlarmsScreenDays -> {
                                previewAlarm = previewAlarm.copy(selectedDays = event.selectedDays)
                            }
                            else -> {}
                        }
                    }
                )

                AppAlarmBox(
                    alarmUiState = previewUiState,
                    use24HourFormat = true,
                    onAlarmEvent = { event ->
                        when (event) {
                            is AlarmsScreenAction.EnableAlarmsScreen -> {
                                previewAlarm = previewAlarm.copy(isEnabled = event.enabled)
                            }
                            is AlarmsScreenAction.ChangeAlarmsScreenDays -> {
                                previewAlarm = previewAlarm.copy(selectedDays = event.selectedDays)
                            }
                            else -> {}
                        }
                    }
                )
            }
        }
    }
}