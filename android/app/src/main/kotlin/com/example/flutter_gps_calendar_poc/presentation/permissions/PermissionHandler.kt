package com.example.flutter_gps_calendar_poc.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Composable for handling runtime permissions with Material 3 UI.
 *
 * Manages:
 * - Location permissions (FINE, COARSE, BACKGROUND)
 * - Calendar permissions (READ, WRITE)
 * - Notification permission (POST_NOTIFICATIONS on Android 13+)
 *
 * Shows rationale dialogs when needed.
 */
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    var showLocationRationale by remember { mutableStateOf(false) }
    var showCalendarRationale by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }
    var permissionsChecked by remember { mutableStateOf(false) }

    // Location permissions
    val locationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showLocationRationale = false
            checkNextPermission()
        }
    }

    // Calendar permissions
    val calendarPermissions = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showCalendarRationale = false
            checkNextPermission()
        }
    }

    // Notification permission (Android 13+)
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showNotificationRationale = false
        }
        onPermissionsGranted()
        permissionsChecked = true
    }

    fun checkNextPermission() {
        if (!permissionsChecked) {
            // First check notification permission (if needed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                showNotificationRationale = true
            } else {
                onPermissionsGranted()
                permissionsChecked = true
            }
        }
    }

    LaunchedEffect(Unit) {
        // Start permission flow on first launch
        showLocationRationale = true
    }

    // Show content
    content()

    // Location Permission Dialog
    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { /* Required for critical permission */ },
            title = { Text("Location Permission Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This app needs location access to:")
                    Text("• Remind you about tasks when you arrive at locations")
                    Text("• Show nearby tasks on the map")
                    Text("• Provide contextual notifications")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Background location is needed to send notifications even when the app is closed.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        locationLauncher.launch(locationPermissions)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocationRationale = false
                        checkNextPermission()
                    }
                ) {
                    Text("Skip")
                }
            }
        )
    }

    // Calendar Permission Dialog
    if (showCalendarRationale) {
        AlertDialog(
            onDismissRequest = { showCalendarRationale = false },
            title = { Text("Calendar Access") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Allow calendar access to:")
                    Text("• Check your availability before sending notifications")
                    Text("• Suggest optimal times for tasks")
                    Text("• Show your schedule in the app")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your calendar data stays on your device and is never uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        calendarLauncher.launch(calendarPermissions)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCalendarRationale = false
                        checkNextPermission()
                    }
                ) {
                    Text("Skip")
                }
            }
        )
    }

    // Notification Permission Dialog (Android 13+)
    if (showNotificationRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AlertDialog(
            onDismissRequest = { /* Required */ },
            title = { Text("Enable Notifications") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notifications are essential for:")
                    Text("• Location-based task reminders")
                    Text("• Calendar event alerts")
                    Text("• Smart suggestions when you're free")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationRationale = false
                        onPermissionsGranted()
                        permissionsChecked = true
                    }
                ) {
                    Text("Skip")
                }
            }
        )
    }
}
