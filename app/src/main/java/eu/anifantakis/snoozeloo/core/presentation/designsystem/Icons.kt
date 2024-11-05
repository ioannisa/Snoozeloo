package eu.anifantakis.snoozeloo.core.presentation.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import eu.anifantakis.snoozeloo.R

object Icons {

    val alarm: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.alarm)

}