package com.example.flutter_gps_calendar_poc.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flutter_gps_calendar_poc.data.local.dao.TaskDao
import com.example.flutter_gps_calendar_poc.data.local.entity.TaskEntity

/**
 * Room Database for the Contextual To-Do List application.
 *
 * Version 1: Initial schema with TaskEntity.
 *
 * @property taskDao Provides access to task data operations.
 */
@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "contextual_todo_db"
    }
}
