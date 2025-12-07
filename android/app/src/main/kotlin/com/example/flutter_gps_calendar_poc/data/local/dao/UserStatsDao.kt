package com.example.flutter_gps_calendar_poc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flutter_gps_calendar_poc.data.local.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user statistics and gamification data.
 */
@Dao
interface UserStatsDao {

    /**
     * Get user stats as Flow (reactive).
     */
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    /**
     * Get user stats once (non-reactive).
     */
    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsOnce(): UserStatsEntity?

    /**
     * Insert or replace user stats.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: UserStatsEntity)

    /**
     * Update user stats.
     */
    @Update
    suspend fun updateStats(stats: UserStatsEntity)

    /**
     * Initialize stats if they don't exist.
     */
    @Query("""
        INSERT OR IGNORE INTO user_stats (
            id, total_points, current_streak, longest_streak,
            tasks_completed_today, tasks_completed_this_week, tasks_completed_total,
            level, badges, last_completion_date
        ) VALUES (1, 0, 0, 0, 0, 0, 0, 1, '', NULL)
    """)
    suspend fun initializeStats()
}
