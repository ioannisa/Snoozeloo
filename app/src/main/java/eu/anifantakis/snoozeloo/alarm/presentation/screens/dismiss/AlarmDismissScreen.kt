package eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.domain.util.ClockUtils.toTime24String
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppActionButton
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppBackground
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppOutlinedActionButton
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText82
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import java.time.LocalTime

@Composable
fun AlarmDismissScreen(
    title: String,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.alarm,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(55.dp)
                        .height(55.dp)
                )

                AppText82(
                    text = LocalTime.now().toTime24String(),
                    modifier = Modifier.padding(top = 16.dp)
                )

                AppText24(
                    text = title,
                )
                Spacer(modifier = Modifier.height(32.dp))

                AppActionButton(
                    text = "Snooze",
                    onClick = onSnooze,
                    contentPadding = PaddingValues(vertical = 16.dp)
                )
                AppOutlinedActionButton(
                    text = "Dismiss",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AlarmDismissScreenPreview() {
    SnoozelooTheme {
        AppBackground {
            AlarmDismissScreen(
                title = "Wake up!",
                onSnooze = { },
                onDismiss = { }
            )
        }
    }
}