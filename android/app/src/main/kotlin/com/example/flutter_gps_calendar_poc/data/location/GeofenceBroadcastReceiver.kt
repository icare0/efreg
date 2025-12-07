package com.example.flutter_gps_calendar_poc.data.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.flutter_gps_calendar_poc.data.worker.GeofenceCheckWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * BroadcastReceiver for geofence transition events.
 *
 * When a user enters a geofence (task location), this receiver is triggered
 * and starts a Worker to check if the user is free (based on calendar)
 * before sending a notification.
 *
 * This replaces the GPS stream monitoring from location_service.dart in the Flutter POC
 * with event-driven geofencing.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        // Get the transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        // We only care about ENTER transitions
        if (geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "Ignoring transition type: $geofenceTransition")
            return
        }

        // Get the geofences that were triggered
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        if (triggeringGeofences.isEmpty()) {
            Log.w(TAG, "No triggering geofences")
            return
        }

        // Extract task IDs from geofence request IDs
        val taskIds = triggeringGeofences.mapNotNull { geofence ->
            extractTaskIdFromGeofenceId(geofence.requestId)
        }

        if (taskIds.isEmpty()) {
            Log.w(TAG, "No valid task IDs extracted from geofences")
            return
        }

        Log.d(TAG, "Geofence ENTER triggered for ${taskIds.size} tasks: $taskIds")

        // Start a Worker to check calendar and potentially send notification
        // We use a Worker instead of direct notification to:
        // 1. Check calendar availability (async operation)
        // 2. Handle potential long-running operations
        // 3. Survive process death
        taskIds.forEach { taskId ->
            startGeofenceCheckWorker(context, taskId)
        }
    }

    /**
     * Starts a Worker to check if user is free and send notification.
     *
     * @param context Application context.
     * @param taskId ID of the task that triggered the geofence.
     */
    private fun startGeofenceCheckWorker(context: Context, taskId: Long) {
        val inputData = Data.Builder()
            .putLong(GeofenceCheckWorker.KEY_TASK_ID, taskId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<GeofenceCheckWorker>()
            .setInputData(inputData)
            .addTag("geofence_check_$taskId")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        Log.d(TAG, "Enqueued GeofenceCheckWorker for task $taskId")
    }

    /**
     * Extracts the task ID from a geofence request ID.
     *
     * Geofence IDs are formatted as: "task_geofence_{taskId}"
     *
     * @param geofenceId The geofence request ID.
     * @return Task ID, or null if parsing fails.
     */
    private fun extractTaskIdFromGeofenceId(geofenceId: String): Long? {
        return try {
            // Expected format: "task_geofence_123"
            val parts = geofenceId.split("_")
            if (parts.size == 3 && parts[0] == "task" && parts[1] == "geofence") {
                parts[2].toLongOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing geofence ID: $geofenceId", e)
            null
        }
    }
}
