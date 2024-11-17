package eu.anifantakis.snoozeloo.core.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms.AlarmsScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.alarmtonesetting.AlarmToneSettingScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor.AlarmEditScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor.AlarmEditViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor.AlarmEditorScreenAction
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

sealed interface NavGraph {
    @Serializable data object Alarms: NavGraph
    @Serializable data class AlarmEditor(val alarmId: String): NavGraph
    @Serializable data class RingtoneSetting(val alarmToneUri: String?): NavGraph
}

@Composable
fun NavigationRoot(
    innerPadding: PaddingValues,
    navController: NavHostController,
) {
    // State to hold navigation results
    var ringtoneSelectionResult by remember { mutableStateOf<Pair<String, String?>?>(null) }

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
                    AlarmsScreenRoot(onOpenAlarmEditor = { alarmId ->
                        navController.navigate(NavGraph.AlarmEditor(alarmId))
                    })
                }

                composable<NavGraph.AlarmEditor> {
                    val args = it.toRoute<NavGraph.AlarmEditor>()
                    val alarmId = args.alarmId
                    val viewModel: AlarmEditViewModel = koinViewModel(parameters = { parametersOf(alarmId) })

                    // Pass the result to AlarmEditScreen when returning from RingtoneSetting
                    ringtoneSelectionResult?.let { (title, uri) ->
                        LaunchedEffect(title, uri) {
                            // Clear the result after consuming it
                            ringtoneSelectionResult = null
                            // Update the ViewModel with both title and URI
                            viewModel.onAction(AlarmEditorScreenAction.UpdateRingtoneResult(title, uri))
                        }
                    }

                    AlarmEditScreenRoot(
                        viewModel = viewModel,
                        onOpenRingtoneSetting = {
                            navController.navigate(NavGraph.RingtoneSetting(viewModel.alarmUiState.value?.alarm?.ringtoneUri))
                        },
                        onClose = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<NavGraph.RingtoneSetting> {
                    val args = it.toRoute<NavGraph.RingtoneSetting>()
                    val alarmToneUri = args.alarmToneUri

                    AlarmToneSettingScreenRoot(
                        alarmToneUri = alarmToneUri,
                        onGoBack = { title, selectedUri ->
                            // Store both title and URI as a Pair
                            ringtoneSelectionResult = Pair(title, selectedUri)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}