package com.example.flutter_gps_calendar_poc.data.local.dao

import androidx.room.*
import com.example.flutter_gps_calendar_poc.data.local.entity.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user preference and feedback data.
 *
 * Supports the adaptive scoring engine by providing:
 * - Feedback history for learning patterns
 * - Time-based preference queries
 * - Task type preference analysis
 */
@Dao
interface UserPreferenceDao {

    /**
     * Inserts a new user feedback entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(preference: UserPreferenceEntity): Long

    /**
     * Gets all feedback for a specific task type and hour.
     * Used to learn when user prefers to do certain tasks.
     */
    @Query("""
        SELECT * FROM user_preferences
        WHERE task_type = :taskType
        AND hour_of_day = :hourOfDay
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getFeedbackByTypeAndHour(
        taskType: String,
        hourOfDay: Int,
        limit: Int = 50
    ): List<UserPreferenceEntity>

    /**
     * Gets all feedback for a specific task type.
     */
    @Query("""
        SELECT * FROM user_preferences
        WHERE task_type = :taskType
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getFeedbackByType(
        taskType: String,
        limit: Int = 100
    ): List<UserPreferenceEntity>

    /**
     * Gets all feedback within a time range.
     */
    @Query("""
        SELECT * FROM user_preferences
        WHERE timestamp >= :startTimestamp
        AND timestamp <= :endTimestamp
        ORDER BY timestamp DESC
    """)
    suspend fun getFeedbackInRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): List<UserPreferenceEntity>

    /**
     * Gets feedback for weekend vs weekday patterns.
     */
    @Query("""
        SELECT * FROM user_preferences
        WHERE is_weekend = :isWeekend
        AND task_type = :taskType
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getFeedbackByWeekendPattern(
        isWeekend: Boolean,
        taskType: String,
        limit: Int = 50
    ): List<UserPreferenceEntity>

    /**
     * Calculates average score for a task type at a specific hour.
     * Used for adaptive scoring.
     */
    @Query("""
        SELECT AVG(
            CASE feedback_action
                WHEN 'COMPLETED' THEN 2
                WHEN 'MARKED_HELPFUL' THEN 1
                WHEN 'VIEWED' THEN 0
                WHEN 'SNOOZED' THEN -1
                WHEN 'DISMISSED' THEN -2
                ELSE 0
            END
        ) as avg_score
        FROM user_preferences
        WHERE task_type = :taskType
        AND hour_of_day = :hourOfDay
        AND timestamp >= :sinceTimestamp
    """)
    suspend fun getAverageScoreByTypeAndHour(
        taskType: String,
        hourOfDay: Int,
        sinceTimestamp: Long
    ): Float?

    /**
     * Deletes old feedback entries (older than 90 days).
     */
    @Query("""
        DELETE FROM user_preferences
        WHERE timestamp < :cutoffTimestamp
    """)
    suspend fun deleteOldFeedback(cutoffTimestamp: Long): Int

    /**
     * Gets total feedback count.
     */
    @Query("SELECT COUNT(*) FROM user_preferences")
    suspend fun getFeedbackCount(): Int

    /**
     * Observes all feedback entries (for debugging/admin).
     */
    @Query("SELECT * FROM user_preferences ORDER BY timestamp DESC LIMIT 100")
    fun observeRecentFeedback(): Flow<List<UserPreferenceEntity>>
}
