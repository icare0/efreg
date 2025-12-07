package com.example.flutter_gps_calendar_poc.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing user feedback and learned preferences.
 *
 * This powers the adaptive scoring engine by tracking:
 * - When user dismisses/completes notifications
 * - What types of tasks work at what times
 * - User behavior patterns over time
 */
@Entity(
    tableName = "user_preferences",
    indices = [
        Index(value = ["task_type", "hour_of_day"]),
        Index(value = ["timestamp"])
    ]
)
data class UserPreferenceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Long,

    @ColumnInfo(name = "task_type")
    val taskType: String,  // TaskType enum name

    @ColumnInfo(name = "hour_of_day")
    val hourOfDay: Int,  // 0-23

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,  // 1-7 (Monday-Sunday)

    @ColumnInfo(name = "is_weekend")
    val isWeekend: Boolean,

    @ColumnInfo(name = "feedback_action")
    val feedbackAction: String,  // UserFeedbackAction enum name

    @ColumnInfo(name = "free_slot_duration_minutes")
    val freeSlotDurationMinutes: Long?,

    @ColumnInfo(name = "effort_level")
    val effortLevel: String?,  // EffortLevel enum name

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
