package eu.anifantakis.snoozeloo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import eu.anifantakis.snoozeloo.core.presentation.NavigationRoot
import eu.anifantakis.snoozeloo.ui.theme.SnoozelooTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main entry point of the application.
 * Handles:
 * - Permission management (overlay and notifications)
 * - Splash screen
 * - Theme setup
 * - Navigation initialization
 */
class MainActivity : ComponentActivity() {
    companion object {
        // Choose appropriate permission based on API level
        private val notificationPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            Manifest.permission.VIBRATE
        }
    }

    private val viewModel by viewModel<MainViewModel>()

    // Dialog visibility states
    private var showOverlayDialog by mutableStateOf(false)
    private var showNotificationDialog by mutableStateOf(false)
    // Tracks whether to show notification dialog after overlay permission
    private var shouldShowNotificationDialog by mutableStateOf(false)

    // Permission request callback
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result handling not needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        // Configure splash screen with view model condition
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.state.showSplash
            }
        }

        enableEdgeToEdge()

        setContent {
            SnoozelooTheme {
                // Overlay permission dialog
                if (showOverlayDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Non-dismissible */ },
                        title = { Text(stringResource(R.string.permission_overlay_request)) },
                        text = { Text(stringResource(R.string.permission_overlay_description)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showOverlayDialog = false
                                // Show notification dialog after overlay if needed
                                if (shouldShowNotificationDialog) {
                                    showNotificationDialog = true
                                }
                                // Open system overlay settings
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                ).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(intent)
                            }) {
                                Text(stringResource(R.string.grant_permission))
                            }
                        }
                    )
                }

                // Notification permission dialog
                if (showNotificationDialog) {
                    AlertDialog(
                        onDismissRequest = { showNotificationDialog = false },
                        title = { Text(stringResource(R.string.permission_notifications_request)) },
                        text = { Text(stringResource(R.string.permission_notifications_description)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showNotificationDialog = false
                                if (Build.VERSION.SDK_INT >= 33) {
                                    requestPermissionLauncher.launch(notificationPermission)
                                }
                            }) {
                                Text(stringResource(R.string.enable))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationDialog = false }) {
                                Text(stringResource(R.string.no_thanks))
                            }
                        }
                    )
                }

                // Main app scaffold
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavigationRoot(
                        innerPadding = innerPadding,
                        navController = navController
                    )
                }
            }
        }
    }

    /**
     * Checks and requests necessary permissions.
     *
     * Permission flow:
     * 1. Check overlay permission (required)
     * 2. If Android 13+, check notification permission
     * 3. Show overlay dialog if needed
     * 4. Queue notification dialog after overlay (if needed)
     * 5. Track notification permission request in preferences
     */
    private fun checkPermissions() {
        val needsOverlay = !Settings.canDrawOverlays(this)
        showOverlayDialog = needsOverlay

        // Handle notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            val hasAskedBefore = getSharedPreferences("permissions", MODE_PRIVATE)
                .getBoolean("notifications_asked", false)

            if (!hasAskedBefore) {
                if (needsOverlay) {
                    // Queue notification dialog after overlay
                    shouldShowNotificationDialog = true
                } else {
                    // Show notification dialog immediately
                    showNotificationDialog = true
                }

                // Mark notification permission as requested
                getSharedPreferences("permissions", MODE_PRIVATE).edit()
                    .putBoolean("notifications_asked", true)
                    .apply()
            }
        }
    }

    /**
     * On resume if the overlay permission is not granted, insist and don't allow to continue
     */
    override fun onResume() {
        super.onResume()
        // Recheck permissions when returning to app
        checkPermissions()
    }
}