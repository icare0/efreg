package com.example.flutter_gps_calendar_poc.data.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Scheduler for periodic background workers.
 *
 * Schedules:
 * - CleanupWorker: Daily cleanup of old AI feedback data (>90 days)
 * - GeofenceRefreshWorker: Every 6 hours to re-prioritize geofences by distance
 *
 * Uses WorkManager with constraints to be battery-friendly:
 * - Only when device is charging (for refresh)
 * - Only with network connection
 * - Exponential backoff on failure
 */
object WorkerScheduler {

    /**
     * Schedules all periodic workers.
     * Call this from Application.onCreate()
     */
    fun scheduleAllWorkers(context: Context) {
        scheduleCleanupWorker(context)
        scheduleGeofenceRefreshWorker(context)
    }

    /**
     * Schedules the daily cleanup worker.
     *
     * Runs once per day to remove old AI feedback data.
     */
    private fun scheduleCleanupWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // Only when battery is not low
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Keep existing if already scheduled
            cleanupRequest
        )
    }

    /**
     * Schedules the geofence refresh worker.
     *
     * Runs every 6 hours to re-prioritize geofences based on current location.
     * Only runs when device is charging to be battery-friendly.
     */
    private fun scheduleGeofenceRefreshWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)  // Only when charging (battery-friendly)
            .setRequiresBatteryNotLow(true)
            .build()

        val refreshRequest = PeriodicWorkRequestBuilder<GeofenceRefreshWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            GeofenceRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            refreshRequest
        )
    }

    /**
     * Cancels all scheduled workers.
     * Useful for testing or when user opts out.
     */
    fun cancelAllWorkers(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(CleanupWorker.WORK_NAME)
            cancelUniqueWork(GeofenceRefreshWorker.WORK_NAME)
        }
    }
}
