package com.example.flutter_gps_calendar_poc.data.ai

import android.util.Log
import com.example.flutter_gps_calendar_poc.data.local.dao.UserPreferenceDao
import com.example.flutter_gps_calendar_poc.data.local.entity.UserPreferenceEntity
import com.example.flutter_gps_calendar_poc.domain.ai.*
import com.example.flutter_gps_calendar_poc.domain.model.FreeSlot
import com.example.flutter_gps_calendar_poc.domain.model.Task
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * Adaptive scoring engine that learns from user behavior.
 *
 * Uses machine learning principles to:
 * - Score notification relevance (0-100)
 * - Learn user preferences over time
 * - Adapt to time-of-day patterns
 * - Consider context (free time, effort, urgency)
 *
 * Only sends notifications when score >= 60 (configurable threshold).
 */
@Singleton
class AdaptiveScoringEngine @Inject constructor(
    private val userPreferenceDao: UserPreferenceDao,
    private val nlpTokenizer: LocalNLPTokenizer
) {
    companion object {
        private const val TAG = "AdaptiveScoringEngine"

        // Scoring weights
        private const val WEIGHT_FREE_TIME = 0.25f       // 25%: Available free time
        private const val WEIGHT_TIME_PATTERN = 0.20f    // 20%: Time-of-day preference
        private const val WEIGHT_URGENCY = 0.20f         // 20%: Task urgency
        private const val WEIGHT_EFFORT_MATCH = 0.15f    // 15%: Effort vs free time
        private const val WEIGHT_USER_HISTORY = 0.20f    // 20%: Learned preferences

        // Learning parameters
        private const val LEARNING_WINDOW_DAYS = 30L     // Consider last 30 days
        private const val MIN_SAMPLES_FOR_LEARNING = 3   // Need 3+ samples to learn
        private const val DECAY_FACTOR = 0.95f           // Older data weighted less
    }

    /**
     * Calculates a relevance score for showing a notification.
     *
     * @param task The task to score.
     * @param freeSlot Current free time slot (null if user is busy).
     * @param analysis NLP analysis of the task.
     * @return Score from 0 to 100 (threshold: 60 to send notification).
     */
    suspend fun calculateNotificationScore(
        task: Task,
        freeSlot: FreeSlot?,
        analysis: TaskAnalysisResult
    ): Float {
        val now = Calendar.getInstance()
        val hourOfDay = now.get(Calendar.HOUR_OF_DAY)
        val isWeekend = now.get(Calendar.DAY_OF_WEEK) in listOf(
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )

        // Component scores (0-1 range)
        val freeTimeScore = calculateFreeTimeScore(freeSlot, analysis)
        val timePatternScore = calculateTimePatternScore(hourOfDay, analysis)
        val urgencyScore = calculateUrgencyScore(analysis)
        val effortMatchScore = calculateEffortMatchScore(freeSlot, analysis)
        val userHistoryScore = calculateUserHistoryScore(task, hourOfDay, isWeekend)

        // Weighted combination
        val totalScore = (
            freeTimeScore * WEIGHT_FREE_TIME +
            timePatternScore * WEIGHT_TIME_PATTERN +
            urgencyScore * WEIGHT_URGENCY +
            effortMatchScore * WEIGHT_EFFORT_MATCH +
            userHistoryScore * WEIGHT_USER_HISTORY
        ) * 100f

        Log.d(TAG, """
            Score breakdown for task ${task.id}:
            - Free Time: ${freeTimeScore * 100}%
            - Time Pattern: ${timePatternScore * 100}%
            - Urgency: ${urgencyScore * 100}%
            - Effort Match: ${effortMatchScore * 100}%
            - User History: ${userHistoryScore * 100}%
            = TOTAL: $totalScore/100
        """.trimIndent())

        return totalScore.coerceIn(0f, 100f)
    }

    /**
     * Records user feedback to improve future predictions.
     *
     * @param task The task that was notified.
     * @param action User's action (dismissed, completed, etc.).
     * @param freeSlot Free time slot at notification time.
     * @param analysis Task analysis result.
     */
    suspend fun recordUserFeedback(
        task: Task,
        action: UserFeedbackAction,
        freeSlot: FreeSlot?,
        analysis: TaskAnalysisResult
    ) {
        val now = Calendar.getInstance()

        val feedback = UserPreferenceEntity(
            taskId = task.id,
            taskType = task.type.name,
            hourOfDay = now.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = now.get(Calendar.DAY_OF_WEEK),
            isWeekend = now.get(Calendar.DAY_OF_WEEK) in listOf(
                Calendar.SATURDAY,
                Calendar.SUNDAY
            ),
            feedbackAction = action.name,
            freeSlotDurationMinutes = freeSlot?.durationMinutes,
            effortLevel = analysis.effortLevel.name
        )

        userPreferenceDao.insertFeedback(feedback)

        Log.d(TAG, "Recorded feedback: ${action.name} for task ${task.id} (${task.type}) at ${now.get(Calendar.HOUR_OF_DAY)}h")
    }

    /**
     * Scores based on available free time vs task duration.
     */
    private fun calculateFreeTimeScore(
        freeSlot: FreeSlot?,
        analysis: TaskAnalysisResult
    ): Float {
        if (freeSlot == null) return 0f

        val freeMinutes = freeSlot.durationMinutes
        val requiredMinutes = analysis.estimatedDurationMinutes

        return when {
            freeMinutes < requiredMinutes -> 0.3f  // Not enough time
            freeMinutes < requiredMinutes * 1.5f -> 0.7f  // Just enough
            else -> 1.0f  // Plenty of time
        }
    }

    /**
     * Scores based on time-of-day preference for this task type.
     */
    private fun calculateTimePatternScore(
        currentHour: Int,
        analysis: TaskAnalysisResult
    ): Float {
        val preference = analysis.timeOfDayPreference

        return when (preference) {
            TimeOfDayPreference.MORNING -> if (currentHour in 6..11) 1.0f else 0.3f
            TimeOfDayPreference.AFTERNOON -> if (currentHour in 12..17) 1.0f else 0.5f
            TimeOfDayPreference.EVENING -> if (currentHour in 18..21) 1.0f else 0.4f
            TimeOfDayPreference.NIGHT -> if (currentHour in 22..23 || currentHour in 0..5) 1.0f else 0.2f
            TimeOfDayPreference.ANY_TIME -> 0.7f
        }
    }

    /**
     * Scores based on task urgency.
     */
    private fun calculateUrgencyScore(analysis: TaskAnalysisResult): Float {
        return when (analysis.urgency) {
            UrgencyLevel.CRITICAL -> 1.0f
            UrgencyLevel.URGENT -> 0.9f
            UrgencyLevel.SOON -> 0.7f
            UrgencyLevel.NORMAL -> 0.5f
            UrgencyLevel.FLEXIBLE -> 0.3f
        }
    }

    /**
     * Scores based on effort level matching available time.
     */
    private fun calculateEffortMatchScore(
        freeSlot: FreeSlot?,
        analysis: TaskAnalysisResult
    ): Float {
        if (freeSlot == null) return 0.5f

        val effortScore = analysis.effortLevel.score
        val freeHours = freeSlot.durationHours

        // Match effort to available time
        return when {
            effortScore <= 2 && freeHours >= 0.25f -> 1.0f  // Quick tasks, any time
            effortScore == 3 && freeHours >= 0.5f -> 1.0f   // Medium tasks, 30+ min
            effortScore >= 4 && freeHours >= 1.0f -> 1.0f   // Big tasks, 1+ hour
            effortScore >= 4 && freeHours < 0.5f -> 0.2f    // Big task, no time
            else -> 0.6f
        }
    }

    /**
     * Scores based on learned user preferences (ML component).
     *
     * Uses exponential decay to weight recent feedback more heavily.
     */
    private suspend fun calculateUserHistoryScore(
        task: Task,
        hourOfDay: Int,
        isWeekend: Boolean
    ): Float {
        val cutoffTimestamp = System.currentTimeMillis() - (LEARNING_WINDOW_DAYS * 24 * 60 * 60 * 1000)

        // Get historical feedback for this task type and hour
        val feedback = userPreferenceDao.getFeedbackByTypeAndHour(
            taskType = task.type.name,
            hourOfDay = hourOfDay,
            limit = 50
        ).filter { it.timestamp >= cutoffTimestamp }

        if (feedback.size < MIN_SAMPLES_FOR_LEARNING) {
            // Not enough data, return neutral score
            return 0.5f
        }

        // Calculate weighted average with decay
        var totalWeight = 0f
        var weightedSum = 0f

        feedback.forEachIndexed { index, entry ->
            val action = try {
                UserFeedbackAction.valueOf(entry.feedbackAction)
            } catch (e: Exception) {
                UserFeedbackAction.VIEWED
            }

            // Exponential decay based on recency
            val weight = exp(-index * (1 - DECAY_FACTOR)).toFloat()
            totalWeight += weight
            weightedSum += action.weight * weight
        }

        // Normalize to 0-1 range
        // Raw score can be -2 to +2, normalize to 0-1
        val avgScore = if (totalWeight > 0) weightedSum / totalWeight else 0f
        return ((avgScore + 2f) / 4f).coerceIn(0f, 1f)
    }

    /**
     * Cleans up old feedback data (90+ days).
     */
    suspend fun cleanupOldData() {
        val cutoff = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000)
        val deleted = userPreferenceDao.deleteOldFeedback(cutoff)
        Log.d(TAG, "Cleaned up $deleted old feedback entries")
    }
}
