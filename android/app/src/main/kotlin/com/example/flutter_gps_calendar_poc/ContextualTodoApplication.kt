package com.example.flutter_gps_calendar_poc

import android.app.Application
import com.example.flutter_gps_calendar_poc.data.worker.WorkerScheduler
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Contextual To-Do List app.
 *
 * The @HiltAndroidApp annotation triggers Hilt's code generation, including:
 * - A base class for the application that serves as the application-level dependency container.
 * - Initialization of Hilt's dependency injection framework.
 *
 * On app startup, schedules periodic background workers:
 * - CleanupWorker: Daily cleanup of old AI feedback data
 * - GeofenceRefreshWorker: Every 6 hours to re-prioritize geofences
 */
@HiltAndroidApp
class ContextualTodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Schedule periodic background workers
        WorkerScheduler.scheduleAllWorkers(this)
    }
}
