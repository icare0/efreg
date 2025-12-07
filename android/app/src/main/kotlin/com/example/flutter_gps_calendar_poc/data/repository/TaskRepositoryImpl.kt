package com.example.flutter_gps_calendar_poc.data.repository

import com.example.flutter_gps_calendar_poc.data.local.dao.TaskDao
import com.example.flutter_gps_calendar_poc.data.local.entity.toDomain
import com.example.flutter_gps_calendar_poc.data.local.entity.toEntity
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of TaskRepository using Room database.
 *
 * This class acts as a bridge between the data layer (Room) and the domain layer,
 * converting between TaskEntity (data) and Task (domain) models.
 *
 * @property taskDao The Data Access Object for task operations.
 */
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    override fun getActiveGeofencedTasks(): Flow<List<Task>> {
        return taskDao.getActiveGeofencedTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    override fun getTasksByType(type: String): Flow<List<Task>> {
        return taskDao.getTasksByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
