package eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppClock
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlarmEditScreen(
    alarmId: String,
    viewModel: AlarmEditViewModel = koinViewModel(),
    onOpenRingtoneSetting: () -> Unit
) {
    val alarm by viewModel.alarm.collectAsStateWithLifecycle()
    LaunchedEffect(alarmId) {
        viewModel.loadAlarm(alarmId)
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        alarm?.let { currentAlarm ->
            AppClock(
                initialHour = currentAlarm.hour,
                initialMinute = currentAlarm.minute
            ) { hour, minute ->
                viewModel.updateAlarmTime(hour, minute)
            }
        }

        AppText24("AlarmId -> $alarmId")

        Button(onClick = {
            onOpenRingtoneSetting()
        }) {
            Text("Open Ringtone Setting")
        }
    }

}