package eu.anifantakis.snoozeloo.core.presentation.designsystem.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.R
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppCard
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppSwitch
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText14
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText16
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText42
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppWeeklyChips

@Composable
fun AlarmScreenRoot() {
    AlarmScreen()
}

@Composable
private fun AlarmScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppText24("Your Alarms")
            AppCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppText16("Wake Up")
                        AppSwitch()
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AppText42("10:00")
                        AppText24("AM", modifier = Modifier.padding(bottom = 4.dp))
                    }
                    AppText14("Alarm in 30min", color = MaterialTheme.colorScheme.outline)
                    AppWeeklyChips(
                        modifier = Modifier.fillMaxWidth(),
                        onSelectionChanged = {

                        }
                    )
                    AppText14(
                        text = String.format(
                            stringResource(id = R.string.get_eight_hours_of_sleep),
                            "10:00",
                            Meridiem.AM.name
                        ),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

enum class Meridiem {
    AM,
    PM
}

@Composable
@Preview
fun AlarmScreenPreview() {
    AlarmScreenRoot()
}