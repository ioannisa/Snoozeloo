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
import androidx.navigation.toRoute
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarm.AlarmScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarmedit.AlarmEditScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.ringtonesetting.RingtoneSettingScreenRoot
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppScreen
import kotlinx.serialization.Serializable

sealed interface NavGraph {
    @Serializable data object Alarms: NavGraph
    @Serializable data class AlarmEditor(val alarmId: String): NavGraph
    @Serializable data class RingtoneSetting(val alarmId: String): NavGraph
}

@Composable
fun NavigationRoot(
    innerPadding: PaddingValues,
    navController: NavHostController,
) {
    AppScreen {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = NavGraph.Alarms,
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
                composable<NavGraph.Alarms> {
                    AlarmScreenRoot(onOpenAlarmEditor = { alarmId ->
                        println("HERE WE ARE!!")
                        navController.navigate(NavGraph.AlarmEditor(alarmId))
                    })
                }

                composable<NavGraph.AlarmEditor> {
                    val args = it.toRoute<NavGraph.AlarmEditor>()
                    val alarmId = args.alarmId

                    AlarmEditScreenRoot(
                        alarmId = alarmId,
                        onOpenRingtoneSetting = {
                            navController.navigate(NavGraph.RingtoneSetting(alarmId))
                        },
                        onClose = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<NavGraph.RingtoneSetting> {
                    val args = it.toRoute<NavGraph.RingtoneSetting>()
                    val alarmId = args.alarmId
                    RingtoneSettingScreenRoot(
                        alarmId = alarmId,
                        onGoBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}