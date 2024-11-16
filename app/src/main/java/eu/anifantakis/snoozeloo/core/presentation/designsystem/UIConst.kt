package eu.anifantakis.snoozeloo.core.presentation.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

object UIConst {

    val padding = 16.dp
    val borderRadius = 4.dp
    val paddingDouble = 32.dp
    val paddingSmall = 8.dp
    val paddingSmaller = 6.dp
    val paddingExtraSmall = 4.dp

    fun grayOutColor(color: Color, blendFactor: Float = 0.5f): Color {
        return lerp(color, Color.Gray, blendFactor)
    }

    fun colorWithAlpha(color: Color, alpha: Float = 0.1f): Color {
        return color.copy(alpha = alpha)
    }
}