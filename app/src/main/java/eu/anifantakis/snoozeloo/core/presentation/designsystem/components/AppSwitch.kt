package eu.anifantakis.snoozeloo.core.presentation.designsystem.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import eu.anifantakis.snoozeloo.core.presentation.designsystem.UIConst
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme

@Composable
fun AppSwitch(
    checked: Boolean,
    width: Int = 60,
    height: Int = 30,
    checkedThumbColor: Color = MaterialTheme.colorScheme.onPrimary,
    uncheckedThumbColor: Color = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedTrackColor: Color = UIConst.colorWithAlpha(MaterialTheme.colorScheme.primary, alpha = 0.3f),
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    uncheckedText: String = "",
    checkedText: String = "",
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    // Animated offset for the thumb
    val offset by animateFloatAsState(
        targetValue = if (checked) (width - height).toFloat() else 0f,
        animationSpec = animationSpec,
        label = "switch_animation"
    )

    // Animate track color
    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        label = "track_color_animation"
    )

    // Animate thumb color
    val thumbColor by animateColorAsState(
        targetValue = if (checked) checkedThumbColor else uncheckedThumbColor,
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
                onCheckedChange(!checked)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(UIConst.padding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            AppText12(checkedText, color = textColor, fontWeight = FontWeight.W900)
            AppText12(uncheckedText, color = textColor, fontWeight = FontWeight.W900)
        }

        Box(
            modifier = Modifier
                .height(height.dp - 4.dp)
                .width(height.dp - 4.dp)
                .offset { IntOffset(
                    x = (offset + 2).dp.roundToPx(),
                    y = 2.dp.roundToPx())
                }
                .clip(CircleShape)
                .background(thumbColor)
                .padding(2.dp)
        )
    }
}


@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppSwitchPreview() {
    SnoozelooTheme {
        AppBackground {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(UIConst.paddingSmall)
            ) {
                AppSwitch(
                    checked = false,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                ) {

                }

                AppSwitch(
                    checked = false,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
            }
        }
    }
}