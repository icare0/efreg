package com.example.flutter_gps_calendar_poc.data.local.datasource

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.flutter_gps_calendar_poc.domain.model.CalendarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Data source for reading calendar events from the Android Calendar Provider.
 * This replaces the Google Calendar API and Microsoft Graph API from the Flutter POC.
 *
 * Uses CalendarContract.Instances to query events across all synchronized calendars
 * (Google, Outlook, Exchange, etc.) without requiring OAuth2 authentication.
 *
 * @property context Application context for ContentResolver access.
 */
class SystemCalendarDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        private const val TAG = "SystemCalendarDataSource"

        // Projection for querying calendar instances
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.ALL_DAY
        )

        // Projection indices
        private const val PROJECTION_ID_INDEX = 0
        private const val PROJECTION_EVENT_ID_INDEX = 1
        private const val PROJECTION_TITLE_INDEX = 2
        private const val PROJECTION_DESCRIPTION_INDEX = 3
        private const val PROJECTION_BEGIN_INDEX = 4
        private const val PROJECTION_END_INDEX = 5
        private const val PROJECTION_LOCATION_INDEX = 6
        private const val PROJECTION_CALENDAR_COLOR_INDEX = 7
        private const val PROJECTION_CALENDAR_DISPLAY_NAME_INDEX = 8
        private const val PROJECTION_ALL_DAY_INDEX = 9
    }

    /**
     * Checks if the app has READ_CALENDAR permission.
     */
    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Retrieves calendar events in a specific time range.
     *
     * Optimization: Queries CalendarContract.Instances with a strict time range
     * to minimize data transfer and battery usage.
     *
     * @param startMillis Start of the time range (Unix timestamp in milliseconds).
     * @param endMillis End of the time range (Unix timestamp in milliseconds).
     * @return List of CalendarEvent objects, or empty list if permission denied.
     */
    suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (!hasCalendarPermission()) {
                Log.w(TAG, "READ_CALENDAR permission not granted")
                return@withContext emptyList()
            }

            val events = mutableListOf<CalendarEvent>()

            try {
                // Build URI with time range for optimized query
                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                android.content.ContentUris.appendId(builder, startMillis)
                android.content.ContentUris.appendId(builder, endMillis)

                // Execute query
                val cursor: Cursor? = contentResolver.query(
                    builder.build(),
                    EVENT_PROJECTION,
                    null, // selection (null = all calendars)
                    null, // selectionArgs
                    "${CalendarContract.Instances.BEGIN} ASC" // Sort by start time
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val event = parseEvent(it)
                        if (event != null) {
                            events.add(event)
                        }
                    }
                }

                Log.d(TAG, "Retrieved ${events.size} events from $startMillis to $endMillis")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Calendar permission denied", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error querying calendar events", e)
            }

            return@withContext events
        }

    /**
     * Retrieves today's events: from 1 hour ago to 24 hours from now.
     * This is the optimized query for the contextual to-do list feature.
     */
    suspend fun getTodayEvents(): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        val startTime = now - (1 * 60 * 60 * 1000) // 1 hour ago
        val endTime = now + (24 * 60 * 60 * 1000) // 24 hours from now

        return getEventsInRange(startTime, endTime)
    }

    /**
     * Retrieves events currently happening.
     */
    suspend fun getCurrentEvents(): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        val events = getTodayEvents()

        return events.filter { it.isHappeningNow() }
    }

    /**
     * Retrieves upcoming events (starting within the next N hours).
     *
     * @param hoursAhead Number of hours to look ahead.
     */
    suspend fun getUpcomingEvents(hoursAhead: Int = 3): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        val endTime = now + (hoursAhead * 60 * 60 * 1000L)

        val events = getEventsInRange(now, endTime)

        return events.filter { it.isUpcoming() }
    }

    /**
     * Parses a calendar event from a Cursor.
     *
     * @param cursor The Cursor positioned at the event row.
     * @return CalendarEvent object, or null if parsing fails.
     */
    private fun parseEvent(cursor: Cursor): CalendarEvent? {
        return try {
            CalendarEvent(
                id = cursor.getLong(PROJECTION_EVENT_ID_INDEX),
                title = cursor.getStringOrEmpty(PROJECTION_TITLE_INDEX),
                description = cursor.getStringOrEmpty(PROJECTION_DESCRIPTION_INDEX),
                startTime = cursor.getLong(PROJECTION_BEGIN_INDEX),
                endTime = cursor.getLong(PROJECTION_END_INDEX),
                location = cursor.getStringOrEmpty(PROJECTION_LOCATION_INDEX),
                calendarColor = cursor.getInt(PROJECTION_CALENDAR_COLOR_INDEX),
                calendarDisplayName = cursor.getStringOrEmpty(PROJECTION_CALENDAR_DISPLAY_NAME_INDEX),
                allDay = cursor.getInt(PROJECTION_ALL_DAY_INDEX) == 1
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event", e)
            null
        }
    }

    /**
     * Extension function to safely get a String from a Cursor, returning empty string if null.
     */
    private fun Cursor.getStringOrEmpty(columnIndex: Int): String {
        return if (isNull(columnIndex)) "" else getString(columnIndex) ?: ""
    }
}
