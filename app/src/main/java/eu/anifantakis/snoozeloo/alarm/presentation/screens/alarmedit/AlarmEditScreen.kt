package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppClock
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24

@Composable
fun AlarmEditScreen(
    alarmId: String
) {

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {

        AppClock(0, 0) { hour, minute ->
            //
        }

        AppText24("AlarmId -> $alarmId")
    }

}