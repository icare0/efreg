package com.example.flutter_gps_calendar_poc.domain.repository

import com.example.flutter_gps_calendar_poc.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining the contract for task data operations.
 * This interface belongs to the domain layer and is framework-agnostic.
 */
interface TaskRepository {
    /**
     * Retrieves all tasks as a Flow.
     */
    fun getAllTasks(): Flow<List<Task>>

    /**
     * Retrieves a single task by its ID.
     */
    suspend fun getTaskById(id: Long): Task?

    /**
     * Retrieves all active tasks that have geofence data (latitude and longitude).
     * Only returns tasks that are not completed and have location coordinates.
     */
    fun getActiveGeofencedTasks(): Flow<List<Task>>

    /**
     * Inserts a new task or updates an existing one.
     * @return The ID of the inserted/updated task.
     */
    suspend fun insertTask(task: Task): Long

    /**
     * Updates an existing task.
     */
    suspend fun updateTask(task: Task)

    /**
     * Deletes a task.
     */
    suspend fun deleteTask(task: Task)

    /**
     * Deletes a task by its ID.
     */
    suspend fun deleteTaskById(id: Long)

    /**
     * Retrieves all tasks of a specific type.
     */
    fun getTasksByType(type: String): Flow<List<Task>>

    /**
     * Retrieves all completed tasks.
     */
    fun getCompletedTasks(): Flow<List<Task>>

    /**
     * Retrieves all active (incomplete) tasks.
     */
    fun getActiveTasks(): Flow<List<Task>>
}
