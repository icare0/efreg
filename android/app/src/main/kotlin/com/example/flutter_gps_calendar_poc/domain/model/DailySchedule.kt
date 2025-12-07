package com.example.flutter_gps_calendar_poc.domain.model

/**
 * Aggregated daily schedule combining tasks, calendar events, and free time slots.
 * This is the unified data model for the Home screen.
 */
data class DailySchedule(
    val tasks: List<Task> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val freeSlots: List<FreeSlot> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Returns active (incomplete) tasks only.
     */
    val activeTasks: List<Task>
        get() = tasks.filter { !it.isCompleted }

    /**
     * Returns completed tasks only.
     */
    val completedTasks: List<Task>
        get() = tasks.filter { it.isCompleted }

    /**
     * Returns calendar events currently happening.
     */
    val currentEvents: List<CalendarEvent>
        get() = calendarEvents.filter { it.isHappeningNow() }

    /**
     * Returns upcoming calendar events (not yet started).
     */
    val upcomingEvents: List<CalendarEvent>
        get() = calendarEvents.filter { it.isUpcoming() }

    /**
     * Returns free slots of at least the specified duration in minutes.
     */
    fun getFreeSlotsAtLeast(minutes: Long): List<FreeSlot> {
        return freeSlots.filter { it.isAtLeast(minutes) }
    }

    /**
     * Returns the current free slot, if any.
     */
    val currentFreeSlot: FreeSlot?
        get() = freeSlots.firstOrNull { it.isHappeningNow() }

    /**
     * Returns true if the schedule has any data.
     */
    val hasData: Boolean
        get() = tasks.isNotEmpty() || calendarEvents.isNotEmpty() || freeSlots.isNotEmpty()
}
