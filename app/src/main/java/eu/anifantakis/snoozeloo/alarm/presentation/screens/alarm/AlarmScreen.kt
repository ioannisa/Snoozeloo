package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppScreenWithFAB
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppSwitch
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText16
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlarmScreenRoot(
    onOpenAlarmEditor: (alarmId: String) -> Unit,
    viewModel: AlarmViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AlarmScreenEvent.OnSelectAlarm -> {
                if (event.alarmId.trim().isNotEmpty()) {
                    onOpenAlarmEditor(event.alarmId)
                }
            }

            is AlarmScreenEvent.OnShowSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = null,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Box {
        AlarmScreen(
            state = viewModel.state,
            onAction = viewModel::onAction
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AlarmScreen(
    state: AlarmsScreenState,
    onAction: (AlarmScreenAction) -> Unit
) {
    AppScreenWithFAB(
        onFabClick = {
            onAction(AlarmScreenAction.AddAlarmScreen)
        }
    ) {
        AlarmListScreen(
            alarms = state.alarms,
            use24HourFormat = state.use24HourFormat,
            onEvent = onAction
        )
    }
}

@Composable
private fun AlarmListScreen(
    alarms: List<AlarmUiState>,
    use24HourFormat: Boolean,
    onEvent: (AlarmScreenAction) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = UIConst.padding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText24(
                stringResource(R.string.your_alarms),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = UIConst.padding)
            )

            AppSwitch(
                width = 90,
                height = 45,
                checked = use24HourFormat,
                uncheckedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedText = stringResource(R.string.clock_12h),
                checkedText = stringResource(R.string.clock_24h),
                onCheckedChange = { use24Hour ->
                    onEvent(AlarmScreenAction.ChangeTimeFormat(use24Hour))
                }
            )
        }

        if (alarms.none { !it.alarm.temporary }) {
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
                items = alarms.filter { !it.alarm.temporary },
                key = { item -> item.alarm.id }
            ) { alarmUiState ->
                SwipeableAlarmItem(
                    alarmUiState = alarmUiState,
                    use24HourFormat = use24HourFormat,
                    onDelete = { onEvent(AlarmScreenAction.DeleteAlarmScreen(alarmUiState.alarm)) },
                    onAlarmEvent = onEvent,
                    modifier = Modifier.padding(vertical = UIConst.paddingSmall)
                )
            }
        }
    }
}

@Composable
private fun SwipeableAlarmItem(
    alarmUiState: AlarmUiState,
    use24HourFormat: Boolean,
    onDelete: () -> Unit,
    onAlarmEvent: (AlarmScreenAction) -> Unit,
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
                        .width(56.dp)
                        .height(56.dp)
                )
            }
        },
        content = {
            AppAlarmBox(
                alarmUiState = alarmUiState,
                use24HourFormat = use24HourFormat,
                onAlarmEvent = onAlarmEvent,
                modifier = modifier
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AlarmScreenPreview() {
    AlarmScreen(
        state = AlarmsScreenState(
            alarms = emptyList(),
            use24HourFormat = false
        ),
        onAction = {}
    )
}