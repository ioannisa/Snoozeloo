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
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppActionButton
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppBackground
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppOutlinedActionButton
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText20
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText24
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText52
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppText82
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import java.time.LocalTime

@Composable
fun AlarmDismissScreen(
    title: String,
    isFullScreen: Boolean,
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
                modifier = Modifier.padding(UIConst.padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isFullScreen) UIConst.padding else UIConst.paddingSmall)
            ) {
                Icon(
                    imageVector = Icons.alarm,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(55.dp)
                        .height(55.dp)
                )

                if (isFullScreen) {
                    AppText82(
                        text = LocalTime.now().toTime24String(),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    AppText52(
                        text = LocalTime.now().toTime24String(),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                if (isFullScreen) {
                    AppText24(text = title)
                } else {
                    AppText20(text = title)
                }

                Spacer(modifier = Modifier.height(if (isFullScreen) UIConst.paddingDouble else UIConst.padding))

                AppActionButton(
                    text = "Snooze",
                    largeText = false,
                    onClick = onSnooze,
                    contentPadding = PaddingValues(vertical = if (isFullScreen) 16.dp else 8.dp)
                )
                AppOutlinedActionButton(
                    text = "Dismiss",
                    largeText = false,
                    onClick = onDismiss,
                    contentPadding = PaddingValues(vertical = if (isFullScreen) 8.dp else 0.dp)
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
                isFullScreen = true,
                onSnooze = { },
                onDismiss = { }
            )
        }
    }
}