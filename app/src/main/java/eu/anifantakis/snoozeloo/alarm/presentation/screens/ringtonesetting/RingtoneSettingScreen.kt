package eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm.AlarmUiEvent
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm.AlarmViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm.AlarmsState
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppCard
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText14
import eu.anifantakis.snoozeloo.core.presentation.ui.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun RingtoneSettingScreenRoot(
    viewModel: AlarmViewModel = koinViewModel()
) {
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AlarmUiEvent.OnClickOnSpecificRingtone -> {
                viewModel.onEvent(AlarmUiEvent.OnClickOnSpecificRingtone(event.ringtone))
            }
            AlarmUiEvent.OnOpenRingtonesSetting -> {
                viewModel.onEvent(AlarmUiEvent.OnOpenRingtonesSetting)
            }
            else -> {}
        }
    }

    RingtoneSettingScreen(state = viewModel.state, onEvent = viewModel::onEvent)
}

@Composable
private fun RingtoneSettingScreen(
    state: AlarmsState,
    onEvent: (AlarmUiEvent) -> Unit
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
        Spacer(modifier = Modifier.height(24.dp))
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.listOfRingtones) { ringtone ->
            RingtoneSettingItem(
                title = ringtone.first,
                isChecked = false,
                onClickOnRingtone = {
                    onEvent(AlarmUiEvent.OnClickOnSpecificRingtone(ringtone))
                }
            )
        }
    }
}

@Composable
fun RingtoneSettingItem(
    title: String,
    isChecked: Boolean,
    onClickOnRingtone: () -> Unit,
    isSilent: Boolean = false
) {
    AppCard(modifier = Modifier.padding(vertical = 10.dp, horizontal = UIConst.padding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UIConst.paddingSmall)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.2f))
                    .clickable {
                        onClickOnRingtone()
                    }
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
    RingtoneSettingScreen(state = AlarmsState()) {

    }
}

enum class RingtoneItem(val text: String) {
    SILENT("Silent"),
    DEFAULT("Default (Bright Morning)"),
    BRIGHT_MORNING("Bright Morning"),
    CUCKOO_CLOCK("Cuckoo Clock"),
    EARLY_TWILIGHT("Early Twilight")
}