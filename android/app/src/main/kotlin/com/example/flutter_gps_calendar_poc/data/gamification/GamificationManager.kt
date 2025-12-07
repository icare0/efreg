package com.example.flutter_gps_calendar_poc.data.gamification

import com.example.flutter_gps_calendar_poc.data.local.dao.UserStatsDao
import com.example.flutter_gps_calendar_poc.data.local.entity.toEntity
import com.example.flutter_gps_calendar_poc.domain.model.Badge
import com.example.flutter_gps_calendar_poc.domain.model.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Manages gamification features: points, badges, streaks, levels.
 *
 * Provided by DatabaseModule.provideGamificationManager()
 */
class GamificationManager(
    private val userStatsDao: UserStatsDao
) {

    companion object {
        private const val POINTS_PER_TASK = 10
        private const val BONUS_STREAK_POINTS = 5
        private const val POINTS_PER_LEVEL = 100
    }

    /**
     * Get user stats as Flow.
     */
    fun getUserStats(): Flow<UserStats> {
        return userStatsDao.getUserStats().map { entity ->
            entity?.let {
                com.example.flutter_gps_calendar_poc.data.local.entity.toDomain(it)
            } ?: UserStats() // Return default if null
        }
    }

    /**
     * Award points when a task is completed.
     */
    suspend fun onTaskCompleted() {
        userStatsDao.initializeStats() // Ensure stats exist

        val stats = userStatsDao.getUserStatsOnce() ?: return
        val currentStats = com.example.flutter_gps_calendar_poc.data.local.entity.toDomain(stats)

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Calculate points
        var points = POINTS_PER_TASK

        // Update streak
        val newStreak = calculateStreak(currentStats, now)
        if (newStreak > currentStats.currentStreak) {
            points += BONUS_STREAK_POINTS // Bonus for maintaining streak
        }

        // Calculate new totals
        val newTotalPoints = currentStats.totalPoints + points
        val newLevel = (newTotalPoints / POINTS_PER_LEVEL) + 1

        // Check for new badges
        val newBadges = checkForNewBadges(
            currentStats.copy(
                tasksCompletedTotal = currentStats.tasksCompletedTotal + 1,
                currentStreak = newStreak
            )
        )

        // Reset daily/weekly counters if needed
        val (tasksToday, tasksWeek) = updateDailyWeeklyCounters(currentStats, now)

        // Update stats
        val updatedStats = currentStats.copy(
            totalPoints = newTotalPoints,
            currentStreak = newStreak,
            longestStreak = maxOf(newStreak, currentStats.longestStreak),
            tasksCompletedToday = tasksToday + 1,
            tasksCompletedThisWeek = tasksWeek + 1,
            tasksCompletedTotal = currentStats.tasksCompletedTotal + 1,
            level = newLevel,
            badges = (currentStats.badges + newBadges).distinct(),
            lastCompletionDate = now
        )

        userStatsDao.updateStats(updatedStats.toEntity())
    }

    /**
     * Calculate current streak.
     */
    private fun calculateStreak(stats: UserStats, now: Long): Int {
        val lastCompletion = stats.lastCompletionDate ?: return 1

        val daysDiff = ((now - lastCompletion) / (1000 * 60 * 60 * 24)).toInt()

        return when {
            daysDiff == 0 -> stats.currentStreak // Same day
            daysDiff == 1 -> stats.currentStreak + 1 // Next day - increment streak
            else -> 1 // Streak broken, restart
        }
    }

    /**
     * Check for newly earned badges.
     */
    private fun checkForNewBadges(stats: UserStats): List<Badge> {
        val newBadges = mutableListOf<Badge>()

        // Streak badges
        when (stats.currentStreak) {
            3 -> if (!stats.badges.contains(Badge.STREAK_3)) newBadges.add(Badge.STREAK_3)
            7 -> if (!stats.badges.contains(Badge.STREAK_7)) newBadges.add(Badge.STREAK_7)
            30 -> if (!stats.badges.contains(Badge.STREAK_30)) newBadges.add(Badge.STREAK_30)
        }

        // Total tasks badges
        when (stats.tasksCompletedTotal) {
            1 -> if (!stats.badges.contains(Badge.FIRST_TASK)) newBadges.add(Badge.FIRST_TASK)
            10 -> if (!stats.badges.contains(Badge.TASKS_10)) newBadges.add(Badge.TASKS_10)
            50 -> if (!stats.badges.contains(Badge.TASKS_50)) newBadges.add(Badge.TASKS_50)
            100 -> if (!stats.badges.contains(Badge.TASKS_100)) newBadges.add(Badge.TASKS_100)
        }

        // Time-based badges
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < 8 && !stats.badges.contains(Badge.EARLY_BIRD)) {
            newBadges.add(Badge.EARLY_BIRD)
        }
        if (hour >= 22 && !stats.badges.contains(Badge.NIGHT_OWL)) {
            newBadges.add(Badge.NIGHT_OWL)
        }

        return newBadges
    }

    /**
     * Reset daily/weekly counters if needed.
     */
    private fun updateDailyWeeklyCounters(stats: UserStats, now: Long): Pair<Int, Int> {
        val lastCompletion = stats.lastCompletionDate ?: return Pair(0, 0)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

        calendar.timeInMillis = lastCompletion
        val lastDay = calendar.get(Calendar.DAY_OF_YEAR)
        val lastWeek = calendar.get(Calendar.WEEK_OF_YEAR)

        val tasksToday = if (currentDay == lastDay) stats.tasksCompletedToday else 0
        val tasksWeek = if (currentWeek == lastWeek) stats.tasksCompletedThisWeek else 0

        return Pair(tasksToday, tasksWeek)
    }
}
