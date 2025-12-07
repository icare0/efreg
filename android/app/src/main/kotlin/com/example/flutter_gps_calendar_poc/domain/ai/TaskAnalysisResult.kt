package com.example.flutter_gps_calendar_poc.domain.ai

import com.example.flutter_gps_calendar_poc.domain.model.TaskType

/**
 * Result of local NLP analysis on a task.
 *
 * This represents the AI's understanding of a task based on its text content.
 */
data class TaskAnalysisResult(
    val taskId: Long,
    val detectedKeywords: List<String>,
    val suggestedType: TaskType,
    val effortLevel: EffortLevel,
    val urgency: UrgencyLevel,
    val timeOfDayPreference: TimeOfDayPreference,
    val estimatedDurationMinutes: Int,
    val confidence: Float // 0.0 to 1.0
)

/**
 * Estimated effort required for a task.
 */
enum class EffortLevel(val score: Int) {
    TRIVIAL(1),    // Quick action: "buy milk"
    LOW(2),        // Simple task: "call dentist"
    MEDIUM(3),     // Moderate effort: "organize closet"
    HIGH(4),       // Significant effort: "prepare presentation"
    VERY_HIGH(5)   // Major effort: "deep clean apartment"
}

/**
 * Urgency level of a task.
 */
enum class UrgencyLevel(val score: Int) {
    FLEXIBLE(1),   // "someday", "eventually"
    NORMAL(2),     // No urgency indicators
    SOON(3),       // "soon", "this week"
    URGENT(4),     // "urgent", "asap"
    CRITICAL(5)    // "immediately", "emergency"
}

/**
 * Preferred time of day for a task based on its nature.
 */
enum class TimeOfDayPreference {
    MORNING,       // 6-12h: Energetic tasks, sports
    AFTERNOON,     // 12-18h: Work tasks, meetings
    EVENING,       // 18-22h: Shopping, leisure
    NIGHT,         // 22-6h: Quiet tasks
    ANY_TIME       // No preference
}

/**
 * User action taken in response to a notification.
 */
enum class UserFeedbackAction(val weight: Int) {
    DISMISSED(-2),      // Swiped away = not relevant
    SNOOZED(-1),        // Not now, but maybe later
    VIEWED(0),          // Opened but didn't act
    COMPLETED(2),       // Completed the task = perfect timing!
    MARKED_HELPFUL(1)   // Explicitly marked as helpful
}
