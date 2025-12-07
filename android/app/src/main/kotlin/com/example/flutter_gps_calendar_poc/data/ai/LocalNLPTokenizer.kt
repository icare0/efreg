package com.example.flutter_gps_calendar_poc.data.ai

import com.example.flutter_gps_calendar_poc.domain.ai.*
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.model.TaskType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local NLP tokenizer for analyzing task text without cloud APIs.
 *
 * Uses keyword matching and pattern recognition to:
 * - Suggest task categories
 * - Estimate effort level
 * - Detect urgency
 * - Determine optimal time of day
 *
 * Supports French and English keywords.
 */
@Singleton
class LocalNLPTokenizer @Inject constructor() {

    companion object {
        // Keyword maps for task categorization (FR/EN)
        private val SHOPPING_KEYWORDS = setOf(
            // French
            "acheter", "courses", "pain", "lait", "supermarché", "magasin",
            "pharmacie", "boulangerie", "épicerie", "marché",
            // English
            "buy", "purchase", "shopping", "groceries", "store", "shop",
            "mall", "market", "pharmacy", "bakery"
        )

        private val WORK_KEYWORDS = setOf(
            // French
            "travail", "réunion", "présentation", "rapport", "email",
            "projet", "client", "bureau", "rendez-vous", "meeting",
            // English
            "work", "meeting", "presentation", "report", "email",
            "project", "client", "office", "appointment", "deadline"
        )

        private val SPORT_KEYWORDS = setOf(
            // French
            "sport", "gym", "courir", "jogging", "yoga", "piscine",
            "vélo", "fitness", "entraînement", "musculation",
            // English
            "sport", "gym", "run", "running", "jogging", "yoga",
            "pool", "swim", "bike", "cycling", "fitness", "workout", "training"
        )

        // Effort detection (action verbs)
        private val HIGH_EFFORT_VERBS = setOf(
            // French
            "organiser", "préparer", "nettoyer", "ranger", "réparer",
            "construire", "rénover", "déménager", "installer",
            // English
            "organize", "prepare", "clean", "fix", "repair",
            "build", "renovate", "move", "install", "setup"
        )

        private val LOW_EFFORT_VERBS = setOf(
            // French
            "appeler", "envoyer", "noter", "vérifier", "lire",
            "regarder", "rappeler", "confirmer",
            // English
            "call", "send", "note", "check", "read",
            "watch", "remind", "confirm", "email"
        )

        // Urgency indicators
        private val URGENT_KEYWORDS = setOf(
            // French
            "urgent", "immédiatement", "asap", "vite", "rapidement",
            "aujourd'hui", "maintenant", "tout de suite", "prioritaire",
            // English
            "urgent", "immediately", "asap", "quickly", "fast",
            "today", "now", "right now", "priority", "critical"
        )

        private val FLEXIBLE_KEYWORDS = setOf(
            // French
            "quand possible", "un jour", "éventuellement", "plus tard",
            "à l'occasion", "si temps",
            // English
            "when possible", "someday", "eventually", "later",
            "whenever", "if time", "no rush"
        )

        // Time of day indicators
        private val MORNING_KEYWORDS = setOf(
            "matin", "morning", "tôt", "early", "réveil", "wake",
            "petit-déjeuner", "breakfast"
        )

        private val EVENING_KEYWORDS = setOf(
            "soir", "evening", "après-travail", "after work",
            "dîner", "dinner", "nuit", "night"
        )
    }

    /**
     * Analyzes a task and returns NLP insights.
     *
     * @param task The task to analyze.
     * @return Analysis result with categorization, effort, urgency, etc.
     */
    fun analyzeTask(task: Task): TaskAnalysisResult {
        val text = "${task.title} ${task.description}".lowercase()
        val tokens = tokenize(text)

        val detectedKeywords = mutableListOf<String>()
        val suggestedType = detectTaskType(tokens, task.type, detectedKeywords)
        val effortLevel = detectEffortLevel(tokens)
        val urgency = detectUrgency(tokens)
        val timePreference = detectTimeOfDayPreference(tokens, suggestedType)
        val estimatedDuration = estimateDuration(effortLevel, suggestedType)
        val confidence = calculateConfidence(detectedKeywords.size, tokens.size)

        return TaskAnalysisResult(
            taskId = task.id,
            detectedKeywords = detectedKeywords,
            suggestedType = suggestedType,
            effortLevel = effortLevel,
            urgency = urgency,
            timeOfDayPreference = timePreference,
            estimatedDurationMinutes = estimatedDuration,
            confidence = confidence
        )
    }

