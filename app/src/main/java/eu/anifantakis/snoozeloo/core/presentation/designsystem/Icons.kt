package eu.anifantakis.snoozeloo.core.presentation.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import eu.anifantakis.snoozeloo.R

object Icons {

    // icon definition via stored resources
    val alarm: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.alarm)


    // icon via material icons library
    val add: ImageVector
        @Composable
        get() = Icons.Outlined.Add

}