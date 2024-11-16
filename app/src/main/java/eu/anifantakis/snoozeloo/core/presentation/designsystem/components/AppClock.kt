package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.Icons
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst

@Composable
fun AppClock(
    initialHour: Int,
    initialMinute: Int,
    nextAlarmAt: String = "",
    canShowNextAlarm: Boolean = false,
    resetKey: Any = Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    // Use resetKey in remember to force reset when key changes
    var selectedHour by remember(resetKey) { mutableIntStateOf(initialHour) }
    var selectedMinute by remember(resetKey) { mutableIntStateOf(initialMinute) }

    Box {
        AppCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    ClockRectangle(
                        range = 0..23,
                        initialValue = initialHour,
                        resetKey = resetKey, // Pass the key down
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
                        resetKey = resetKey, // Pass the key down
                        modifier = Modifier.weight(1f),
                        speed = 0.5f,
                        onValueSelected = { minute ->
                            selectedMinute = minute
                            onTimeSelected(selectedHour, selectedMinute)
                        }
                    )
                }

                AppText16(
                    if (canShowNextAlarm) nextAlarmAt else " ",
                    modifier = Modifier.padding(vertical = UIConst.paddingSmall)
                )
            }
        }
    }
}

@Composable
private fun ClockRectangle(
    range: IntRange = 0..100,
    initialValue: Int = range.first,
    resetKey: Any = Unit, // Add reset key here too
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    onValueSelected: (Int) -> Unit
) {
    var state by remember(resetKey) { mutableIntStateOf(initialValue) }

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
            resetKey = resetKey,
            initialSelectedNumber = initialValue,
            onValueSelected = { value ->
                state = value
                onValueSelected(value)
            }
        )
    }
}