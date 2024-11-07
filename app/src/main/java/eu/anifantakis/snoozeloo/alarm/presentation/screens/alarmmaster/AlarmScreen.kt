package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmmaster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AlarmEvent
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AlarmState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppAlarmBox
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppScreenWithFAB
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText16
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlarmScreenRoot(
    onClockClick: () -> Unit,
    viewModel: AlarmViewModel = koinViewModel()
) {
    ObserveAsEvents(viewModel.events) { event ->
        if (event == AlarmUiEvent.OnClockTapped) {
            onClockClick()
        }
    }

    AlarmScreen(
        state = viewModel.state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun AlarmScreen(
    state: AlarmsState,
    onEvent: (AlarmUiEvent) -> Unit
) {
    AppScreenWithFAB(
        onFabClick = {
            onEvent(AlarmUiEvent.OnAddAlarm)
        }
    ) {
        AlarmListScreen(
            alarms = state.alarms,
            onEvent = onEvent
        )
    }
}

@Composable
private fun AlarmListScreen(
    alarms: List<AlarmState>,
    onEvent: (AlarmUiEvent) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = UIConst.padding)) {
        AppText24(stringResource(R.string.your_alarms), modifier = Modifier.padding(vertical = UIConst.padding))

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UIConst.padding)
                ){
                    Icon(
                        imageVector = Icons.alarm,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp)
                    )
                    AppText16(stringResource(R.string.empty_alarms_screen))
                }
            }
        }

        LazyColumn{
            items(
                items = alarms,
                key = { item -> item.id }
            ) { alarmState ->
                AppAlarmBox(
                    initialState = alarmState,
                    onAlarmEvent = { event ->
                        when (event) {
                            is AlarmEvent.OnEnabledChanged -> {
                                onEvent(AlarmUiEvent.OnAlarmEnabled(alarmState, event.enabled))
                            }

                            is AlarmEvent.OnDaysChanged -> {
                                onEvent(
                                    AlarmUiEvent.OnAlarmDaysChanged(
                                        alarmState,
                                        event.selectedDays
                                    )
                                )
                            }

                            AlarmEvent.OnClockTapped -> {
                                onEvent(AlarmUiEvent.OnClockTapped)
                            }
                        }
                    },
                    modifier = Modifier.padding(vertical = UIConst.paddingSmall)
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlarmScreenPreview() {
    AlarmScreen(
        state = AlarmsState(emptyList()),
        onEvent = {}
    )
}