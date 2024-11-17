package eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.alarmtonesetting

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppActionButton
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppCard
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText14
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.LocalDimmingState
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlarmToneSettingScreenRoot(
    alarmId: String,
    onGoBack: () -> Unit,
    viewModel: AlarmToneSettingViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.onAction(AlarmToneAction.OnOpenRingtonesSetting(alarmId = alarmId))
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AlarmToneEvent.OnNavigateBack -> {
                onGoBack()
            }
        }
    }

    val dimmingState = LocalDimmingState.current
    LaunchedEffect(viewModel.state.isLoading) {
        dimmingState.isDimmed = viewModel.state.isLoading
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
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

@Composable
private fun AlarmToneSettingScreen(
    state: AlarmToneState,
    onAction: (AlarmToneAction) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.ringtones) {
        val selectedIndex = state.ringtones.indexOfFirst { it.isSelected }
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex + 1)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(UIConst.padding),
        modifier = Modifier
            .padding(horizontal = UIConst.padding)
    ) {
        AppActionButton(
            icon = Icons.back,
            cornerRadius = 10.dp,
            fillWidth = false,
            enabled = true,
            contentPadding = PaddingValues(all = 0.dp),
        ) {
            onAction(AlarmToneAction.NavigateBack)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            state = listState,
        ) {
            item {
                RingtoneSettingItem(
                    alarmoneItem = AlarmoneItem(title = "Silent", uri = null),
                    isSilent = true,
                    isSelected = state.currentAlarm?.ringtoneUri == null,
                    onClickOnRingtone = { selectedRingtone ->
                        onAction(AlarmToneAction.OnSelectAlarmTone(selectedRingtone))
                    }
                )
            }
            items(state.ringtones) { ringtone ->
                RingtoneSettingItem(
                    alarmoneItem = ringtone,
                    isSelected = ringtone.title == state.currentAlarm?.ringtoneTitle,
                    onClickOnRingtone = { selectedRingtone ->
                        onAction(AlarmToneAction.OnSelectAlarmTone(selectedRingtone))
                    }
                )
            }
        }
    }
}

@Composable
fun RingtoneSettingItem(
    alarmoneItem: AlarmoneItem,
    isSelected: Boolean,
    onClickOnRingtone: (AlarmoneItem) -> Unit,
    isSilent: Boolean = false
) {
    AppCard(modifier = Modifier
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

            AppText14(alarmoneItem.title, modifier = Modifier.weight(1f))
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
@Preview
fun RingtoneSettingScreenPreview() {
    AlarmToneSettingScreen(state = AlarmToneState()) {

    }
}