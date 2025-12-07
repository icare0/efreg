package com.example.flutter_gps_calendar_poc.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flutter_gps_calendar_poc.data.local.dao.TaskDao
import com.example.flutter_gps_calendar_poc.data.local.dao.UserPreferenceDao
import com.example.flutter_gps_calendar_poc.data.local.entity.TaskEntity
import com.example.flutter_gps_calendar_poc.data.local.entity.UserPreferenceEntity

/**
 * Room Database for the Contextual To-Do List application.
 *
 * Version 1: Initial schema with TaskEntity.
 * Version 2: Added UserPreferenceEntity for AI learning.
 *
 * @property taskDao Provides access to task data operations.
 * @property userPreferenceDao Provides access to user feedback for AI learning.
 */
@Database(
    entities = [
        TaskEntity::class,
        UserPreferenceEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userPreferenceDao(): UserPreferenceDao

    companion object {
        const val DATABASE_NAME = "contextual_todo_db"
    }
}
