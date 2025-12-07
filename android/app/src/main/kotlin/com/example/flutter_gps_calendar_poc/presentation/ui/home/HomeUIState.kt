package com.example.flutter_gps_calendar_poc.presentation.ui.home

import com.example.flutter_gps_calendar_poc.domain.model.DailySchedule

/**
 * UI state for the Home screen.
 * Follows the standard Loading -> Success/Error pattern.
 */
sealed class HomeUIState {
    /**
     * Initial loading state.
     */
    data object Loading : HomeUIState()

    /**
     * Success state with loaded data.
     *
     * @property schedule The daily schedule containing tasks, events, and free slots.
     * @property hasCalendarPermission Whether the app has calendar read permission.
     * @property hasLocationPermission Whether the app has location permission.
     */
    data class Success(
        val schedule: DailySchedule,
        val hasCalendarPermission: Boolean = false,
        val hasLocationPermission: Boolean = false
    ) : HomeUIState()

    /**
     * Error state.
     *
     * @property message Error message to display to the user.
     * @property throwable Optional throwable for debugging.
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : HomeUIState()
}
