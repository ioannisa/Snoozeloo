package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

/** Visual configuration constants for the number picker UI **/
private const val WHEEL_RADIUS_DP = 50f         // The radius of the wheel effect in dp (density-independent pixels)
private const val VISIBLE_ITEMS_COUNT = 3       // Number of items visible at once; must be an odd number to center the selection
private const val START_OFFSET = 5              // Initial scroll offset for a smooth entry animation
private const val BUFFER_MULTIPLIER = 3         // Multiplier for the initial list size to enable infinite scrolling
private const val SCROLL_THRESHOLD = 0.7f       // Threshold (as a fraction) to trigger loading more items when scrolling
private const val ANIMATION_DURATION = 350      // Duration in milliseconds for reset and snap animations
private const val INITIAL_ANIMATION_DURATION = 800  // Duration in milliseconds for the initial entry animation

/** 3D effect configuration constants for the wheel-like appearance **/
private const val CAMERA_DISTANCE_MULTIPLIER = 12f  // Multiplier affecting the camera distance for 3D perspective
private const val Y_TRANSLATION_MULTIPLIER = 0.45f  // Multiplier for vertical translation in the wheel effect
private const val SCALE_MIN = 0.7f                 // Minimum scale for items at the edges of the wheel
private const val SCALE_MAX = 1f                   // Maximum scale for the center item
private const val ROTATION_MULTIPLIER = 0.7f       // Multiplier for the rotation angle of items
private const val PERSPECTIVE_MULTIPLIER = 0.8f    // Multiplier affecting the perspective distortion

/**
 * State holder for the number picker that manages:
 * - Currently selected number
 * - Scroll state (whether the picker is currently being scrolled)
 * - Value change callbacks to notify when the final value is selected
 */
class NumberPickerState {
    // The currently selected number in the picker.
    private var selectedNumber by mutableIntStateOf(0)

    // Flag to indicate whether the scroll is in progress.
    var isScrollInProgress by mutableStateOf(false)
        internal set

    // Callback function to be invoked when the final value is selected after scrolling.
    private var onFinalValueCallback: ((Int) -> Unit)? = null

    /**
     * Sets the callback to be invoked when the final value is selected.
     * @param callback A lambda function that receives the selected number.
     */
    fun setOnFinalValueSelected(callback: (Int) -> Unit) {
        onFinalValueCallback = callback
    }

    /**
     * Internal function to update the final value when scrolling stops.
     * @param number The number selected.
     */
    internal fun updateFinalValue(number: Int) {
        selectedNumber = number
        // Invoke the callback to notify about the final selected value.
        onFinalValueCallback?.invoke(number)
    }
}

/**
 * Composable function to remember and provide a NumberPickerState instance.
 * This ensures that the state survives recompositions.
 */
@Composable
fun rememberNumberPickerState() = remember { NumberPickerState() }

/**
 * Custom FlingBehavior that modifies scroll velocity for better control over scrolling dynamics.
 * @param velocityMultiplier Multiplier for the fling velocity.
 *        Values less than 1 slow down the scrolling; values greater than 1 speed it up.
 */
class MultiplierFlingBehavior(
    private val baseFlingBehavior: FlingBehavior, // The base FlingBehavior to delegate to.
    private val velocityMultiplier: Float
) : FlingBehavior {

    /**
     * Overrides the performFling function to adjust the initial velocity.
     * @param initialVelocity The initial velocity of the fling.
     * @return Remaining velocity after fling (if any).
     */
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // Adjust the initial velocity by the multiplier and delegate to the base behavior.
        return with(baseFlingBehavior) {
            performFling(initialVelocity * velocityMultiplier)
        }
    }
}

/**
 * Helper function for linear interpolation between two values.
 * @param start The start value.
 * @param stop The end value.
 * @param fraction The fraction between 0 and 1 to interpolate.
 * @return The interpolated value.
 */
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

