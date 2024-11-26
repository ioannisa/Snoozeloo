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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import eu.anifantakis.navhelper.navtype.mapper
import eu.anifantakis.snoozeloo.alarm.domain.Alarm
import eu.anifantakis.snoozeloo.alarm.presentation.screens.alarms.AlarmsScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.alarmtonesetting.AlarmToneSettingScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor.AlarmEditScreenRoot
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor.AlarmEditViewModel
import eu.anifantakis.snoozeloo.alarm.presentation.screens.editor.maineditor.AlarmEditorScreenAction
import eu.anifantakis.snoozeloo.core.presentation.designsystem.components.AppScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.typeOf

/**
 * Sealed interface defining the navigation destinations in the app.
 * Uses Serializable for type-safe navigation arguments.
 */
sealed interface NavGraph {
    @Serializable data object Alarms: NavGraph
    @Serializable data class AlarmEditor(val alarm: Alarm): NavGraph
    @Serializable data class RingtoneSetting(val alarmToneUri: String?): NavGraph
}

/**
 * Root navigation composable that sets up the navigation graph and handles screen transitions.
 * Manages navigation state and data passing between screens.
 *
 * Key features:
 * - Type-safe navigation using sealed interface
 * - Handles custom navigation types (Alarm)
 * - Manages data flow between screens
 *
 * @param innerPadding Padding values from scaffold
 * @param navController Navigation controller for managing navigation
 */
@Composable
fun NavigationRoot(
    innerPadding: PaddingValues,
    navController: NavHostController,
) {
    // State holder for passing ringtone selection results back to editor
    // This approach allows passing data back without using SavedStateHandle
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
                // Alarms list screen
                composable<NavGraph.Alarms>(
                    typeMap = mapOf(typeOf<Alarm>() to NavType.mapper<Alarm>())
                ) {
                    AlarmsScreenRoot(onOpenAlarmEditor = { alarm ->
                        navController.navigate(NavGraph.AlarmEditor(alarm))
                    })
                }

                // Alarm editor screen
                composable<NavGraph.AlarmEditor>(
                    typeMap = mapOf(typeOf<Alarm>() to NavType.mapper<Alarm>())
                ) {
                    val alarm = it.toRoute<NavGraph.AlarmEditor>().alarm
                    val viewModel: AlarmEditViewModel = koinViewModel(parameters = { parametersOf(alarm) })

                    // Handle ringtone selection result
                    // When returning from ringtone selection, update the editor's state
                    ringtoneSelectionResult?.let { (title, uri) ->
                        LaunchedEffect(title, uri) {
                            // Clear the result after consuming to prevent re-processing
                            ringtoneSelectionResult = null
                            // Update editor with selected ringtone
                            viewModel.onAction(AlarmEditorScreenAction.UpdateRingtoneResult(title, uri))
                        }
                    }

                    AlarmEditScreenRoot(
                        viewModel = viewModel,
                        onOpenRingtoneSetting = {
                            navController.navigate(NavGraph.RingtoneSetting(alarm.ringtoneUri))
                        },
                        onClose = {
                            navController.popBackStack()
                        }
                    )
                }

                // Ringtone selection screen
                composable<NavGraph.RingtoneSetting> {
                    val args = it.toRoute<NavGraph.RingtoneSetting>()
                    val alarmToneUri = args.alarmToneUri

                    AlarmToneSettingScreenRoot(
                        alarmToneUri = alarmToneUri,
                        onGoBack = { title, selectedUri ->
                            // Store selection result in navigation root state
                            // This will be picked up by the editor screen when we return
                            ringtoneSelectionResult = Pair(title, selectedUri)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}