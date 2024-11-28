package eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
import eu.anifantakis.snoozeloo.alarm.presentation.screens.AlarmUiState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UiText
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppActionButton
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppBackground
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppCard
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppClock
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppSlider
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppSwitch
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText16
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppWeeklyChips
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.LocalDimmingState
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Root composable for the alarm editor screen.
 * Handles navigation events and snackbar messages.
 *
 * @param viewModel ViewModel managing alarm editing state
 * @param onOpenRingtoneSetting Callback for opening ringtone selection
 * @param onClose Callback for closing the editor
 */
@Composable
fun AlarmEditScreenRoot(
    viewModel: AlarmEditViewModel = koinViewModel(),
    onOpenRingtoneSetting: () -> Unit,
    onClose: () -> Unit
) {
    val alarmUiState by viewModel.alarmUiState
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle one-time events (navigation, messages)
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AlarmEditorScreenEvent.OnOpenRingtoneSettings -> onOpenRingtoneSetting()
            is AlarmEditorScreenEvent.OnShowSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
            is AlarmEditorScreenEvent.OnClose -> onClose()
        }
    }

    Box {
        AlarmEditScreen(
            alarmUiState = alarmUiState,
            onAction = viewModel::onAction
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Main alarm editor screen content.
 * Displays alarm settings including time, title, days, ringtone, volume, and vibration.
 *
 * @param alarmUiState Current alarm state
 * @param onAction Callback for user actions
 */
@Composable
fun AlarmEditScreen(
    alarmUiState: AlarmUiState?,
    onAction: (AlarmEditorScreenAction) -> Unit,
) {
    val dimmingState = LocalDimmingState.current

    // Local UI state for dialog visibility and clock reset
    var showDialog by remember { mutableStateOf(false) }
    var clockResetTrigger by remember { mutableIntStateOf(0) }

    // Update dimming when dialog is shown
    LaunchedEffect(showDialog) {
        dimmingState.isDimmed = showDialog
    }

    Box {
        Column(
            verticalArrangement = Arrangement.spacedBy(UIConst.padding),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Top bar with save/cancel buttons
            TopButtons(
                hasChanges = alarmUiState?.hasChanges ?: false,
                onSave = { onAction(AlarmEditorScreenAction.SaveAlarm) },
                onCancel = {
                    onAction(AlarmEditorScreenAction.CancelChanges)
                    clockResetTrigger++ // Reset clock to original time
                }
            )

            alarmUiState?.let { currentState ->
                // Time picker clock
                AppClock(
                    initialHour = currentState.alarm.hour,
                    initialMinute = currentState.alarm.minute,
                    resetKey = clockResetTrigger,
                    nextAlarmAt = currentState.timeUntilNextAlarm.asString(),
                    canShowNextAlarm = currentState.alarm.selectedDays.hasAnyDaySelected()
                ) { hour, minute ->
                    onAction(AlarmEditorScreenAction.UpdateAlarmTime(hour, minute))
                }

                // Scrollable settings section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(UIConst.padding)
                ) {
                    // Alarm title card
                    AppCard(
                        modifier = Modifier.clickable { showDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AppText16(
                                stringResource(R.string.alarm_name),
                                fontWeight = FontWeight.W700
                            )
                            AppText16(
                                if (currentState.alarm.title.trim() == "") {
                                    stringResource(R.string.default_alarm_title)
                                } else {
                                    currentState.alarm.title.trim()
                                },
                                fontWeight = FontWeight.W400,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Weekly repeat selection
                    AppCard {
                        Column {
                            AppText16(stringResource(R.string.repeat), fontWeight = FontWeight.W700)
                            AppWeeklyChips(
                                selectedDays = currentState.alarm.selectedDays,
                                onSelectionChanged = { newDays ->
                                    onAction(AlarmEditorScreenAction.UpdateAlarmDays(newDays))
                                },
                                onError = {
                                    onAction(AlarmEditorScreenAction.ShowDaysValidationError)
                                }
                            )
                        }
                    }

                    // Ringtone selection
                    AppCard(
                        modifier = Modifier.clickable {
                            onAction(AlarmEditorScreenAction.OpenRingtoneSettings)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AppText16(
                                stringResource(R.string.alarm_ringtone),
                                fontWeight = FontWeight.W700
                            )
                            AppText16(
                                if (currentState.alarm.ringtoneTitle.trim() == "") {
                                    stringResource(R.string.default_ringtone_name)
                                } else {
                                    currentState.alarm.ringtoneTitle.trim()
                                },
                                fontWeight = FontWeight.W400,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Volume slider
                    AppCard {
                        Column {
                            AppText16(
                                stringResource(R.string.alarm_volume),
                                fontWeight = FontWeight.W700
                            )
                            AppSlider(
                                value = currentState.alarm.volume,
                                onValueChange = { newValue ->
                                    onAction(AlarmEditorScreenAction.UpdateAlarmVolume(newValue))
                                },
                                valueRange = 0f..1f
                            )
                        }
                    }

                    // Vibration toggle
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppText16(
                                stringResource(R.string.vibrate),
                                fontWeight = FontWeight.W700
                            )
                            AppSwitch(
                                width = 48,
                                height = 24,
                                checked = currentState.alarm.vibrate,
                                onCheckedChange = { newValue ->
                                    onAction(AlarmEditorScreenAction.UpdateAlarmVibration(newValue))
                                }
                            )
                        }
                    }

                    // Title edit dialog
                    if (showDialog) {
                        ShowAlertDialog(
                            initialTitle = currentState.alarm.title.trim(),
                            onDismiss = { showDialog = false },
                            onSave = { alarmTitle ->
                                showDialog = false
                                onAction(AlarmEditorScreenAction.UpdateAlarmTitle(alarmTitle))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top bar buttons for saving or canceling alarm edits.
 * Save button is enabled only when changes are present.
 */
@Composable
fun TopButtons(
    hasChanges: Boolean = false,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.weight(1f)) {
            AppActionButton(
                icon = Icons.close,
                cornerRadius = 10.dp,
                fillWidth = false,
                enabled = true,
                contentPadding = PaddingValues(all = 0.dp),
                onClick = onCancel
            )
        }

        Box {
            AppActionButton(
                text = stringResource(R.string.save),
                largeText = false,
                cornerRadius = 24.dp,
                fillWidth = false,
                enabled = hasChanges,
                contentPadding = PaddingValues(horizontal = 24.dp),
                onClick = onSave
            )
        }
    }
}

/**
 * Dialog for editing alarm title with keyboard focus management.
 * Supports both button and keyboard-based confirmation.
 */
@Composable
private fun ShowAlertDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialTitle,
                selection = TextRange(0, initialTitle.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus text field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {},
        text = {
            Column {
                AppText16(stringResource(R.string.alarm_name), fontWeight = FontWeight.W700)
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = UIConst.padding)
                        .focusRequester(focusRequester),
                    placeholder = {
                        AppText16(
                            stringResource(R.string.default_alarm_title),
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onSave(textInput.text)
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    onSave(textInput.text)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = UIConst.padding
    )
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AlarmEditScreenPreview() {
    val previewAlarm = Alarm(
        id = "",
        hour = 17,
        minute = 32,
        isEnabled = true,
        title = "Sample Title",
        selectedDays = DaysOfWeek(
            mo = false, tu = false, we = false,
            th = false, fr = false, sa = false, su = false
        )
    )

    val previewUiState = AlarmUiState(
        alarm = previewAlarm,
        timeUntilNextAlarm = UiText.StringResource(R.string.alarm_in_1_hour)
    )

    SnoozelooTheme {
        AppBackground {
            AlarmEditScreen(
                alarmUiState = previewUiState,
                onAction = {}
            )
        }
    }
}