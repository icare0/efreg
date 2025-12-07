package com.example.flutter_gps_calendar_poc.data.local.dao

import androidx.room.*
import com.example.flutter_gps_calendar_poc.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for task operations.
 * Provides methods for CRUD operations and specialized queries.
 */
@Dao
interface TaskDao {

    /**
     * Retrieves all tasks ordered by creation (ID descending).
     */
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /**
     * Retrieves a single task by ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    /**
     * Optimized query for geofencing: retrieves all active tasks that have location data.
     * Uses the index on is_completed for better performance.
     * Filters tasks where:
     * - isCompleted = 0 (not completed)
     * - latitude IS NOT NULL
     * - longitude IS NOT NULL
     */
    @Query("""
        SELECT * FROM tasks
        WHERE is_completed = 0
        AND latitude IS NOT NULL
        AND longitude IS NOT NULL
        ORDER BY id DESC
    """)
    fun getActiveGeofencedTasks(): Flow<List<TaskEntity>>

    /**
     * Retrieves all tasks of a specific type.
     */
    @Query("SELECT * FROM tasks WHERE type = :type ORDER BY id DESC")
    fun getTasksByType(type: String): Flow<List<TaskEntity>>

    /**
     * Retrieves all completed tasks.
     */
    @Query("SELECT * FROM tasks WHERE is_completed = 1 ORDER BY id DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    /**
     * Retrieves all active (incomplete) tasks.
     */
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY id DESC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    /**
     * Retrieves tasks due before a specific timestamp.
     */
    @Query("SELECT * FROM tasks WHERE due_date IS NOT NULL AND due_date <= :timestamp ORDER BY due_date ASC")
    fun getTasksDueBefore(timestamp: Long): Flow<List<TaskEntity>>

    /**
     * Inserts a new task. Returns the row ID of the inserted task.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    /**
     * Inserts multiple tasks.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    /**
     * Updates an existing task.
     */
    @Update
    suspend fun updateTask(task: TaskEntity)

    /**
     * Deletes a task.
     */
    @Delete
    suspend fun deleteTask(task: TaskEntity)

    /**
     * Deletes a task by ID.
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    /**
     * Deletes all tasks (useful for testing).
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    /**
     * Counts all tasks in the database.
     */
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    /**
     * Counts active tasks.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0")
    suspend fun getActiveTaskCount(): Int
}
