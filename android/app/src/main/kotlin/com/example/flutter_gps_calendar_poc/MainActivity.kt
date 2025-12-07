package com.example.flutter_gps_calendar_poc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.flutter_gps_calendar_poc.presentation.ui.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Contextual To-Do List application.
 *
 * This replaces the Flutter-based MainActivity with a native
 * Jetpack Compose implementation using Material 3.
 *
 * @HiltAndroidApp annotation enables Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ContextualTodoTheme {
                HomeScreen()
            }
        }
    }
}

/**
 * Material 3 theme for the Contextual To-Do List app.
 */
@Composable
fun ContextualTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
