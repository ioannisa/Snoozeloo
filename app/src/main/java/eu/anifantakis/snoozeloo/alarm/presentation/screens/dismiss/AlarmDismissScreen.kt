package eu.anifantakis.snoozeloo.alarm.presentation.screens.dismiss

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.*
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
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.*
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import java.time.LocalTime

/**
 * Screen shown when an alarm is triggered.
 * Displays current time, alarm title, and snooze/dismiss actions.
 * Adapts its layout based on whether it's shown full screen or as a compact overlay.
 *
 * @param title The alarm's title to display
 * @param isFullScreen Whether to show full screen layout (true) or compact overlay (false)
 * @param onSnooze Callback for snooze action
 * @param onDismiss Callback for dismiss action
 */
@Composable
fun AlarmDismissScreen(
    title: String,
    isFullScreen: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    // Main surface with rounded corners and elevation
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp
    ) {
        // Center all content vertically
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Content column with adaptive spacing
            Column(
                modifier = Modifier.padding(UIConst.padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    if (isFullScreen) UIConst.padding else UIConst.paddingSmall
                )
            ) {
                // Alarm icon
                Icon(
                    imageVector = Icons.alarm,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(55.dp)
                        .height(55.dp)
                )

                // Current time display - larger in full screen
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

                // Alarm title - larger in full screen
                if (isFullScreen) {
                    AppText24(text = title)
                } else {
                    AppText20(text = title)
                }

                // Spacing before buttons
                Spacer(
                    modifier = Modifier.height(
                        if (isFullScreen) UIConst.paddingDouble else UIConst.padding
                    )
                )

                // Action buttons
                AppActionButton(
                    text = "Snooze",
                    largeText = false,
                    onClick = onSnooze,
                    contentPadding = PaddingValues(
                        vertical = if (isFullScreen) 16.dp else 8.dp
                    )
                )
                AppOutlinedActionButton(
                    text = "Dismiss",
                    largeText = false,
                    onClick = onDismiss,
                    contentPadding = PaddingValues(
                        vertical = if (isFullScreen) 8.dp else 0.dp
                    )
                )
            }
        }
    }
}

/**
 * Preview for the alarm dismiss screen in both light and dark modes.
 * Shows the full screen version of the layout.
 */
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