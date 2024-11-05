package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst

@Composable
fun AppSwitch(
    initialState: Boolean = false,
    width: Int = 60,
    height: Int = 30,
    checkedThumbColor: Color = Color.White,
    uncheckedThumbColor: Color = Color.White,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedTrackColor: Color = UIConst.colorWithAlpha(MaterialTheme.colorScheme.primary, alpha = 0.3f),
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    onCheckedChange: (Boolean) -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(initialState) }

    // Animated offset for the thumb
    val offset by animateFloatAsState(
        targetValue = if (isChecked) (width - height).toFloat() else 0f,
        animationSpec = animationSpec,
        label = "switch_animation"
    )

    // Animate track color
    val trackColor by animateColorAsState(
        targetValue = if (isChecked) checkedTrackColor else uncheckedTrackColor,
        label = "track_color_animation"
    )

    // Animate thumb color
    val thumbColor by animateColorAsState(
        targetValue = if (isChecked) checkedThumbColor else uncheckedThumbColor,
        label = "thumb_color_animation"
    )



    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(height*2))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isChecked = !isChecked
                onCheckedChange(isChecked)
            }
    ) {
        Box(
            modifier = Modifier
                .height(height.dp - 4.dp)
                .width(height.dp - 4.dp)
                .offset(x = offset.dp + 2.dp)
                .offset(y = 2.dp)
                .clip(CircleShape)
                .background(thumbColor)
                .padding(2.dp)
        )
    }

}