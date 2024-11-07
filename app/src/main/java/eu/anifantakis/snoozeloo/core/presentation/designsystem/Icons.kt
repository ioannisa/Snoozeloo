package eu.anifantakis.snoozeloo.core.presentation.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import eu.anifantakis.snoozeloo.R

object Icons {

    // icon definition via stored resources
    val alarm: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.alarm)

    val colon: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.colon)

    // icon via material icons library
    val add: ImageVector
        @Composable
        get() = Icons.Outlined.Add

    val delete: ImageVector
        @Composable
        get() = Icons.Default.Delete

    val bellOn: ImageVector
        @Composable
        get() = Icons.Outlined.NotificationsActive

    val bellOff: ImageVector
        @Composable
        get() = Icons.Outlined.NotificationsOff

    val close: ImageVector
        @Composable
        get() = Icons.Outlined.Close

    val back: ImageVector
        @Composable
        get() = Icons.AutoMirrored.Outlined.ArrowBack
}