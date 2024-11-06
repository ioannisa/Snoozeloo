package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme

private data class DayChip(
    val code: String,
    val label: String,
    val enabled: Boolean = true
)

@Composable
private fun DayFilterChip(
    day: DayChip,
    selected: Boolean,
    onSelectedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = { if (day.enabled) onSelectedChanged(!selected) },
        label = {
            Text(
                text = day.label,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = modifier.defaultMinSize(
            minWidth = 32.dp,
            minHeight = 32.dp
        ),
        enabled = day.enabled,
        shape = RoundedCornerShape(38.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderWidth = 0.dp,
            enabled = true,
            selected = true,
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayFilterChipGroup(
    days: List<DayChip>,
    selectedDays: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { day ->
            DayFilterChip(
                day = day,
                selected = selectedDays.contains(day.code),
                onSelectedChanged = { selected ->
                    val newSelection = if (selected) {
                        selectedDays + day.code
                    } else {
                        selectedDays - day.code
                    }
                    onSelectionChanged(newSelection)
                }
            )
        }
    }
}

@Composable
fun AppWeeklyChips(

    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectionChanged: (Map<String, Boolean>) -> Unit,
) {
    // Define the days
    val days = remember {
        listOf(
            DayChip("mo", "Mo", enabled),
            DayChip("tu", "Tu", enabled),
            DayChip("we", "We", enabled),
            DayChip("th", "Th", enabled),
            DayChip("fr", "Fr", enabled),
            DayChip("sa", "Sa", enabled),
            DayChip("su", "Su", enabled)
        )
    }

    // Maintain selection state
    var selectedDays by remember { mutableStateOf(emptySet<String>()) }

    DayFilterChipGroup(
        days = days,
        selectedDays = selectedDays,
        onSelectionChanged = { newSelection ->
            selectedDays = newSelection
            // Convert to map of day codes to boolean selection state
            val daysMap = days.associate {
                it.code to newSelection.contains(it.code)
            }
            onSelectionChanged(daysMap)
        },
        modifier = modifier
    )
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun WeeklyChipsPreview() {
    var selectedDays by remember { mutableStateOf(emptyMap<String, Boolean>()) }

    SnoozelooTheme {
        AppBackground {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppWeeklyChips(
                    onSelectionChanged = {
                        selectedDays = it
                    }
                )

                // Show what's selected
                Text(
                    text = selectedDays.entries
                        .filter { it.value }
                        .map { it.key.uppercase() }
                        .joinToString(", ")
                        .ifEmpty { "No days selected" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}