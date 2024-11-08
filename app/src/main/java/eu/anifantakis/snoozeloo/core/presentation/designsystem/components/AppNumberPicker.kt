package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class NumberPickerState {
    var selectedNumber by mutableIntStateOf(0)
    var isScrollInProgress by mutableStateOf(false)
    private var onFinalValueCallback: ((Int) -> Unit)? = null

    fun setOnFinalValueSelected(callback: (Int) -> Unit) {
        onFinalValueCallback = callback
    }

    internal fun updateFinalValue(number: Int) {
        selectedNumber = number
        onFinalValueCallback?.invoke(number)
    }
}

@Composable
fun rememberNumberPickerState() = remember { NumberPickerState() }

class MultiplierFlingBehavior(
    private val baseFlingBehavior: FlingBehavior,
    private val velocityMultiplier: Float
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val adjustedVelocity = initialVelocity * velocityMultiplier
        return with(baseFlingBehavior) {
            performFling(adjustedVelocity)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNumberPicker(
    range: IntRange = 0..100,
    state: NumberPickerState = rememberNumberPickerState(),
    modifier: Modifier = Modifier,
    flingMultiplier: Float = 1f,
    initialSelectedNumber: Int = range.first,
    onValueSelected: (Int) -> Unit = {}
) {
    val numbers = range.toList()
    val visibleItemsCount = 3
    val visibleItemsMiddle = visibleItemsCount / 2

    val listScrollCount = Int.MAX_VALUE
    val listScrollMiddle = listScrollCount / 2

    // Calculate the offset for starting 5 numbers before the target
    val startOffset = 5
    val adjustedInitialNumber = if (initialSelectedNumber >= startOffset) {
        initialSelectedNumber - startOffset
    } else {
        // Handle wrapping around the range if needed
        range.last - (startOffset - initialSelectedNumber - 1)
    }

    // Calculate the starting index based on the adjusted initial number
    val initialNumberIndexInNumbers = numbers.indexOf(adjustedInitialNumber)
    val listStartIndex = listScrollMiddle - listScrollMiddle % numbers.size - visibleItemsMiddle + initialNumberIndexInNumbers + 1

    fun getNumber(index: Int) = numbers[index % numbers.size]

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex)
    val coroutineScope = rememberCoroutineScope()

    // Track whether initial animation has played
    var hasPlayedInitialAnimation by rememberSaveable { mutableStateOf(false) }

    val itemHeightPixels = remember { mutableIntStateOf(0) }

    // Perform initial scroll animation
    LaunchedEffect(itemHeightPixels.value) {
        if (!hasPlayedInitialAnimation && itemHeightPixels.value > 0) {
            coroutineScope.launch {
                var scrollDistance = itemHeightPixels.value * startOffset.toFloat()
                scrollDistance -= (itemHeightPixels.value / 5f)

                listState.animateScrollBy(
                    value = scrollDistance,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = EaseOutCirc
                    )
                )
            }
            hasPlayedInitialAnimation = true
        }
    }

    val baseFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val flingBehavior = remember(baseFlingBehavior, flingMultiplier) {
        MultiplierFlingBehavior(baseFlingBehavior, flingMultiplier)
    }

    val density = LocalDensity.current
    val wheelRadius = with(density) { 50.dp.toPx() }

    // Monitor scroll state for final value selection
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                state.isScrollInProgress = isScrolling
                if (!isScrolling) {
                    val finalValue = getNumber(listState.firstVisibleItemIndex + visibleItemsMiddle)
                    state.updateFinalValue(finalValue)
                    onValueSelected(finalValue)
                }
            }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { itemHeightPixels.value.toDp() } * visibleItemsCount)
        ) {
            items(listScrollCount) { index ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .onSizeChanged { size -> itemHeightPixels.value = size.height }
                        .fillMaxWidth()
                ) {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    val itemInfo = visibleItems.find { it.index == index }
                    val itemOffset = itemInfo?.offset ?: 0
                    val center = listState.layoutInfo.viewportEndOffset / 2f
                    val distanceFromCenter = itemOffset - center + (itemInfo?.size ?: 0) / 2f

                    val angleInRadians = (distanceFromCenter / wheelRadius)
                    val rotationAngle = (angleInRadians * 180f / PI).toFloat()
                    val yOffset = sin(angleInRadians) * wheelRadius * 0.3f

                    val scale = cos(angleInRadians).toFloat()
                    val alpha = when {
                        abs(rotationAngle) > 90f -> 0f
                        abs(rotationAngle) > 75f -> 0.2f
                        abs(rotationAngle) > 45f -> 0.5f
                        else -> scale.coerceIn(0.7f, 1f)
                    }

                    Text(
                        text = String.format("%02d", getNumber(index)),
                        style = LocalTextStyle.current.copy(
                            fontSize = 54.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .graphicsLayer {
                                rotationX = -rotationAngle
                                translationY = yOffset
                                scaleX = scale.coerceIn(0.65f, 1f)
                                scaleY = scale.coerceIn(0.65f, 1f)
                                cameraDistance = wheelRadius * 0.8f
                            }
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun TimePickerExample() {
    var selectedHour by remember { mutableStateOf(5) }  // Initial hour set to 5
    var selectedMinute by remember { mutableStateOf(2) } // Initial minute set to 2

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Hours picker
        AppNumberPicker(
            range = 0..23,
            modifier = Modifier.width(100.dp),
            flingMultiplier = 0.2f,
            initialSelectedNumber = selectedHour,
            onValueSelected = { hour ->
                selectedHour = hour
            }
        )

        Text(
            ":",
            fontSize = 54.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Minutes picker
        AppNumberPicker(
            range = 0..59,
            modifier = Modifier.width(100.dp),
            flingMultiplier = 0.5f,
            initialSelectedNumber = selectedMinute,
            onValueSelected = { minute ->
                selectedMinute = minute
            }
        )
    }

    Text(
        text = String.format("%02d:%02d", selectedHour, selectedMinute),
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 16.dp)
    )
}
