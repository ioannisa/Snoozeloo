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

class MainActivity : ComponentActivity() {
    companion object {
        private val notificationPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            Manifest.permission.VIBRATE
        }
    }

    private val viewModel by viewModel<MainViewModel>()
    private var showOverlayDialog by mutableStateOf(false)
    private var showNotificationDialog by mutableStateOf(false)
    private var shouldShowNotificationDialog by mutableStateOf(false) // New state to track if we should show notification dialog

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Do nothing with result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.state.showSplash
            }
        }

        enableEdgeToEdge()

        setContent {
            SnoozelooTheme {
                if (showOverlayDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Do nothing */ },
                        title = { Text(stringResource(R.string.permission_overlay_request)) },
                        text = { Text(stringResource(R.string.permission_overlay_description)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showOverlayDialog = false
                                if (shouldShowNotificationDialog) {
                                    showNotificationDialog = true
                                }
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

    private fun checkPermissions() {
        val needsOverlay = !Settings.canDrawOverlays(this)
        showOverlayDialog = needsOverlay

        if (Build.VERSION.SDK_INT >= 33) {
            val hasAskedBefore = getSharedPreferences("permissions", MODE_PRIVATE)
                .getBoolean("notifications_asked", false)

            if (!hasAskedBefore) {
                if (needsOverlay) {
                    // Store that we need to show notification dialog after overlay dialog
                    shouldShowNotificationDialog = true
                } else {
                    // Show notification dialog immediately if no overlay dialog needed
                    showNotificationDialog = true
                }

                getSharedPreferences("permissions", MODE_PRIVATE).edit()
                    .putBoolean("notifications_asked", true)
                    .apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
}

