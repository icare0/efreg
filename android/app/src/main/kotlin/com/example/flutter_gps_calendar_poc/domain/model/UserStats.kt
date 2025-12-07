package com.example.flutter_gps_calendar_poc.domain.model

/**
 * User statistics and gamification data.
 *
 * Tracks user progress, achievements, and engagement metrics
 * to encourage task completion and app usage.
 */
data class UserStats(
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val tasksCompletedToday: Int = 0,
    val tasksCompletedThisWeek: Int = 0,
    val tasksCompletedTotal: Int = 0,
    val level: Int = 1,
    val badges: List<Badge> = emptyList(),
    val lastCompletionDate: Long? = null
) {
    /**
     * Points needed for next level (exponential growth).
     */
    val pointsForNextLevel: Int
        get() = level * 100

    /**
     * Current progress towards next level (0.0 to 1.0).
     */
    val levelProgress: Float
        get() = (totalPoints % pointsForNextLevel).toFloat() / pointsForNextLevel
}

/**
 * Achievement badges for gamification.
 */
enum class Badge(val displayName: String, val icon: String, val description: String) {
    FIRST_TASK("Débutant", "🎯", "Complète ta première tâche"),
    STREAK_3("Sur une lancée", "🔥", "3 jours consécutifs"),
    STREAK_7("Une semaine!", "⭐", "7 jours consécutifs"),
    STREAK_30("Un mois!", "🏆", "30 jours consécutifs"),
    TASKS_10("Travailleur", "💪", "10 tâches complétées"),
    TASKS_50("Expert", "🎓", "50 tâches complétées"),
    TASKS_100("Maître", "👑", "100 tâches complétées"),
    EARLY_BIRD("Lève-tôt", "🌅", "Tâche avant 8h"),
    NIGHT_OWL("Oiseau de nuit", "🦉", "Tâche après 22h"),
    PERFECT_WEEK("Semaine parfaite", "✨", "Toutes les tâches complétées cette semaine")
}
