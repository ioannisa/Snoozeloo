package eu.anifantakis.snoozeloo.alarm.presentation.screens.clock

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText52

@Composable
fun ClockScreen(
    alarmId: String
) {

    Column {
        AppText52("AlarmId -> $alarmId")
    }

}