package com.example.flutter_gps_calendar_poc.di

import com.example.flutter_gps_calendar_poc.data.local.datasource.SystemCalendarDataSource
import com.example.flutter_gps_calendar_poc.data.repository.CalendarRepositoryImpl
import com.example.flutter_gps_calendar_poc.domain.repository.CalendarRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing calendar-related dependencies.
 *
 * This module provides the Calendar Repository that replaces the Google Calendar API
 * and Microsoft Graph API from the Flutter POC with native Android Calendar Provider access.
 */
@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {

    /**
     * Provides the CalendarRepository implementation.
     *
     * The SystemCalendarDataSource is automatically injected by Hilt.
     *
     * @param calendarDataSource The system calendar data source.
     * @return CalendarRepository implementation.
     */
    @Provides
    @Singleton
    fun provideCalendarRepository(
        calendarDataSource: SystemCalendarDataSource
    ): CalendarRepository {
        return CalendarRepositoryImpl(calendarDataSource)
    }
}
