package com.example.flutter_gps_calendar_poc.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flutter_gps_calendar_poc.data.gamification.GamificationManager
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.model.UserStats
import com.example.flutter_gps_calendar_poc.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simplified ViewModel for the modern home screen.
 *
 * Manages tasks and gamification in a clean, simple way.
 */
@HiltViewModel
class SimplifiedHomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val gamificationManager: GamificationManager
) : ViewModel() {

    /**
     * All tasks as StateFlow.
     */
    val tasks: StateFlow<List<Task>> = taskRepository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * User stats for gamification.
     */
    val stats: StateFlow<UserStats?> = gamificationManager.getUserStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Add a new task.
     */
    fun addTask(task: Task) {
        viewModelScope.launch {
            taskRepository.insertTask(task)
        }
    }

    /**
     * Toggle task completion status.
     * Awards points if task is being completed.
     */
    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskRepository.updateTask(updated)

            // Award points if completing the task
            if (updated.isCompleted) {
                gamificationManager.onTaskCompleted()
            }
        }
    }

    /**
     * Delete a task.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }
}
