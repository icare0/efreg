package com.example.flutter_gps_calendar_poc.data.repository

import com.example.flutter_gps_calendar_poc.data.local.datasource.SystemCalendarDataSource
import com.example.flutter_gps_calendar_poc.domain.model.CalendarEvent
import com.example.flutter_gps_calendar_poc.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementation of CalendarRepository using the Android Calendar Provider.
 *
 * This replaces the Google Calendar API and Microsoft Graph API from the Flutter POC
 * with native CalendarContract access, providing:
 * - No OAuth2 authentication required
 * - Access to all synchronized calendars (Google, Outlook, Exchange, etc.)
 * - Better battery life (no network calls)
 * - Better privacy (data stays on device)
 *
 * @property calendarDataSource Data source for reading system calendar events.
 */
class CalendarRepositoryImpl @Inject constructor(
    private val calendarDataSource: SystemCalendarDataSource
) : CalendarRepository {

    override fun getEventsInRange(startMillis: Long, endMillis: Long): Flow<List<CalendarEvent>> = flow {
        val events = calendarDataSource.getEventsInRange(startMillis, endMillis)
        emit(events)
    }

    override fun getTodayEvents(): Flow<List<CalendarEvent>> = flow {
        val events = calendarDataSource.getTodayEvents()
        emit(events)
    }

    override fun getCurrentEvents(): Flow<List<CalendarEvent>> = flow {
        val events = calendarDataSource.getCurrentEvents()
        emit(events)
    }

    override fun getUpcomingEvents(hoursAhead: Int): Flow<List<CalendarEvent>> = flow {
        val events = calendarDataSource.getUpcomingEvents(hoursAhead)
        emit(events)
    }

    override suspend fun hasCalendarPermission(): Boolean {
        return calendarDataSource.hasCalendarPermission()
    }
}
