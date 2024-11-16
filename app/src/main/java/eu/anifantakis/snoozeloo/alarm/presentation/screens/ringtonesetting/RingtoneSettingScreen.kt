package eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun RingtoneSettingScreenRoot(
    alarmId: String,
    onGoBack: () -> Unit,
    viewModel: RingtoneSettingsViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.onAction(RingtoneAction.OnOpenRingtonesSetting(alarmId = alarmId))
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RingtoneEvent.OnNavigateBack -> {
                onGoBack()
            }
        }
    }

    RingtoneSettingScreen(
        state = viewModel.state,
        onAction = viewModel::onAction
    )
}

@Composable
private fun RingtoneSettingScreen(
    state: RingtoneState,
    onAction: (RingtoneAction) -> Unit
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
            onAction(RingtoneAction.NavigateBack)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            state = listState,
        ) {
            item {
                RingtoneSettingItem(
                    ringtoneItem = RingtoneItem(title = "Silent", uri = null),
                    isSilent = true,
                    isSelected = state.currentAlarm?.ringtoneUri == null,
                    onClickOnRingtone = { selectedRingtone ->
                        onAction(RingtoneAction.OnSelectRingtone(selectedRingtone))
                    }
                )
            }
            items(state.ringtones) { ringtone ->
                RingtoneSettingItem(
                    ringtoneItem = ringtone,
                    isSelected = ringtone.title == state.currentAlarm?.ringtoneTitle,
                    onClickOnRingtone = { selectedRingtone ->
                        onAction(RingtoneAction.OnSelectRingtone(selectedRingtone))
                    }
                )
            }
        }
    }
}

@Composable
fun RingtoneSettingItem(
    ringtoneItem: RingtoneItem,
    isSelected: Boolean,
    onClickOnRingtone: (RingtoneItem) -> Unit,
    isSilent: Boolean = false
) {
    AppCard(modifier = Modifier
        .padding(vertical = UIConst.paddingExtraSmall)
        .clickable {
            onClickOnRingtone(ringtoneItem)
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

            AppText14(ringtoneItem.title, modifier = Modifier.weight(1f))
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
    RingtoneSettingScreen(state = RingtoneState()) {

    }
}