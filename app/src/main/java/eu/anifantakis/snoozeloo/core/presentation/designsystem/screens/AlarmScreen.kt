// AlarmScreen.kt
package eu.anifantakis.snoozeloo.core.presentation.designsystem.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AlarmEvent
import androidx.compose.foundation.layout.padding
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AlarmState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppAlarmBox
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppScreenWithFAB
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlarmScreenRoot(
    viewModel: AlarmViewModel = koinViewModel()
) {
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
    LazyColumn(
        modifier = Modifier.padding(horizontal = UIConst.padding)
    ) {
        items(alarms) { alarmState ->
            AppAlarmBox(
                initialState = alarmState,
                onAlarmEvent = { event ->
                    when (event) {
                        is AlarmEvent.OnEnabledChanged -> {
                            onEvent(AlarmUiEvent.OnAlarmEnabled(alarmState, event.enabled))
                        }
                        is AlarmEvent.OnDaysChanged -> {
                            onEvent(AlarmUiEvent.OnAlarmDaysChanged(alarmState, event.selectedDays))
                        }
                    }
                },
                modifier = Modifier.padding(vertical = UIConst.paddingSmall)
            )
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