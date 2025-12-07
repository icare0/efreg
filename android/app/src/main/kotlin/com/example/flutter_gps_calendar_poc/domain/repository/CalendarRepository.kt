package com.example.flutter_gps_calendar_poc.domain.repository

import com.example.flutter_gps_calendar_poc.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for accessing calendar events.
 * This replaces the Google Calendar API and Microsoft Graph API
 * from the Flutter POC with native Android CalendarContract access.
 */
interface CalendarRepository {
    /**
     * Retrieves events in a specific time range.
     *
     * @param startMillis Start of the time range (Unix timestamp in milliseconds).
     * @param endMillis End of the time range (Unix timestamp in milliseconds).
     * @return Flow of calendar events in the specified range.
     */
    fun getEventsInRange(startMillis: Long, endMillis: Long): Flow<List<CalendarEvent>>

    /**
     * Retrieves today's events (from 1 hour ago to 24 hours from now).
     * This is the optimized query for the contextual to-do list feature.
     */
    fun getTodayEvents(): Flow<List<CalendarEvent>>

    /**
     * Retrieves events currently happening.
     */
    fun getCurrentEvents(): Flow<List<CalendarEvent>>

    /**
     * Retrieves upcoming events (starting within the next N hours).
     *
     * @param hoursAhead Number of hours to look ahead (default: 3).
     */
    fun getUpcomingEvents(hoursAhead: Int = 3): Flow<List<CalendarEvent>>

    /**
     * Checks if the app has permission to read calendar.
     */
    suspend fun hasCalendarPermission(): Boolean
}
