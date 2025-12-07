package com.example.flutter_gps_calendar_poc.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for accessing device location using Fused Location Provider.
 *
 * Provides:
 * - Current location (one-time)
 * - Location updates (Flow)
 * - Permission checks
 *
 * Replaces the continuous GPS stream from Flutter POC with efficient location access.
 */
@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationService"
        private const val UPDATE_INTERVAL_MS = 60000L // 1 minute
        private const val FASTEST_INTERVAL_MS = 30000L // 30 seconds
    }

    /**
     * Gets the last known location (cached, fast).
     *
     * @return Last known location, or null if unavailable or no permission.
     */
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting last location", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last location", e)
            null
        }
    }

    /**
     * Gets the current location (requests fresh location update).
     *
     * More accurate than getLastKnownLocation but slower.
     *
     * @return Current location, or null if unavailable.
     */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        return try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(5000) // Max 5 seconds old
                .build()

            fusedLocationClient.getCurrentLocation(request, null).await()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting current location", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            null
        }
    }

    /**
     * Provides a Flow of location updates.
     *
     * Emits new locations as they become available.
     * Automatically stops when Flow is cancelled.
     *
     * @return Flow of Location updates.
     */
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted for updates")
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setWaitForAccurateLocation(false)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                context.mainLooper
            ).await()

            Log.d(TAG, "Started location updates")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates", e)
            close(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates", e)
            close(e)
        }

        awaitClose {
            Log.d(TAG, "Stopping location updates")
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Checks if the app has location permission.
     */
    fun hasLocationPermission(): Boolean {
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

    /**
     * Checks if background location permission is granted (Android 10+).
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
