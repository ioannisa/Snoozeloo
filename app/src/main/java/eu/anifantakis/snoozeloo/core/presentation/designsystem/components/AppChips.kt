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
import eu.anifantakis.snoozeloo.alarm.domain.DaysOfWeek
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
    modifier: Modifier = Modifier,
    onSelectedChanged: (Boolean) -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = { if (day.enabled) onSelectedChanged(!selected) },
        label = {
            AppText12(
                text = day.label,
                color = MaterialTheme.colorScheme.surfaceDim
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
    modifier: Modifier = Modifier,
    onError: () -> Unit = {},
    onSelectionChanged: (Set<String>) -> Unit,
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
                    if (!selected && selectedDays.size <= 1 && selectedDays.contains(day.code)) {
                        // Attempting to deselect the last selected day
                        onError()
                    } else {
                        val newSelection = if (selected) {
                            selectedDays + day.code
                        } else {
                            selectedDays - day.code
                        }
                        onSelectionChanged(newSelection)
                    }
                }
            )
        }
    }
}

@Composable
fun AppWeeklyChips(
    modifier: Modifier = Modifier,
    selectedDays: DaysOfWeek,  // Changed from initialSelection: Map<String, Boolean>
    onError: () -> Unit = {},
    onSelectionChanged: (DaysOfWeek) -> Unit,
) {
    // Define the days
    val days = remember {
        listOf(
            DayChip("mo", "Mo"),
            DayChip("tu", "Tu"),
            DayChip("we", "We"),
            DayChip("th", "Th"),
            DayChip("fr", "Fr"),
            DayChip("sa", "Sa"),
            DayChip("su", "Su")
        )
    }

    // Initialize selectedDays based on DaysOfWeek
    var selectedDaySet by remember(selectedDays) {
        mutableStateOf(buildSet {
            if (selectedDays.mo) add("mo")
            if (selectedDays.tu) add("tu")
            if (selectedDays.we) add("we")
            if (selectedDays.th) add("th")
            if (selectedDays.fr) add("fr")
            if (selectedDays.sa) add("sa")
            if (selectedDays.su) add("su")
        })
    }

    DayFilterChipGroup(
        days = days,
        selectedDays = selectedDaySet,
        onSelectionChanged = { newSelection ->
            selectedDaySet = newSelection

            val daysOfWeek = DaysOfWeek(
                mo = newSelection.contains("mo"),
                tu = newSelection.contains("tu"),
                we = newSelection.contains("we"),
                th = newSelection.contains("th"),
                fr = newSelection.contains("fr"),
                sa = newSelection.contains("sa"),
                su = newSelection.contains("su")
            )

            onSelectionChanged(daysOfWeek)
        },
        onError = onError,
        modifier = modifier
    )
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun WeeklyChipsPreview() {
    var selectedDays by remember {
        mutableStateOf(
            DaysOfWeek(
                mo = false,
                tu = false,
                we = false,
                th = false,
                fr = false,
                sa = false,
                su = false
            )
        )
    }

    SnoozelooTheme {
        AppBackground {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppWeeklyChips(
                    selectedDays = selectedDays,
                    onSelectionChanged = { newDays ->
                        selectedDays = newDays
                    }
                )

                // Show what's selected
                Text(
                    text = buildString {
                        val selectedList = mutableListOf<String>()
                        if (selectedDays.mo) selectedList.add("MO")
                        if (selectedDays.tu) selectedList.add("TU")
                        if (selectedDays.we) selectedList.add("WE")
                        if (selectedDays.th) selectedList.add("TH")
                        if (selectedDays.fr) selectedList.add("FR")
                        if (selectedDays.sa) selectedList.add("SA")
                        if (selectedDays.su) selectedList.add("SU")

                        append(if (selectedList.isEmpty()) {
                            "No days selected"
                        } else {
                            selectedList.joinToString(", ")
                        })
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}