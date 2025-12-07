package com.example.flutter_gps_calendar_poc.presentation.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.repository.CalendarRepository
import com.example.flutter_gps_calendar_poc.domain.repository.TaskRepository
import com.example.flutter_gps_calendar_poc.domain.usecase.GetDailyScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 *
 * This replaces the logic from home_screen.dart in the Flutter POC,
 * properly structured according to MVVM architecture.
 *
 * Responsibilities:
 * - Combine tasks and calendar events via GetDailyScheduleUseCase
 * - Calculate free time slots
 * - Check permissions
 * - Expose UI state as StateFlow
 *
 * @property getDailyScheduleUseCase Use case for fetching daily schedule.
 * @property taskRepository Repository for task operations.
 * @property calendarRepository Repository for calendar operations.
 * @property context Application context for permission checks.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyScheduleUseCase: GetDailyScheduleUseCase,
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUIState>(HomeUIState.Loading)
    val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

    init {
        loadDailySchedule()
    }

    /**
     * Loads the daily schedule by combining tasks and calendar events.
     * Automatically updates when underlying data changes (reactive Flow).
     */
    private fun loadDailySchedule() {
        viewModelScope.launch {
            getDailyScheduleUseCase()
                .map { schedule ->
                    HomeUIState.Success(
                        schedule = schedule,
                        hasCalendarPermission = hasCalendarPermission(),
                        hasLocationPermission = hasLocationPermission()
                    )
                }
                .catch { throwable ->
                    _uiState.value = HomeUIState.Error(
                        message = "Failed to load schedule: ${throwable.message}",
                        throwable = throwable
                    )
                }
                .collect { successState ->
                    _uiState.value = successState
                }
        }
    }

    /**
     * Adds a new task.
     */
    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.insertTask(task)
            } catch (e: Exception) {
                _uiState.value = HomeUIState.Error(
                    message = "Failed to add task: ${e.message}",
                    throwable = e
                )
            }
        }
    }

    /**
     * Updates an existing task.
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(task)
            } catch (e: Exception) {
                _uiState.value = HomeUIState.Error(
                    message = "Failed to update task: ${e.message}",
                    throwable = e
                )
            }
        }
    }

    /**
     * Deletes a task.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task)
            } catch (e: Exception) {
                _uiState.value = HomeUIState.Error(
                    message = "Failed to delete task: ${e.message}",
                    throwable = e
                )
            }
        }
    }

    /**
     * Toggles the completion status of a task.
     */
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(isCompleted = !task.isCompleted)
                taskRepository.updateTask(updatedTask)
            } catch (e: Exception) {
                _uiState.value = HomeUIState.Error(
                    message = "Failed to toggle task: ${e.message}",
                    throwable = e
                )
            }
        }
    }

    /**
     * Refreshes the daily schedule.
     */
    fun refresh() {
        loadDailySchedule()
    }

    /**
     * Checks if the app has calendar read permission.
     */
    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if the app has fine location permission.
     */
    private fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Called when permissions are granted.
     * Refreshes the data to reflect the new permission state.
     */
    fun onPermissionsGranted() {
        refresh()
    }
}