    /**
     * Tokenizes text into words, removing punctuation.
     */
    private fun tokenize(text: String): List<String> {
        return text
            .replace(Regex("[^a-zà-ÿ0-9\\s-]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    /**
     * Detects task type based on keywords.
     */
    private fun detectTaskType(
        tokens: List<String>,
        userType: TaskType,
        detectedKeywords: MutableList<String>
    ): TaskType {
        val scores = mutableMapOf(
            TaskType.SHOPPING to 0,
            TaskType.WORK to 0,
            TaskType.SPORT to 0,
            TaskType.TASK to 0
        )

        tokens.forEach { token ->
            when {
                token in SHOPPING_KEYWORDS -> {
                    scores[TaskType.SHOPPING] = scores[TaskType.SHOPPING]!! + 1
                    detectedKeywords.add(token)
                }
                token in WORK_KEYWORDS -> {
                    scores[TaskType.WORK] = scores[TaskType.WORK]!! + 1
                    detectedKeywords.add(token)
                }
                token in SPORT_KEYWORDS -> {
                    scores[TaskType.SPORT] = scores[TaskType.SPORT]!! + 1
                    detectedKeywords.add(token)
                }
            }
        }

        // If user already categorized, boost that type
        scores[userType] = scores[userType]!! + 2

        // Return type with highest score, or TASK if no clear winner
        return scores.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: TaskType.TASK
    }

    /**
     * Detects effort level based on action verbs.
     */
    private fun detectEffortLevel(tokens: List<String>): EffortLevel {
        val hasHighEffort = tokens.any { it in HIGH_EFFORT_VERBS }
        val hasLowEffort = tokens.any { it in LOW_EFFORT_VERBS }

        return when {
            hasHighEffort -> EffortLevel.HIGH
            hasLowEffort -> EffortLevel.LOW
            else -> EffortLevel.MEDIUM
        }
    }

    /**
     * Detects urgency level based on keywords.
     */
    private fun detectUrgency(tokens: List<String>): UrgencyLevel {
        val hasUrgent = tokens.any { it in URGENT_KEYWORDS }
        val hasFlexible = tokens.any { it in FLEXIBLE_KEYWORDS }

        return when {
            hasUrgent -> UrgencyLevel.URGENT
            hasFlexible -> UrgencyLevel.FLEXIBLE
            else -> UrgencyLevel.NORMAL
        }
    }

    /**
     * Detects preferred time of day based on task type and keywords.
     */
    private fun detectTimeOfDayPreference(
        tokens: List<String>,
        taskType: TaskType
    ): TimeOfDayPreference {
        val hasMorning = tokens.any { it in MORNING_KEYWORDS }
        val hasEvening = tokens.any { it in EVENING_KEYWORDS }

        return when {
            hasMorning -> TimeOfDayPreference.MORNING
            hasEvening -> TimeOfDayPreference.EVENING
            taskType == TaskType.SPORT -> TimeOfDayPreference.MORNING
            taskType == TaskType.SHOPPING -> TimeOfDayPreference.EVENING
            taskType == TaskType.WORK -> TimeOfDayPreference.AFTERNOON
            else -> TimeOfDayPreference.ANY_TIME
        }
    }

    /**
     * Estimates task duration based on effort and type.
     */
    private fun estimateDuration(effort: EffortLevel, type: TaskType): Int {
        val baseMinutes = when (effort) {
            EffortLevel.TRIVIAL -> 5
            EffortLevel.LOW -> 15
            EffortLevel.MEDIUM -> 30
            EffortLevel.HIGH -> 60
            EffortLevel.VERY_HIGH -> 120
        }

        // Adjust by task type
        val multiplier = when (type) {
            TaskType.SHOPPING -> 1.5f  // Shopping takes longer
            TaskType.WORK -> 1.2f      // Work tasks vary
            TaskType.SPORT -> 1.0f     // Sport is predictable
            TaskType.TASK -> 1.0f
        }

        return (baseMinutes * multiplier).toInt()
    }

    /**
     * Calculates confidence score based on keyword matches.
     */
    private fun calculateConfidence(keywordsFound: Int, totalTokens: Int): Float {
        if (totalTokens == 0) return 0f

        val ratio = keywordsFound.toFloat() / totalTokens.toFloat()
        return (ratio * 100).coerceIn(0f, 100f) / 100f
    }
}
