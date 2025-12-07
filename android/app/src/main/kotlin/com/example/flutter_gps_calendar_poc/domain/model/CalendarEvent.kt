package com.example.flutter_gps_calendar_poc.domain.model

/**
 * Domain model representing a calendar event.
 * This replaces the Google Calendar API and Microsoft Graph API calls
 * from the Flutter POC with local ContentProvider access.
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String = "",
    val startTime: Long, // Unix timestamp in milliseconds
    val endTime: Long,   // Unix timestamp in milliseconds
    val location: String = "",
    val calendarColor: Int = 0,
    val calendarDisplayName: String = "",
    val allDay: Boolean = false
) {
    /**
     * Returns true if this event is currently happening.
     */
    fun isHappeningNow(): Boolean {
        val now = System.currentTimeMillis()
        return now in startTime..endTime
    }

    /**
     * Returns true if this event will start in the future.
     */
    fun isUpcoming(): Boolean {
        return startTime > System.currentTimeMillis()
    }

    /**
     * Returns true if this event has already ended.
     */
    fun isPast(): Boolean {
        return endTime < System.currentTimeMillis()
    }
}
