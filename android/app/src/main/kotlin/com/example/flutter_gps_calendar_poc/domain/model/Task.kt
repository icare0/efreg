package com.example.flutter_gps_calendar_poc.domain.model

/**
 * Domain model representing a task in the Contextual To-Do List application.
 * This is a pure data class without any framework dependencies.
 */
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "",
    val type: TaskType = TaskType.TASK
)

/**
 * Enum representing different task categories.
 */
enum class TaskType {
    TASK,
    SHOPPING,
    WORK,
    SPORT
}
