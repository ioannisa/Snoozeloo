package eu.anifantakis.snoozeloo.core.presentation.designsystem.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import eu.anifantakis.snoozeloo.core.presentation.designsystem.screens.AlarmScreenRoot

@Composable
fun NavigationRoot(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "alarm"
    ) {
        composable(route = "alarm") {
            AlarmScreenRoot()
        }
    }
}