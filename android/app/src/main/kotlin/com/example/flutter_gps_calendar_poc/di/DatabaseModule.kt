package com.example.flutter_gps_calendar_poc.di

import android.content.Context
import androidx.room.Room
import com.example.flutter_gps_calendar_poc.data.local.dao.TaskDao
import com.example.flutter_gps_calendar_poc.data.local.dao.UserPreferenceDao
import com.example.flutter_gps_calendar_poc.data.local.dao.UserStatsDao
import com.example.flutter_gps_calendar_poc.data.local.database.AppDatabase
import com.example.flutter_gps_calendar_poc.data.repository.TaskRepositoryImpl
import com.example.flutter_gps_calendar_poc.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 *
 * This module is installed in the SingletonComponent, meaning all provided
 * dependencies will have a singleton lifecycle (one instance per application).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance.
     *
     * The database is created with:
     * - Fallback to destructive migration for development (should be changed for production)
     * - Application context to avoid memory leaks
     *
     * @param context Application context injected by Hilt.
     * @return Singleton instance of AppDatabase.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development; use proper migrations in production
            .build()
    }

    /**
     * Provides the TaskDao instance from the database.
     *
     * @param database The AppDatabase instance.
     * @return TaskDao for task operations.
     */
    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    /**
     * Provides the UserPreferenceDao instance from the database.
     *
     * Used by the AI adaptive scoring engine to learn user preferences.
     *
     * @param database The AppDatabase instance.
     * @return UserPreferenceDao for AI learning operations.
     */
    @Provides
    @Singleton
    fun provideUserPreferenceDao(database: AppDatabase): UserPreferenceDao {
        return database.userPreferenceDao()
    }

    /**
     * Provides the UserStatsDao instance from the database.
     *
     * Used by the gamification system to track user progress.
     *
     * @param database The AppDatabase instance.
     * @return UserStatsDao for gamification operations.
     */
    @Provides
    @Singleton
    fun provideUserStatsDao(database: AppDatabase): UserStatsDao {
        return database.userStatsDao()
    }

    /**
     * Provides the TaskRepository implementation.
     *
     * Binds the TaskRepositoryImpl to the TaskRepository interface.
     * This allows the domain layer to depend on the interface while
     * the data layer provides the concrete implementation.
     *
     * @param taskDao The TaskDao instance.
     * @return TaskRepository implementation.
     */
    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepositoryImpl(taskDao)
    }
}
