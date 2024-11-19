package eu.anifantakis.snoozeloo.core.domain.util

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.State

fun <T> StateFlow<T>.toComposeState(scope: CoroutineScope): State<T> {
    val state = mutableStateOf(this.value)
    scope.launch {
        this@toComposeState.collect {
            state.value = it
        }
    }
    return state
}