package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.*
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Root composable for the alarms list screen. Handles event observation and snackbar displays.
 *
 * @param onOpenAlarmEditor Callback for navigating to alarm editor
 * @param viewModel ViewModel handling the screen's logic
 */
@Composable
fun AlarmsScreenRoot(
    onOpenAlarmEditor: (alarm: Alarm) -> Unit,
    viewModel: AlarmsViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle screen events (navigation and snackbar displays)
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AlarmsScreenEvent.OnOpenAlarmEditorFor -> {
                onOpenAlarmEditor(event.alarm)
            }
            is AlarmsScreenEvent.OnShowSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = event.message.asString(context),
                        actionLabel = null,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Main screen layout with snackbar host
    Box {
        AlarmsScreen(
            state = viewModel.state,
            onAction = viewModel::onAction
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Main alarms screen composable with FAB for adding new alarms.
 */
@Composable
private fun AlarmsScreen(
    state: AlarmsScreenState,
    onAction: (AlarmsScreenAction) -> Unit
) {
    AppScreenWithFAB(
        onFabClick = { onAction(AlarmsScreenAction.AddNewAlarm) }
    ) {
        AlarmsListScreen(
            alarms = state.alarms,
            use24HourFormat = state.use24HourFormat,
            onEvent = onAction
        )
    }
}

/**
 * Displays the list of alarms with a header containing the time format toggle.
 * Shows an empty state when no alarms are present.
 */
@Composable
private fun AlarmsListScreen(
    alarms: List<AlarmUiState>,
    use24HourFormat: Boolean,
    onEvent: (AlarmsScreenAction) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = UIConst.padding)) {
        // Header with title and time format switch
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

            // Time format toggle switch (12/24 hour)
            AppSwitch(
                width = 90,
                height = 45,
                checked = use24HourFormat,
                uncheckedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSecondary,
                uncheckedText = stringResource(R.string.clock_12h),
                checkedText = stringResource(R.string.clock_24h),
                textColor = MaterialTheme.colorScheme.onSecondary,
                onCheckedChange = { use24Hour ->
                    onEvent(AlarmsScreenAction.ChangeTimeFormat(use24Hour))
                }
            )
        }

        // Empty state display
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

        // Alarm list with swipeable items
        LazyColumn {
            items(
                items = alarms,
                key = { item -> item.alarm.id }
            ) { alarmUiState ->
                SwipeableAlarmItem(
                    alarmUiState = alarmUiState,
                    use24HourFormat = use24HourFormat,
                    onDelete = { onEvent(AlarmsScreenAction.DeleteAlarmsScreen(alarmUiState.alarm)) },
                    onAlarmEvent = onEvent,
                    modifier = Modifier.padding(vertical = UIConst.paddingSmall)
                )
            }
        }
    }
}

/**
 * Individual alarm item with swipe-to-delete functionality.
 * Swipe threshold is set to 50% of the total possible swipe distance.
 */
@Composable
private fun SwipeableAlarmItem(
    alarmUiState: AlarmUiState,
    use24HourFormat: Boolean,
    onDelete: () -> Unit,
    onAlarmEvent: (AlarmsScreenAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.5f }
    )

    // Trigger deletion when swipe reaches threshold
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Delete icon shown during swipe
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
    AlarmsScreen(
        state = AlarmsScreenState(
            alarms = emptyList(),
            use24HourFormat = false
        ),
        onAction = {}
    )
}