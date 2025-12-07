package com.example.flutter_gps_calendar_poc.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flutter_gps_calendar_poc.data.location.GeofenceManager
import com.example.flutter_gps_calendar_poc.data.location.LocationService
import com.example.flutter_gps_calendar_poc.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodic worker that refreshes geofences based on current location.
 *
 * Runs every 6 hours to re-prioritize geofences when user moves significantly.
 * This ensures the 100 closest tasks always have active geofences.
 *
 * Why periodic refresh?
 * - Android limits to 100 geofences per app
 * - If user moves far from home, we want to prioritize nearby tasks
 * - Balances battery efficiency with location relevance
 */
@HiltWorker
class GeofenceRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val locationService: LocationService,
    private val geofenceManager: GeofenceManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "GeofenceRefreshWorker"
        const val WORK_NAME = "geofence_refresh_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic geofence refresh")

            // Get current location
            val currentLocation = locationService.getCurrentLocation()

            if (currentLocation == null) {
                Log.w(TAG, "Could not get current location, skipping refresh")
                return Result.retry()
            }

            Log.d(TAG, "Current location: ${currentLocation.latitude}, ${currentLocation.longitude}")

            // Get all active tasks with locations
            val tasks = taskRepository.getAllTasks().first()
            val activeTasks = tasks.filter { !it.isCompleted && it.latitude != null && it.longitude != null }

            Log.d(TAG, "Found ${activeTasks.size} active tasks with locations")

            // Remove all existing geofences
            geofenceManager.removeAllGeofences()

            // Add new geofences prioritized by distance
            val result = geofenceManager.addGeofencesForTasks(activeTasks, currentLocation)

            result.onSuccess { count ->
                Log.d(TAG, "Successfully refreshed $count geofences")
            }.onFailure { e ->
                Log.e(TAG, "Failed to refresh geofences", e)
                return Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during geofence refresh", e)
            Result.failure()
        }
    }
}