/**
 * Main infinite number picker composable.
 * Creates a 3D wheel-like selector that can scroll infinitely in both directions.
 *
 * @param range The range of numbers to display (e.g., 0..23 for hours)
 * @param state State holder for the picker
 * @param modifier Modifier for customization
 * @param flingMultiplier Controls scroll speed/momentum
 * @param initialSelectedNumber Starting number
 * @param resetKey Triggers reset animation when changed
 * @param onValueSelected Callback for value changes
 */
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
    // STEP 1: Initialize base data structures.
    // Convert the provided range into a list of numbers.
    val numbers = range.toList()
    // Calculate the index of the middle item in the visible items.
    val visibleItemsMiddle = VISIBLE_ITEMS_COUNT / 2

    // STEP 2: Setup infinite scroll state.
    // Initialize the total number of items by multiplying the number of items in the range
    // by the BUFFER_MULTIPLIER. This creates an initial buffer to enable infinite scrolling.
    var totalItems by remember { mutableIntStateOf(numbers.size * BUFFER_MULTIPLIER) }
    // Counters to track how many times we have prepended or appended the numbers list
    // to support infinite scrolling.
    var prependCount by remember { mutableIntStateOf(1) }
    var appendCount by remember { mutableIntStateOf(1) }

    // STEP 3: Calculate initial scroll position.
    // Compute the initial position in the list where the picker should start.
    // We place the initial selected number in the middle of the list, adjusting for the START_OFFSET.
    // This helps with the entry animation and ensures the selected item is centered.
    val initialMiddlePosition = numbers.size +
            (numbers.indexOf(initialSelectedNumber) - START_OFFSET).mod(numbers.size)

    // STEP 4: Setup state management.
    // Remember the LazyListState for the LazyColumn, initialized to the calculated initial position.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialMiddlePosition)
    // Create a CoroutineScope for launching coroutines related to scrolling animations.
    val coroutineScope = rememberCoroutineScope()
    // Flag to ensure the initial entry animation plays only once.
    var hasPlayedInitialAnimation by rememberSaveable { mutableStateOf(false) }
    // Flag to indicate whether the auto-scrolling (entry animation) has completed.
    var autoScrollingCompleted by rememberSaveable { mutableStateOf(false) }
    // Variable to store the height of each item in pixels, used for calculating scroll distances.
    var itemHeightPixels by remember { mutableIntStateOf(0) }

    // STEP 5: Setup haptic feedback.
    // Obtain the current HapticFeedback instance from the local context.
    val hapticFeedback = LocalHapticFeedback.current
    // Variable to keep track of the previous center item index to detect changes.
    var previousCenterItemIndex by remember { mutableIntStateOf(-1) }

    // STEP 6: Monitor scroll position for infinite scroll.
    // Launch a coroutine to monitor changes in the first visible item index of the list.
    LaunchedEffect(listState) {
        // Use snapshotFlow to observe changes to the firstVisibleItemIndex.
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged() // Only react when the index actually changes.
            .collect { firstVisibleIndex ->
                // Calculate the last visible index based on the visible items count.
                val lastVisibleIndex = firstVisibleIndex + VISIBLE_ITEMS_COUNT

                // Check if the user has scrolled near the start of the list.
                if (firstVisibleIndex < numbers.size * SCROLL_THRESHOLD) {
                    // Increase the prepend count and total items.
                    prependCount++
                    totalItems += numbers.size
                    // Adjust the scroll position to account for the new items added at the start.
                    coroutineScope.launch {
                        listState.scrollToItem(firstVisibleIndex + numbers.size)
                    }
                }

                // Check if the user has scrolled near the end of the list.
                if (lastVisibleIndex > totalItems - (numbers.size * SCROLL_THRESHOLD)) {
                    // Increase the append count and total items.
                    appendCount++
                    totalItems += numbers.size
                }
            }
    }

    // STEP 7: Handle reset animations.
    // When the resetKey changes, trigger a reset animation to scroll back to the initial selected number.
    LaunchedEffect(resetKey) {
        if (listState.firstVisibleItemIndex != initialMiddlePosition) {
            // Get the currently centered value.
            val finalValue = numbers[(listState.firstVisibleItemIndex +
                    visibleItemsMiddle) % numbers.size]
            // Calculate the offset needed to scroll to the initial selected number.
            val offset = calculateOffset(finalValue, initialSelectedNumber, range)
            // Calculate the scroll distance in pixels.
            val scrollDistance = itemHeightPixels * offset.toFloat()

            // Launch a coroutine to animate the scroll.
            coroutineScope.launch {
                listState.animateScrollBy(
                    value = scrollDistance,
                    animationSpec = tween(
                        durationMillis = ANIMATION_DURATION,
                        easing = EaseOutCirc
                    )
                )
            }
        }
    }

    // STEP 8: Track layout info for 3D transformations.
    // Remember a derived state that provides the list of currently visible items
    // and calculates the center position of the viewport. This is used to apply
    // the 3D transformations to each item based on its position relative to the center.
    val layoutInfo = remember {
        derivedStateOf {
            // Get the list of visible items.
            val items = listState.layoutInfo.visibleItemsInfo
            // Calculate the center of the viewport.
            val viewportCenter = listState.layoutInfo.viewportEndOffset / 2f
            // Return both as a pair.
            Pair(items, viewportCenter)
        }
    }

    // STEP 9: Setup initial animation.
    // When the item height is available and the initial animation hasn't played yet.
    LaunchedEffect(itemHeightPixels) {
        if (!hasPlayedInitialAnimation && itemHeightPixels > 0) {
            coroutineScope.launch {
                autoScrollingCompleted = false
                // Calculate the total scroll distance for the entry animation.
                var scrollDistance = itemHeightPixels * START_OFFSET.toFloat()
                // Adjust slightly to prevent overshooting.
                scrollDistance -= (itemHeightPixels / 5f)

                // Animate the scroll by the calculated distance.
                listState.animateScrollBy(
                    value = scrollDistance,
                    animationSpec = tween(
                        durationMillis = INITIAL_ANIMATION_DURATION,
                        easing = EaseOutCirc
                    )
                )
                autoScrollingCompleted = true
            }
            hasPlayedInitialAnimation = true
        }
    }

    // STEP 10: Configure scroll behavior.
    // Get the base fling behavior that snaps to items.
    val baseFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    // Create a custom fling behavior that adjusts the scrolling speed using the MultiplierFlingBehavior.
    val flingBehavior = remember(baseFlingBehavior, flingMultiplier) {
        MultiplierFlingBehavior(baseFlingBehavior, flingMultiplier)
    }
    // Get the current density for converting dp to pixels.
    val density = LocalDensity.current
    // Calculate the wheel radius in pixels.
    val wheelRadius = with(density) { WHEEL_RADIUS_DP.dp.toPx() }

    // STEP 11: Handle haptic feedback.
    // Monitor changes to the center item index to provide haptic feedback when the center item changes.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex + visibleItemsMiddle }
            .distinctUntilChanged()
            .collect { currentCenterItemIndex ->
                // If the center item index has changed (and it's not the initial state).
                if (previousCenterItemIndex != -1 &&
                    previousCenterItemIndex != currentCenterItemIndex) {
                    // Perform haptic feedback to indicate item change.
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                // Update the previous center item index.
                previousCenterItemIndex = currentCenterItemIndex
            }
    }

    // STEP 12: Track scroll state and update values.
    // Monitor the isScrollInProgress state of the list to detect when scrolling starts and stops.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                // Update the isScrollInProgress in our state.
                state.isScrollInProgress = isScrolling
                if (!isScrolling) {
                    // When scrolling has stopped, determine the final selected value.
                    val finalValue = numbers[(listState.firstVisibleItemIndex +
                            visibleItemsMiddle) % numbers.size]
                    // Update the state with the final selected number.
                    state.updateFinalValue(finalValue)

                    // If the initial auto-scrolling animation has completed, invoke the callback.
                    if (autoScrollingCompleted) {
                        onValueSelected(finalValue)
                    }
                }
            }
    }

    // STEP 13: Main layout.
    // Create a Box as the container for the number picker.
    Box(modifier = modifier) {
        // Use a LazyColumn to display the list of numbers.
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior, // Use the custom fling behavior.
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                // Set the height of the LazyColumn to fit the visible items.
                .height(with(density) { itemHeightPixels.toDp() } * VISIBLE_ITEMS_COUNT)
        ) {
            // Provide the items for the LazyColumn.
            items(
                count = totalItems,
                key = { index -> index } // Use the index as the key to maintain item identity.
            ) { index ->
                // Calculate the actual index in the numbers list using modulo operation.
                val actualIndex = index.mod(numbers.size)
                // Display each number item with appropriate transformations.
                NumberItem(
                    index = index,
                    getNumber = { numbers[actualIndex] }, // Get the number to display.
                    layoutInfo = layoutInfo.value, // Pass the layout info for transformations.
                    wheelRadius = wheelRadius, // Pass the wheel radius.
                    modifier = Modifier
                        // Capture the item height when it's positioned.
                        .onGloballyPositioned { coordinates ->
                            itemHeightPixels = coordinates.size.height
                        }
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Individual number item composable with 3D wheel effect.
 * Applies 3D transformations to create the wheel-like appearance.
 *
 * @param index The index of the item in the list.
 * @param getNumber A lambda function to get the number to display.
 * @param layoutInfo Pair containing the list of visible items and the center of the viewport.
 * @param wheelRadius The radius of the wheel effect in pixels.
 * @param modifier Modifier for customization.
 */
@Composable
private fun NumberItem(
    index: Int,
    getNumber: () -> Int,
    layoutInfo: Pair<List<LazyListItemInfo>, Float>,
    wheelRadius: Float,
    modifier: Modifier = Modifier
) {
    // Unpack the layout info.
    val (visibleItems, center) = layoutInfo

    // Find the layout information for this item.
    val itemInfo = visibleItems.find { it.index == index }

    // Get the offset and size of the item, defaulting to 0 if not found.
    val itemOffset = itemInfo?.offset ?: 0
    val itemSize = itemInfo?.size ?: 0

    // Calculate the distance of the item from the center of the viewport.
    val distanceFromCenter = (itemOffset - center + itemSize / 2f) / wheelRadius

    // Convert the distance into an angle in radians for trigonometric calculations.
    val angleInRadians = (distanceFromCenter * PI / 2).toFloat()

    // Calculate the rotation angle based on the sine of the angle, scaled by the multiplier.
    val rotationAngle = sin(angleInRadians) * 90f * ROTATION_MULTIPLIER

    // Calculate the vertical translation (y-axis offset) to position the item along the wheel curve.
    val yOffset = sin(angleInRadians) * wheelRadius * Y_TRANSLATION_MULTIPLIER *
            (1 - abs(cos(angleInRadians * PERSPECTIVE_MULTIPLIER)))

    // Calculate the scale factor based on the cosine of the angle, affecting size.
    val scale = cos(angleInRadians * PERSPECTIVE_MULTIPLIER)

    // Calculate the alpha (opacity) to fade out items towards the edges.
    val normalizedDistance = abs(distanceFromCenter)
    val alpha = cos(normalizedDistance * PI / 2f).coerceIn(0.0, 1.0)

    // Build the UI.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Display the number with transformations.
        AppText52(
            text = String.format(Locale.ROOT, "%02d", getNumber()),
            color = Color.DarkGray,
            modifier = Modifier
                .graphicsLayer {
                    // Apply rotation along the X-axis.
                    rotationX = -rotationAngle
                    // Apply vertical translation.
                    translationY = yOffset
                    // Calculate and apply scaling.
                    val targetScale = lerp(
                        start = SCALE_MIN,
                        stop = SCALE_MAX,
                        fraction = scale.coerceIn(0f, 1f)
                    )
                    scaleX = targetScale
                    scaleY = targetScale
                    // Set camera distance for 3D effect.
                    cameraDistance = wheelRadius * CAMERA_DISTANCE_MULTIPLIER
                }
                // Apply alpha for fading effect.
                .alpha(alpha.toFloat())
        )
    }
}

/**
 * Calculate scroll offset for number alignment.
 *
 * Determines the number of positions to scroll to align the final value with the initial selected number.
 *
 * @param finalValue The value currently selected in the picker.
 * @param initialSelectedNumber The target value to align to.
 * @param range The range of valid numbers.
 * @return The offset (number of positions) to scroll.
 */
private fun calculateOffset(finalValue: Int, initialSelectedNumber: Int, range: IntRange): Int {
    return when {
        // Special case when wrapping from last to first.
        finalValue == range.last && initialSelectedNumber == range.first -> 1
        // Special case when wrapping from first to last.
        finalValue == range.first && initialSelectedNumber == range.last -> -1
        else -> {
            // Calculate the normal difference.
            val normalDiff = initialSelectedNumber - finalValue
            // Calculate the wraparound difference (taking into account wrapping).
            val wraparoundDiff = when {
                normalDiff > 0 -> normalDiff - range.last - 1
                normalDiff < 0 -> normalDiff + range.last + 1
                else -> 0
            }
            // Return the smallest absolute difference.
            if (abs(normalDiff) <= abs(wraparoundDiff)) normalDiff else wraparoundDiff
        }
    }
}

/**
 * Preview composable function demonstrating how to use the AppNumberPicker
 * to create a time picker with hours and minutes.
 */
@Preview
@Composable
fun TimePickerExample() {
    // State variables to hold the selected hour and minute.
    var selectedHour by remember { mutableIntStateOf(5) }
    var selectedMinute by remember { mutableIntStateOf(2) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row to arrange the hour and minute pickers horizontally.
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Hour picker from 0 to 23.
            AppNumberPicker(
                range = 0..23,
                modifier = Modifier.width(100.dp),
                flingMultiplier = 0.2f, // Adjusts the scroll speed.
                initialSelectedNumber = selectedHour,
                onValueSelected = { hour ->
                    selectedHour = hour
                }
            )

            // Separator between hours and minutes.
            AppText52(
                ":",
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minute picker from 0 to 59.
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

        // Display the selected time.
        AppText16(
            text = String.format(Locale.ROOT, "%02d:%02d", selectedHour, selectedMinute),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
