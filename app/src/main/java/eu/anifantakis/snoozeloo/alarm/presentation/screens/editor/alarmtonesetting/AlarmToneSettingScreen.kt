package eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.alarmtonesetting

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.*
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

/**
 * Root composable for the alarm tone selection screen.
 * Handles navigation, state observation, and loading states.
 *
 * @param alarmToneUri Currently selected alarm tone URI
 * @param onGoBack Callback when user navigates back with selected tone
 * @param viewModel ViewModel handling tone selection logic
 */
@Composable
fun AlarmToneSettingScreenRoot(
    alarmToneUri: String?,
    onGoBack: (String, String?) -> Unit,
    viewModel: AlarmToneSettingViewModel = koinViewModel()
) {
    // Handle system back button
    BackHandler {
        viewModel.onAction(AlarmToneAction.NavigateBack)
    }

    // Load ringtones when screen opens
    LaunchedEffect(Unit) {
        viewModel.onAction(AlarmToneAction.OnOpenRingtonesSetting(alarmToneUri = alarmToneUri))
    }

    // Handle navigation events
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AlarmToneEvent.OnNavigateBack -> {
                onGoBack(event.title, event.uri.toString())
            }
        }
    }

    // Manage screen dimming during loading
    val dimmingState = LocalDimmingState.current
    LaunchedEffect(viewModel.state.isLoading) {
        dimmingState.isDimmed = viewModel.state.isLoading
    }

    // Main layout with loading indicator
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            AlarmToneSettingScreen(
                state = viewModel.state,
                onAction = viewModel::onAction
            )
        }

        if (viewModel.state.isLoading) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Main screen content showing the list of available ringtones.
 * Includes a back button and scrollable list of tone options.
 */
@Composable
private fun AlarmToneSettingScreen(
    state: AlarmToneState,
    onAction: (AlarmToneAction) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to selected ringtone when list loads
    LaunchedEffect(state.ringtones) {
        val selectedIndex = state.ringtones.indexOfFirst { it.isSelected }
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex + 1)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(UIConst.padding),
        modifier = Modifier.padding(horizontal = UIConst.padding)
    ) {
        // Back button
        AppActionButton(
            icon = Icons.back,
            cornerRadius = 10.dp,
            fillWidth = false,
            enabled = true,
            contentPadding = PaddingValues(all = 0.dp),
        ) {
            onAction(AlarmToneAction.NavigateBack)
        }

        // Ringtone list
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
        ) {
            // Silent option
            item {
                RingtoneSettingItem(
                    alarmoneItem = AlarmoneItem(title = "Silent", uri = null),
                    isSilent = true,
                    isSelected = state.currentSelectedRingtone?.uri == null,
                    onClickOnRingtone = { selectedRingtone ->
                        onAction(AlarmToneAction.OnSelectAlarmTone(selectedRingtone))
                    }
                )
            }

            // Default system ringtone if available
            state.defaultSystemRingtone?.let { defaultRingtone ->
                item {
                    RingtoneSettingItem(
                        alarmoneItem = defaultRingtone,
                        isSelected = defaultRingtone.uri?.toString() == state.currentSelectedRingtone?.uri?.toString(),
                        onClickOnRingtone = { selectedRingtone ->
                            onAction(AlarmToneAction.OnSelectAlarmTone(selectedRingtone))
                        }
                    )
                }
            }

            // Custom ringtones, excluding system default
            items(state.ringtones.filter { it.uri?.toString() != state.defaultSystemRingtone?.uri?.toString() }) { ringtone ->
                RingtoneSettingItem(
                    alarmoneItem = ringtone,
                    isSelected = ringtone.uri?.toString() == state.currentSelectedRingtone?.uri?.toString(),
                    onClickOnRingtone = { selectedRingtone ->
                        onAction(AlarmToneAction.OnSelectAlarmTone(selectedRingtone))
                    }
                )
            }
        }
    }
}

/**
 * Individual ringtone item showing name and selection state.
 * Displays different icons for silent and regular ringtones.
 *
 * @param alarmoneItem Ringtone data
 * @param isSelected Whether this tone is currently selected
 * @param onClickOnRingtone Callback when tone is selected
 * @param isSilent Whether this is the silent option
 */
@Composable
fun RingtoneSettingItem(
    alarmoneItem: AlarmoneItem,
    isSelected: Boolean,
    onClickOnRingtone: (AlarmoneItem) -> Unit,
    isSilent: Boolean = false
) {
    AppCard(
        modifier = Modifier
            .padding(vertical = UIConst.paddingExtraSmall)
            .clickable {
                onClickOnRingtone(alarmoneItem.copy(
                    title = if (isSilent) "Silent" else alarmoneItem.title,
                    uri = if (isSilent) null else alarmoneItem.uri
                ))
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UIConst.paddingSmall)
        ) {
            // Bell icon (on/off)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (isSilent) Icons.bellOff else Icons.bellOn,
                    contentDescription = "Logo",
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp)
                )
            }

            // Ringtone title
            AppText14(alarmoneItem.title, modifier = Modifier.weight(1f))

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.checked,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = "checked",
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(UIConst.paddingExtraSmall)
                        .width(UIConst.padding)
                        .height(UIConst.padding)
                )
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun RingtoneSettingScreenPreview() {
    AlarmToneSettingScreen(state = AlarmToneState()) { }
}