package eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm.AlarmUiEvent
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm.AlarmsState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppCard
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText14
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun RingtoneSettingScreenRoot(
    viewModel: RingtoneSettingsViewModel = koinViewModel()
) {
    ObserveAsEvents(viewModel.events) { event ->

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
    Column {
        Box(
            modifier = Modifier
                .padding(top = UIConst.paddingDouble)
                .clip(RoundedCornerShape(UIConst.borderRadius))
                .background(
                    MaterialTheme.colorScheme.primary
                )
                .size((32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.back, contentDescription = "alarm")
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            RingtoneSettingItem(
                title = "Silent",
                isSilent = true,
                onClickOnRingtone = {
                    onAction(RingtoneAction.OnSelectRingtone(null))
                }
            )
        }
        items(state.ringtones) { ringtone ->
            RingtoneSettingItem(
                title = ringtone.first,
                onClickOnRingtone = {
                    onAction(RingtoneAction.OnSelectRingtone(ringtone))
                }
            )
        }
    }
}

@Composable
fun RingtoneSettingItem(
    title: String,
    onClickOnRingtone: () -> Unit,
    isSilent: Boolean = false
) {
    //todo: link isChecked with the selected ringtone
    //todo: get the already selected ringtone from the data store
    var isChecked by remember { mutableStateOf(
        false
    ) }
    AppCard(modifier = Modifier.padding(vertical = 10.dp, horizontal = UIConst.padding)) {
        Row(
            modifier = Modifier.clickable {
                isChecked = true
                onClickOnRingtone()
            },
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

            AppText14(title, modifier = Modifier.weight(1f))
            if (isChecked)
            // todo: fix background issue
                Icon(
                    imageVector = Icons.checked,
                    tint = MaterialTheme.colorScheme.inversePrimary,
                    contentDescription = "checked"
                )
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