package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppAlarmBox
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppScreenWithFAB
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText16
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlarmScreenRoot(
    onOpenAlarmEditor: (alarmId: String) -> Unit,
    viewModel: AlarmViewModel = koinViewModel()
) {
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AlarmUiEvent.OnOpenAlarmEditor -> {
                if (event.alarmId.trim().isNotEmpty()) {
                    onOpenAlarmEditor(event.alarmId)
                }
            }
            else -> { }
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
    alarms: List<Alarm>,
    onEvent: (AlarmUiEvent) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = UIConst.padding)) {
        AppText24(
            stringResource(R.string.your_alarms),
            modifier = Modifier.padding(vertical = UIConst.padding)
        )

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UIConst.padding)
                ) {
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

        LazyColumn {
            items(
                items = alarms,
                key = { item -> item.id }
            ) { alarm ->
                SwipeableAlarmItem(
                    alarmState = alarm,
                    onDelete = { onEvent(AlarmUiEvent.OnAlarmDeleted(alarm)) },
                    onAlarmEvent = onEvent,
                    modifier = Modifier.padding(vertical = UIConst.paddingSmall)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAlarmItem(
    alarmState: Alarm,
    onDelete: () -> Unit,
    onAlarmEvent: (AlarmUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.5f }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.delete,
                    contentDescription = "Delete Alarm",
                    tint = UIConst.colorWithAlpha(MaterialTheme.colorScheme.onSurface, 0.4f),
                    modifier = Modifier
                        .width(72.dp)
                        .height(72.dp)
                )
            }
        },
        content = {
            AppAlarmBox(
                initialState = alarmState,
                onAlarmEvent = onAlarmEvent,
                modifier = modifier
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}


@Preview
@Composable
private fun AlarmScreenPreview() {
    AlarmScreen(
        state = AlarmsState(emptyList()),
        onEvent = {}
    )
}