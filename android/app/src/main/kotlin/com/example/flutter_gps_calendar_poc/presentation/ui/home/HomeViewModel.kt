package com.example.flutter_gps_calendar_poc.presentation.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flutter_gps_calendar_poc.data.location.GeofenceManager
import com.example.flutter_gps_calendar_poc.data.location.LocationService
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.repository.CalendarRepository
import com.example.flutter_gps_calendar_poc.domain.repository.TaskRepository
import com.example.flutter_gps_calendar_poc.domain.usecase.GetDailyScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 *
 * Enhanced with geofencing and location services.
 *
 * Responsibilities:
 * - Combine tasks and calendar events via GetDailyScheduleUseCase
 * - Manage geofences automatically when tasks change
 * - Provide current location for UI
 * - Check permissions
 * - Expose UI state as StateFlow
 *
 * @property getDailyScheduleUseCase Use case for fetching daily schedule.
 * @property taskRepository Repository for task operations.
 * @property calendarRepository Repository for calendar operations.
 * @property locationService Service for accessing device location.
 * @property geofenceManager Manager for creating/removing geofences.
 * @property context Application context for permission checks.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyScheduleUseCase: GetDailyScheduleUseCase,
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val locationService: LocationService,
    private val geofenceManager: GeofenceManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow<HomeUIState>(HomeUIState.Loading)
    val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    init {
        loadDailySchedule()
        startLocationUpdates()
        observeTasksForGeofencing()
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
     * Starts location updates to keep current location fresh.
     */
    private fun startLocationUpdates() {
        viewModelScope.launch {
            // Get initial location
            val initialLocation = locationService.getCurrentLocation()
            _currentLocation.value = initialLocation

            // Subscribe to updates
            locationService.getLocationUpdates()
                .catch { e ->
                    Log.e(TAG, "Error in location updates", e)
                }
                .collect { location ->
                    _currentLocation.value = location
                    Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                }
        }
    }

    /**
     * Observes task changes and automatically updates geofences.
     */
    private fun observeTasksForGeofencing() {
        viewModelScope.launch {
            taskRepository.getAllTasks()
                .catch { e ->
                    Log.e(TAG, "Error observing tasks for geofencing", e)
                }
                .collect { tasks ->
                    refreshGeofences(tasks)
                }
        }
    }

    /**
     * Refreshes geofences based on current tasks and location.
     */
    private suspend fun refreshGeofences(tasks: List<Task>) {
        try {
            // Remove all existing geofences first
            geofenceManager.removeAllGeofences()

            // Add new geofences with current location for prioritization
            val currentLoc = _currentLocation.value
            val result = geofenceManager.addGeofencesForTasks(tasks, currentLoc)

            result.onSuccess { count ->
                Log.d(TAG, "Refreshed $count geofences")
            }.onFailure { e ->
                Log.e(TAG, "Failed to refresh geofences", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing geofences", e)
        }
    }

    /**
     * Adds a new task and creates geofence if it has location.
     */
    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                val taskId = taskRepository.insertTask(task)

                // If task has location, add geofence
                if (task.latitude != null && task.longitude != null) {
                    val taskWithId = task.copy(id = taskId)
                    geofenceManager.addGeofencesForTasks(
                        tasks = listOf(taskWithId),
                        currentLocation = _currentLocation.value
                    )
                    Log.d(TAG, "Added geofence for new task $taskId")
                }
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

                // Geofences will be refreshed automatically via observeTasksForGeofencing
                Log.d(TAG, "Updated task ${task.id}")
            } catch (e: Exception) {
                _uiState.value = HomeUIState.Error(
                    message = "Failed to update task: ${e.message}",
                    throwable = e
                )
            }
        }
    }

    /**
     * Deletes a task and removes its geofence.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                // Remove geofence first
                geofenceManager.removeGeofenceForTask(task.id)

                // Delete task
                taskRepository.deleteTask(task)

                Log.d(TAG, "Deleted task ${task.id} and its geofence")
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

                // Remove geofence if task is completed
                if (updatedTask.isCompleted) {
                    geofenceManager.removeGeofenceForTask(task.id)
                    Log.d(TAG, "Removed geofence for completed task ${task.id}")
                }
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
     * Refreshes location manually.
     */
    fun refreshLocation() {
        viewModelScope.launch {
            val location = locationService.getCurrentLocation()
            _currentLocation.value = location
            Log.d(TAG, "Location refreshed manually")
        }
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
        refreshLocation()
    }
}
