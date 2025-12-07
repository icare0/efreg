package com.example.flutter_gps_calendar_poc

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Contextual To-Do List app.
 *
 * The @HiltAndroidApp annotation triggers Hilt's code generation, including:
 * - A base class for the application that serves as the application-level dependency container.
 * - Initialization of Hilt's dependency injection framework.
 *
 * This class must be specified in AndroidManifest.xml as the application name.
 */
@HiltAndroidApp
class ContextualTodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization logic can be added here
    }
}
