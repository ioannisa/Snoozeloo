package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class DimmingState {
    var isDimmed by mutableStateOf(false)
}

val LocalDimmingState = compositionLocalOf { DimmingState() }


@Composable
fun AppScreen(
    content: @Composable () -> Unit
) {
    val dimmingState = remember { DimmingState() }

    CompositionLocalProvider(LocalDimmingState provides dimmingState) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackground {
                content()
            }

            // Dimmed background overlay
            if (dimmingState.isDimmed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
        }
    }
}
