package com.example.flutter_gps_calendar_poc.domain.usecase

import com.example.flutter_gps_calendar_poc.domain.model.CalendarEvent
import com.example.flutter_gps_calendar_poc.domain.model.DailySchedule
import com.example.flutter_gps_calendar_poc.domain.model.FreeSlot
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.repository.CalendarRepository
import com.example.flutter_gps_calendar_poc.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case that combines tasks and calendar events into a unified daily schedule.
 *
 * This replaces the logic from home_screen.dart in the Flutter POC, but properly
 * separated according to MVVM architecture.
 *
 * Features:
 * - Combines tasks from Room database
 * - Combines calendar events from Calendar Provider
 * - Calculates free time slots (gaps > 30 minutes between events)
 *
 * @property taskRepository Repository for task data.
 * @property calendarRepository Repository for calendar event data.
 */
class GetDailyScheduleUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository
) {
    companion object {
        /**
         * Minimum duration in minutes for a slot to be considered "free".
         * Slots shorter than this are ignored.
         */
        private const val MIN_FREE_SLOT_MINUTES = 30L
    }

    /**
     * Returns a Flow that emits the daily schedule whenever tasks or calendar events change.
     *
     * The flow combines:
     * - All tasks (active and completed)
     * - Today's calendar events ([Now - 1h] to [Now + 24h])
     * - Calculated free slots between events
     */
    operator fun invoke(): Flow<DailySchedule> {
        return combine(
            taskRepository.getAllTasks(),
            calendarRepository.getTodayEvents()
        ) { tasks, events ->
            val freeSlots = calculateFreeSlots(events)

            DailySchedule(
                tasks = tasks,
                calendarEvents = events,
                freeSlots = freeSlots
            )
        }
    }

    /**
     * Returns a Flow that emits only active tasks with their associated schedule.
     */
    fun getActiveSchedule(): Flow<DailySchedule> {
        return combine(
            taskRepository.getActiveTasks(),
            calendarRepository.getTodayEvents()
        ) { tasks, events ->
            val freeSlots = calculateFreeSlots(events)

            DailySchedule(
                tasks = tasks,
                calendarEvents = events,
                freeSlots = freeSlots
            )
        }
    }

    /**
     * Calculates free time slots between calendar events.
     *
     * Algorithm:
     * 1. Sort events by start time
     * 2. Find gaps between consecutive events
     * 3. Filter gaps that are at least MIN_FREE_SLOT_MINUTES
     *
     * @param events List of calendar events (should be sorted by start time).
     * @return List of free time slots.
     */
    private fun calculateFreeSlots(events: List<CalendarEvent>): List<FreeSlot> {
        if (events.isEmpty()) {
            // If no events, return one large free slot for today
            val now = System.currentTimeMillis()
            val endOfDay = now + (24 * 60 * 60 * 1000)
            return listOf(FreeSlot(now, endOfDay))
        }

        val freeSlots = mutableListOf<FreeSlot>()
        val sortedEvents = events.sortedBy { it.startTime }

        // Add free slot from now to first event (if applicable)
        val now = System.currentTimeMillis()
        val firstEvent = sortedEvents.first()
        if (firstEvent.startTime > now) {
            val slot = FreeSlot(now, firstEvent.startTime)
            if (slot.isAtLeast(MIN_FREE_SLOT_MINUTES)) {
                freeSlots.add(slot)
            }
        }

        // Find gaps between consecutive events
        for (i in 0 until sortedEvents.size - 1) {
            val currentEvent = sortedEvents[i]
            val nextEvent = sortedEvents[i + 1]

            // Gap exists if next event starts after current event ends
            if (nextEvent.startTime > currentEvent.endTime) {
                val slot = FreeSlot(currentEvent.endTime, nextEvent.startTime)
                if (slot.isAtLeast(MIN_FREE_SLOT_MINUTES)) {
                    freeSlots.add(slot)
                }
            }
        }

        // Add free slot from last event to end of query range (24h from now)
        val lastEvent = sortedEvents.last()
        val endOfRange = now + (24 * 60 * 60 * 1000)
        if (lastEvent.endTime < endOfRange) {
            val slot = FreeSlot(lastEvent.endTime, endOfRange)
            if (slot.isAtLeast(MIN_FREE_SLOT_MINUTES)) {
                freeSlots.add(slot)
            }
        }

        return freeSlots
    }
}
