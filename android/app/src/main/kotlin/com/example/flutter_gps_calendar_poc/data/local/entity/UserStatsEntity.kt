package com.example.flutter_gps_calendar_poc.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.flutter_gps_calendar_poc.domain.model.Badge
import com.example.flutter_gps_calendar_poc.domain.model.UserStats

/**
 * Room entity for storing user statistics and gamification data.
 */
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1, // Singleton - only one stats record
    @ColumnInfo(name = "total_points") val totalPoints: Int = 0,
    @ColumnInfo(name = "current_streak") val currentStreak: Int = 0,
    @ColumnInfo(name = "longest_streak") val longestStreak: Int = 0,
    @ColumnInfo(name = "tasks_completed_today") val tasksCompletedToday: Int = 0,
    @ColumnInfo(name = "tasks_completed_this_week") val tasksCompletedThisWeek: Int = 0,
    @ColumnInfo(name = "tasks_completed_total") val tasksCompletedTotal: Int = 0,
    @ColumnInfo(name = "level") val level: Int = 1,
    @ColumnInfo(name = "badges") val badges: String = "", // Comma-separated badge names
    @ColumnInfo(name = "last_completion_date") val lastCompletionDate: Long? = null
)

/**
 * Converts entity to domain model.
 */
fun UserStatsEntity.toDomain(): UserStats {
    val badgeList = if (badges.isNotEmpty()) {
        badges.split(",").mapNotNull { name ->
            try {
                Badge.valueOf(name)
            } catch (e: Exception) {
                null
            }
        }
    } else {
        emptyList()
    }

    return UserStats(
        totalPoints = totalPoints,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        tasksCompletedToday = tasksCompletedToday,
        tasksCompletedThisWeek = tasksCompletedThisWeek,
        tasksCompletedTotal = tasksCompletedTotal,
        level = level,
        badges = badgeList,
        lastCompletionDate = lastCompletionDate
    )
}

/**
 * Converts domain model to entity.
 */
fun UserStats.toEntity(): UserStatsEntity {
    return UserStatsEntity(
        totalPoints = totalPoints,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        tasksCompletedToday = tasksCompletedToday,
        tasksCompletedThisWeek = tasksCompletedThisWeek,
        tasksCompletedTotal = tasksCompletedTotal,
        level = level,
        badges = badges.joinToString(",") { it.name },
        lastCompletionDate = lastCompletionDate
    )
}
