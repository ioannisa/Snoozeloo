package eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
            else -> {

            }
        }
    }

    RingtoneSettingScreen(onEvent = viewModel::onEvent)
}

@Composable
private fun RingtoneSettingScreen(
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
        RingtoneSettingItem(RingtoneItem.SILENT.text, isChecked = false, isSilent = true)
        RingtoneSettingItem(RingtoneItem.DEFAULT.text, isChecked = true)
        RingtoneSettingItem(RingtoneItem.BRIGHT_MORNING.text, isChecked = false)
        RingtoneSettingItem(RingtoneItem.CUCKOO_CLOCK.text, isChecked = false)
        RingtoneSettingItem(RingtoneItem.EARLY_TWILIGHT.text, isChecked = false)
    }
}

@Composable
fun RingtoneSettingItem(text: String, isChecked: Boolean, isSilent: Boolean = false) {
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
                        // todo: add the onEvent callback
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

            AppText14(text, modifier = Modifier.weight(1f))
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

@Composable
@Preview
fun RingtoneSettingScreenPreview() {
    RingtoneSettingScreen() {

    }
}

enum class RingtoneItem(val text: String) {
    SILENT("Silent"),
    DEFAULT("Default (Bright Morning)"),
    BRIGHT_MORNING("Bright Morning"),
    CUCKOO_CLOCK("Cuckoo Clock"),
    EARLY_TWILIGHT("Early Twilight")
}