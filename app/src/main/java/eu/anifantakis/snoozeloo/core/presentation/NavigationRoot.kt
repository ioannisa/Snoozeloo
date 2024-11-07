package eu.anifantakis.snoozeloo.core.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppBackground
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmmaster.AlarmScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.clockscreen.ClockScreen
import kotlinx.serialization.Serializable

sealed interface NavGraph {
    @Serializable data object AlarmScreen: NavGraph
    @Serializable data object ClockScreen: NavGraph
}

@Composable
fun NavigationRoot(
    innerPadding: PaddingValues,
    navController: NavHostController,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppBackground {
            NavHost(
                navController = navController,
                startDestination = NavGraph.AlarmScreen,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                        )
                    )
            ) {
                composable<NavGraph.AlarmScreen> {
                    AlarmScreenRoot(onClockClick = {
                        println("HERE WE ARE!!")
                        navController.navigate(NavGraph.ClockScreen)
                    })
                }

                composable<NavGraph.ClockScreen> {
                    ClockScreen()
                }
            }
        }
    }
}