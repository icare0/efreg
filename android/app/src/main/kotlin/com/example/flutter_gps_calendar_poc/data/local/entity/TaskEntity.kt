package com.example.flutter_gps_calendar_poc.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.model.TaskType

/**
 * Room entity representing a task in the local database.
 *
 * The @Index annotation on isCompleted improves query performance for filtering
 * completed/active tasks, especially important for geofencing queries.
 */
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["is_completed"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,

    @ColumnInfo(name = "due_date")
    val dueDate: Long?,

    @ColumnInfo(name = "latitude")
    val latitude: Double?,

    @ColumnInfo(name = "longitude")
    val longitude: Double?,

    @ColumnInfo(name = "location_name")
    val locationName: String,

    @ColumnInfo(name = "type")
    val type: String
)

/**
 * Extension function to convert TaskEntity to domain Task model.
 */
fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        dueDate = dueDate,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        type = TaskType.valueOf(type)
    )
}

/**
 * Extension function to convert domain Task model to TaskEntity.
 */
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        dueDate = dueDate,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        type = type.name
    )
}
