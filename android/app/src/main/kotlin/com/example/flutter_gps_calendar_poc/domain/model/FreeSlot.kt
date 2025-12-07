package com.example.flutter_gps_calendar_poc.domain.model

/**
 * Represents a free time slot in the user's schedule.
 * A free slot is a period where no calendar events are scheduled.
 */
data class FreeSlot(
    val startTime: Long,  // Unix timestamp in milliseconds
    val endTime: Long,    // Unix timestamp in milliseconds
) {
    /**
     * Returns the duration of this free slot in minutes.
     */
    val durationMinutes: Long
        get() = (endTime - startTime) / (60 * 1000)

    /**
     * Returns the duration of this free slot in hours.
     */
    val durationHours: Float
        get() = durationMinutes / 60f

    /**
     * Returns true if this free slot is at least the specified duration in minutes.
     */
    fun isAtLeast(minutes: Long): Boolean {
        return durationMinutes >= minutes
    }

    /**
     * Returns true if this free slot is currently happening.
     */
    fun isHappeningNow(): Boolean {
        val now = System.currentTimeMillis()
        return now in startTime..endTime
    }
}
