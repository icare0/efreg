package com.example.flutter_gps_calendar_poc.data.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for geofencing operations.
 *
 * This replaces the GPS stream from location_service.dart in the Flutter POC
 * with battery-efficient geofencing API.
 *
 * Features:
 * - Manages up to 100 geofences (Android limit)
 * - Prioritizes closest geofences to current location
 * - Automatically adds geofences when tasks with locations are created
 * - Removes geofences when tasks are deleted or completed
 *
 * @property context Application context.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        private const val TAG = "GeofenceManager"
        private const val MAX_GEOFENCES = 100
        private const val GEOFENCE_RADIUS_METERS = 150f // 150m radius
        private const val GEOFENCE_EXPIRATION_MS = Geofence.NEVER_EXPIRE
    }

    /**
     * Pending intent for geofence transitions.
     * Points to GeofenceBroadcastReceiver.
     */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Adds geofences for tasks with location data.
     *
     * If the number of tasks exceeds MAX_GEOFENCES, only the closest ones are added.
     *
     * @param tasks List of tasks to create geofences for.
     * @param currentLocation Current user location (for prioritization), or null.
     */
    suspend fun addGeofencesForTasks(
        tasks: List<Task>,
        currentLocation: Location? = null
    ): Result<Int> {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return Result.failure(SecurityException("Location permission required"))
        }

        // Filter tasks that have location data and are not completed
        val tasksWithLocation = tasks.filter {
            !it.isCompleted && it.latitude != null && it.longitude != null
        }

        if (tasksWithLocation.isEmpty()) {
            Log.d(TAG, "No tasks with location to geofence")
            return Result.success(0)
        }

        // Prioritize closest tasks if we have current location and exceed limit
        val prioritizedTasks = if (currentLocation != null && tasksWithLocation.size > MAX_GEOFENCES) {
            prioritizeClosestTasks(tasksWithLocation, currentLocation, MAX_GEOFENCES)
        } else {
            tasksWithLocation.take(MAX_GEOFENCES)
        }

        // Create geofence objects
        val geofences = prioritizedTasks.mapNotNull { task ->
            createGeofenceForTask(task)
        }

        if (geofences.isEmpty()) {
            return Result.success(0)
        }

        // Create geofencing request
        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()

        return try {
            // Add geofences
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.d(TAG, "Successfully added ${geofences.size} geofences")
            Result.success(geofences.size)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException adding geofences", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding geofences", e)
            Result.failure(e)
        }
    }

    /**
     * Removes a geofence for a specific task.
     *
     * @param taskId ID of the task.
     */
    suspend fun removeGeofenceForTask(taskId: Long): Result<Unit> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission required"))
        }

        val geofenceId = getGeofenceId(taskId)

        return try {
            geofencingClient.removeGeofences(listOf(geofenceId)).await()
            Log.d(TAG, "Removed geofence for task $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofence for task $taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Removes all geofences.
     */
    suspend fun removeAllGeofences(): Result<Unit> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission required"))
        }

        return try {
            geofencingClient.removeGeofences(geofencePendingIntent).await()
            Log.d(TAG, "Removed all geofences")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing all geofences", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a Geofence object for a task.
     *
     * @param task The task to create a geofence for.
     * @return Geofence object, or null if task doesn't have location.
     */
    private fun createGeofenceForTask(task: Task): Geofence? {
        val latitude = task.latitude ?: return null
        val longitude = task.longitude ?: return null

        return Geofence.Builder()
            .setRequestId(getGeofenceId(task.id))
            .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_METERS)
            .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
    }

    /**
     * Prioritizes tasks by distance to current location.
     *
     * @param tasks List of tasks to prioritize.
     * @param currentLocation Current user location.
     * @param limit Maximum number of tasks to return.
     * @return List of closest tasks.
     */
    private fun prioritizeClosestTasks(
        tasks: List<Task>,
        currentLocation: Location,
        limit: Int
    ): List<Task> {
        return tasks
            .map { task ->
                val distance = calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    task.latitude!!,
                    task.longitude!!
                )
                task to distance
            }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Calculates distance between two coordinates using Haversine formula.
     *
     * @return Distance in meters.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Generates a unique geofence ID for a task.
     */
    private fun getGeofenceId(taskId: Long): String {
        return "task_geofence_$taskId"
    }

    /**
     * Checks if the app has location permission.
     */
    private fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }
}
