package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppClock
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText52

@Composable
fun AlarmEditScreen(
    alarmId: String
) {

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {

        AppClock("12", "22")

        AppText24("AlarmId -> $alarmId")
    }

}