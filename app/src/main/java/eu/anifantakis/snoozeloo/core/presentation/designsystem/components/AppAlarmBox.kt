package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme

enum class Meridiem {
    AM,
    PM
}

data class AlarmState(
    val time: String,
    val meridiem: Meridiem,
    val isEnabled: Boolean,
    val selectedDays: Map<String, Boolean>,
    val timeUntilAlarm: String,
    val suggestedSleepTime: String
)

sealed class AlarmEvent {
    data class OnEnabledChanged(val enabled: Boolean) : AlarmEvent()
    data class OnDaysChanged(val selectedDays: Map<String, Boolean>) : AlarmEvent()
}

@Composable
fun AppAlarmBox(
    initialState: AlarmState,
    onAlarmEvent: (AlarmEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(UIConst.paddingSmall)
        ) {
            // Header with title and switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText16("Wake Up")
                AppSwitch(
                    initialState = initialState.isEnabled,
                    onCheckedChange = { enabled ->
                        onAlarmEvent(AlarmEvent.OnEnabledChanged(enabled))
                    }
                )
            }

            // Time display
            Row(
                horizontalArrangement = Arrangement.spacedBy(UIConst.paddingExtraSmall),
                verticalAlignment = Alignment.Bottom
            ) {
                AppText42(initialState.time)
                AppText24(
                    initialState.meridiem.name,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Time until alarm
            AppText14(
                text = initialState.timeUntilAlarm,
                color = MaterialTheme.colorScheme.outline
            )

            // Weekly chips
            AppWeeklyChips(
                modifier = Modifier.fillMaxWidth(),
                enabled = initialState.isEnabled,
                onSelectionChanged = { selectedDays ->
                    onAlarmEvent(AlarmEvent.OnDaysChanged(selectedDays))
                }
            )

            // Suggested sleep time
            AppText14(
                text = String.format(
                    stringResource(id = R.string.get_eight_hours_of_sleep),
                    initialState.suggestedSleepTime,
                    initialState.meridiem.name
                ),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// Preview
@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppAlarmBoxPreview() {
    var previewState by remember {
        mutableStateOf(
            AlarmState(
                time = "10:00",
                meridiem = Meridiem.AM,
                isEnabled = true,
                selectedDays = mapOf(
                    "mo" to false,
                    "tu" to false,
                    "we" to false,
                    "th" to false,
                    "fr" to false,
                    "sa" to false,
                    "su" to false
                ),
                timeUntilAlarm = "Alarm in 30min",
                suggestedSleepTime = "10:00"
            )
        )
    }

    SnoozelooTheme {
        AppBackground {
            AppAlarmBox(
                initialState = previewState,
                onAlarmEvent = { event ->
                    when (event) {
                        is AlarmEvent.OnEnabledChanged -> {
                            previewState = previewState.copy(isEnabled = event.enabled)
                        }
                        is AlarmEvent.OnDaysChanged -> {
                            previewState = previewState.copy(selectedDays = event.selectedDays)
                        }
                    }
                },
                modifier = Modifier.padding(UIConst.padding)
            )
        }
    }
}