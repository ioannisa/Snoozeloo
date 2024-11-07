package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
fun AppClock(
    hour: String,
    minute: String,
) {
    Box {
        AppCard {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                ClockRectangle(
                    text = hour,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.colon,
                    contentDescription = "",
                    modifier = Modifier
                        .padding(UIConst.paddingExtraSmall)
                        .height(16.dp)
                        .width(16.dp)
                )

                ClockRectangle(
                    text = minute,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ClockRectangle(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
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

                AppClock("12", "35")
            }
        }
    }
}