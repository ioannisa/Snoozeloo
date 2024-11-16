package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class NumberPickerState {
    private var selectedNumber by mutableIntStateOf(0)
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

@Composable
fun AppNumberPicker(
    range: IntRange = 0..100,
    state: NumberPickerState = rememberNumberPickerState(),
    modifier: Modifier = Modifier,
    flingMultiplier: Float = 1f,
    initialSelectedNumber: Int = range.first,
    resetKey: Any = Unit,
    onValueSelected: (Int) -> Unit = {}
) {
    val numbers = range.toList()
    val visibleItemsCount = 3
    val visibleItemsMiddle = visibleItemsCount / 2

    val listScrollCount = Int.MAX_VALUE
    val listScrollMiddle = listScrollCount / 2

    val startOffset = 5
    val adjustedInitialNumber = if (initialSelectedNumber >= startOffset) {
        initialSelectedNumber - startOffset
    } else {
        range.last - (startOffset - initialSelectedNumber - 1)
    }

    val initialNumberIndexInNumbers = numbers.indexOf(adjustedInitialNumber)
    val listStartIndex = listScrollMiddle - listScrollMiddle % numbers.size - visibleItemsMiddle + initialNumberIndexInNumbers + 1

    fun getNumber(index: Int) = numbers[index % numbers.size]

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex)
    val coroutineScope = rememberCoroutineScope()

    var hasPlayedInitialAnimation by rememberSaveable { mutableStateOf(false) }
    var autoScrollingCompleted by rememberSaveable { mutableStateOf(false) }
    val itemHeightPixels = remember { mutableIntStateOf(0) }

    // Reset handling
    LaunchedEffect(resetKey) {
        if (listState.firstVisibleItemIndex != listStartIndex) {
            val finalValue = getNumber(listState.firstVisibleItemIndex + visibleItemsMiddle)

            // Calculate shortest path considering wraparound
            val offset = when {
                // If going from 59 to 0, we want to go forward
                finalValue == range.last && initialSelectedNumber == range.first -> 1
                // If going from 0 to 59, we want to go backward
                finalValue == range.first && initialSelectedNumber == range.last -> -1
                // Normal case: calculate shortest distance considering wraparound
                else -> {
                    val normalDiff = initialSelectedNumber - finalValue
                    val wraparoundDiff = when {
                        normalDiff > 0 -> normalDiff - range.last - 1
                        normalDiff < 0 -> normalDiff + range.last + 1
                        else -> 0
                    }
                    // Use the smallest absolute difference
                    if (abs(normalDiff) <= abs(wraparoundDiff)) normalDiff else wraparoundDiff
                }
            }

            val scrollDistance = itemHeightPixels.intValue * offset.toFloat()

            coroutineScope.launch {
                listState.animateScrollBy(
                    value = scrollDistance,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseOutCirc
                    )
                )
            }
        }
    }

    val layoutInfo = remember {
        derivedStateOf {
            val items = listState.layoutInfo.visibleItemsInfo
            val viewportCenter = listState.layoutInfo.viewportEndOffset / 2f
            Pair(items, viewportCenter)
        }
    }

    // Launches animation when picker first shows
    LaunchedEffect(itemHeightPixels.intValue) {
        if (!hasPlayedInitialAnimation && itemHeightPixels.intValue > 0) {
            coroutineScope.launch {
                autoScrollingCompleted = false
                var scrollDistance = itemHeightPixels.intValue * startOffset.toFloat()
                scrollDistance -= (itemHeightPixels.intValue / 5f)

                // The animateScrollBy returns when the animation completes
                listState.animateScrollBy(
                    value = scrollDistance,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = EaseOutCirc
                    )
                )

                // This line will execute after animation completes
                autoScrollingCompleted = true
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

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                state.isScrollInProgress = isScrolling
                if (!isScrolling) {
                    val finalValue = getNumber(listState.firstVisibleItemIndex + visibleItemsMiddle)
                    state.updateFinalValue(finalValue)

                    // update selected values only after initial is finished playing
                    if (autoScrollingCompleted) {
                        onValueSelected(finalValue)
                    }
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
                .height(with(density) { itemHeightPixels.intValue.toDp() } * visibleItemsCount)
        ) {
            items(listScrollCount) { index ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .onSizeChanged { size -> itemHeightPixels.intValue = size.height }
                        .fillMaxWidth()
                ) {
                    val (visibleItems, center) = layoutInfo.value
                    val itemInfo = visibleItems.find { it.index == index }
                    val itemOffset = itemInfo?.offset ?: 0
                    val distanceFromCenter = itemOffset - center + (itemInfo?.size ?: 0) / 2f

                    val angleInRadians = (distanceFromCenter / wheelRadius)
                    val rotationAngle = (angleInRadians * 180f / PI).toFloat()
                    val yOffset = sin(angleInRadians) * wheelRadius * 0.3f

                    val scale = cos(angleInRadians)
                    val alpha = when {
                        abs(rotationAngle) > 90f -> 0f
                        abs(rotationAngle) > 75f -> 0.2f
                        abs(rotationAngle) > 45f -> 0.5f
                        else -> scale.coerceIn(0.7f, 1f)
                    }

                    AppText52(
                        text = String.format(Locale.ROOT, "%02d", getNumber(index)),
                        color = Color.DarkGray,
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
    var selectedHour by remember { mutableIntStateOf(5) }
    var selectedMinute by remember { mutableIntStateOf(2) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
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
        text = String.format(Locale.ROOT, "%02d:%02d", selectedHour, selectedMinute),
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 16.dp)
    )
}