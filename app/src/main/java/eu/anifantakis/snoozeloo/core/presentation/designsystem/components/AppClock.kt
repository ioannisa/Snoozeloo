package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme

@Composable
fun AppClock() {
    Box {
        AppCard {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClockRectangle(
                    text = "16",
                    modifier = Modifier.weight(1f)
                )


                Icon(
                    imageVector = Icons.colon,
                    contentDescription = "",
                    modifier = Modifier
                        .padding(UIConst.paddingSmall)
                        .height(UIConst.paddingDouble)

                )

                ClockRectangle(
                    text = "45",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ClockRectangle(
    text: String,
    modifier: Modifier = Modifier  // Add modifier parameter
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier  // Use the passed modifier
            .height(95.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        AppText52(text)
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppClockPreview() {
    SnoozelooTheme {
        AppBackground {

            Box(
                modifier = Modifier
                    .padding(16.dp)
            ) {

                AppClock()
            }
        }
    }
}