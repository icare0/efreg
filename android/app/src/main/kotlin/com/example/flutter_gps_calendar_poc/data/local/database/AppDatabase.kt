package com.example.flutter_gps_calendar_poc.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flutter_gps_calendar_poc.data.local.dao.TaskDao
import com.example.flutter_gps_calendar_poc.data.local.dao.UserPreferenceDao
import com.example.flutter_gps_calendar_poc.data.local.dao.UserStatsDao
import com.example.flutter_gps_calendar_poc.data.local.entity.TaskEntity
import com.example.flutter_gps_calendar_poc.data.local.entity.UserPreferenceEntity
import com.example.flutter_gps_calendar_poc.data.local.entity.UserStatsEntity

/**
 * Room Database for the Contextual To-Do List application.
 *
 * Version 1: Initial schema with TaskEntity.
 * Version 2: Added UserPreferenceEntity for AI learning.
 * Version 3: Added UserStatsEntity for gamification.
 *
 * @property taskDao Provides access to task data operations.
 * @property userPreferenceDao Provides access to user feedback for AI learning.
 * @property userStatsDao Provides access to gamification data.
 */
@Database(
    entities = [
        TaskEntity::class,
        UserPreferenceEntity::class,
        UserStatsEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        const val DATABASE_NAME = "contextual_todo_db"
    }
}
