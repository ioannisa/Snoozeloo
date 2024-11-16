package eu.anifantakis.snoozeloo.core.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItem(
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart,
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDismiss()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Your background content when swiping
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red)
            )
        },
        content = {
            // Your main content here
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Swipe me to dismiss",
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,  // Only allow end-to-start swipe
        enableDismissFromEndToStart = true
    )
}