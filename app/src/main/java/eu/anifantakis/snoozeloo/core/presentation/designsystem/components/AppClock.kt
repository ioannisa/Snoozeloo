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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    Box {
        AppCard {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                ClockRectangle(
                    range = 0..23,
                    initialValue = initialHour,
                    modifier = Modifier.weight(1f),
                    speed = 0.3f,
                    onValueSelected = { hour ->
                        selectedHour = hour
                        onTimeSelected(selectedHour, selectedMinute)
                    }
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
                    range = 0..59,
                    initialValue = initialMinute,
                    modifier = Modifier.weight(1f),
                    speed = 0.5f,
                    onValueSelected = { minute ->
                        selectedMinute = minute
                        onTimeSelected(selectedHour, selectedMinute)
                    }
                )
            }
        }
    }
}


@Composable
private fun ClockRectangle(
    range: IntRange = 0..100,
    initialValue: Int = range.first,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    onValueSelected: (Int) -> Unit
) {
    var state by remember { mutableIntStateOf(initialValue) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(95.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        AppNumberPicker(
            range = range,
            modifier = Modifier.width(100.dp),
            flingMultiplier = speed,
            initialSelectedNumber = initialValue,
            onValueSelected = { value ->
                state = value
                onValueSelected(value)
            }
        )
    }
}


@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppClockPreview() {
    var hour by remember { mutableIntStateOf(12) }
    var minute by remember { mutableIntStateOf(35) }

    SnoozelooTheme {
        AppBackground {
            Box(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                AppClock(
                    initialHour = hour,
                    initialMinute = minute,
                    onTimeSelected = { newHour, newMinute ->
                        hour = newHour
                        minute = newMinute
                    }
                )
            }
        }
    }
}