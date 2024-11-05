package eu.anifantakis.snoozeloo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class MainState(
    val showSplash: Boolean = false,
)


class MainViewModel(): ViewModel() {

    var state by mutableStateOf(MainState())
        private set

    init {
        viewModelScope.launch {
            state = state.copy(showSplash = true)

            // simulate delay to fetch info
            delay(2.seconds)

            state = state.copy(
                showSplash = false
            )

        }
    }
}