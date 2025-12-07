package com.example.flutter_gps_calendar_poc.di

import android.content.Context
import com.example.flutter_gps_calendar_poc.data.location.GeofenceManager
import com.example.flutter_gps_calendar_poc.data.location.LocationService
import com.example.flutter_gps_calendar_poc.data.notification.NotificationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing location, geofencing and notification services.
 *
 * These services replace the Flutter POC implementations:
 * - LocationService provides current location and updates
 * - NotificationService replaces notification_service.dart
 * - GeofenceManager replaces the GPS stream from location_service.dart
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    /**
     * Provides the NotificationService singleton.
     *
     * NotificationService manages all app notifications with proper channels:
     * - GEOFENCE_CHANNEL: Location-based task reminders
     * - CALENDAR_CHANNEL: Calendar event notifications
     * - GENERAL_CHANNEL: General app notifications
     */
    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext context: Context
    ): NotificationService {
        return NotificationService(context)
    }

    /**
     * Provides the GeofenceManager singleton.
     *
     * GeofenceManager handles all geofencing operations:
     * - Adding geofences for tasks with locations
     * - Removing geofences when tasks are deleted/completed
     * - Prioritizing closest geofences (max 100 limit)
     */
    @Provides
    @Singleton
    fun provideGeofenceManager(
        @ApplicationContext context: Context
    ): GeofenceManager {
        return GeofenceManager(context)
    }

    /**
     * Provides the LocationService singleton.
     *
     * LocationService uses Fused Location Provider for:
     * - Getting current device location
     * - Receiving location updates
     * - Checking location permissions
     */
    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context
    ): LocationService {
        return LocationService(context)
    }
}
