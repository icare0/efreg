package com.example.flutter_gps_calendar_poc.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flutter_gps_calendar_poc.data.ai.AdaptiveScoringEngine
import com.example.flutter_gps_calendar_poc.data.ai.LocalNLPTokenizer
import com.example.flutter_gps_calendar_poc.data.notification.NotificationService
import com.example.flutter_gps_calendar_poc.domain.model.FreeSlot
import com.example.flutter_gps_calendar_poc.domain.repository.CalendarRepository
import com.example.flutter_gps_calendar_poc.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Worker that checks calendar availability when a geofence is triggered.
 *
 * Enhanced with local AI to intelligently decide when to notify:
 * 1. User enters a task location (geofence triggered)
 * 2. Worker checks if user is currently free (no calendar events)
 * 3. **AI analyzes task text with NLP** (keywords, effort, urgency)
 * 4. **Adaptive scoring engine calculates relevance score (0-100)**
 * 5. Only notifies if score >= 60 (learned from user feedback)
 * 6. Records user action to improve future predictions
 *
 * This replaces the manual GPS monitoring from the Flutter POC with
 * intelligent, battery-efficient, ML-powered geofencing.
 */
@HiltWorker
class GeofenceCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val notificationService: NotificationService,
    private val nlpTokenizer: LocalNLPTokenizer,
    private val scoringEngine: AdaptiveScoringEngine
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "GeofenceCheckWorker"
        const val KEY_TASK_ID = "task_id"

        // Minimum free slot duration to trigger contextual notification (in minutes)
        private const val MIN_FREE_SLOT_MINUTES = 15L

        // AI threshold: Only notify if score >= this value
        private const val NOTIFICATION_SCORE_THRESHOLD = 60f
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)

        if (taskId == -1L) {
            Log.e(TAG, "Invalid task ID")
            return Result.failure()
        }

        Log.d(TAG, "🤖 AI-powered geofence check for task $taskId")

        return try {
            // 1. Get the task
            val task = taskRepository.getTaskById(taskId)

            if (task == null) {
                Log.w(TAG, "Task $taskId not found")
                return Result.failure()
            }

            if (task.isCompleted) {
                Log.d(TAG, "Task $taskId is already completed, skipping notification")
                return Result.success()
            }

            Log.d(TAG, "📋 Found task: ${task.title} at ${task.locationName}")

            // 2. Run local NLP analysis
            val analysis = nlpTokenizer.analyzeTask(task)

            Log.d(TAG, """
                🧠 NLP Analysis:
                - Type: ${analysis.suggestedType}
                - Effort: ${analysis.effortLevel}
                - Urgency: ${analysis.urgency}
                - Time preference: ${analysis.timeOfDayPreference}
                - Estimated duration: ${analysis.estimatedDurationMinutes}min
                - Keywords: ${analysis.detectedKeywords.joinToString()}
                - Confidence: ${(analysis.confidence * 100).toInt()}%
            """.trimIndent())

            // 3. Check calendar availability
            val freeSlotData = checkIfUserIsFree()
            val isFree = freeSlotData.first
            val freeSlot = freeSlotData.second

            // 4. Calculate AI relevance score
            val score = scoringEngine.calculateNotificationScore(
                task = task,
                freeSlot = freeSlot,
                analysis = analysis
            )

            Log.d(TAG, "🎯 AI Score: $score/100 (threshold: $NOTIFICATION_SCORE_THRESHOLD)")

            // 5. Decision: notify or skip
            if (score >= NOTIFICATION_SCORE_THRESHOLD) {
                // High score - send notification
                if (isFree) {
                    val freeSlotDuration = freeSlot?.durationMinutes ?: 0L

                    Log.d(TAG, "✅ NOTIFY: User is FREE (${freeSlotDuration}min) + High score")

                    notificationService.showContextualTaskNotification(
                        taskTitle = task.title,
                        taskDescription = task.description,
                        locationName = task.locationName,
                        freeSlotDuration = freeSlotDuration
                    )
                } else {
                    Log.d(TAG, "✅ NOTIFY: User is BUSY but score is high, sending simple reminder")

                    notificationService.showGeofenceNotification(
                        taskTitle = task.title,
                        taskDescription = task.description,
                        locationName = task.locationName
                    )
                }
            } else {
                // Low score - skip notification
                Log.d(TAG, "❌ SKIP: Score too low ($score < $NOTIFICATION_SCORE_THRESHOLD)")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing geofence check for task $taskId", e)
            Result.failure()
        }
    }

    /**
     * Checks if the user is currently free (no ongoing calendar events).
     *
     * Logic:
     * 1. Get current calendar events
     * 2. Check if any event is happening now
     * 3. If no current events, calculate current free slot duration
     *
     * @return Pair<isFree, freeSlot?>
     */
    private suspend fun checkIfUserIsFree(): Pair<Boolean, FreeSlot?> {
        return try {
            // Check if user has calendar permission
            val hasPermission = calendarRepository.hasCalendarPermission()

            if (!hasPermission) {
                Log.w(TAG, "Calendar permission not granted, assuming user is free")
                return Pair(true, null)
            }

            // Get current events
            val currentEvents = calendarRepository.getCurrentEvents().first()

            if (currentEvents.isNotEmpty()) {
                // User has events happening now - NOT free
                Log.d(TAG, "User has ${currentEvents.size} current events")
                return Pair(false, null)
            }

            // User has no current events - check free slot duration
            val now = System.currentTimeMillis()
            val upcomingEvents = calendarRepository.getUpcomingEvents(hoursAhead = 3).first()

            val freeSlot = if (upcomingEvents.isNotEmpty()) {
                // Free until next event
                val nextEvent = upcomingEvents.minByOrNull { it.startTime }!!
                FreeSlot(now, nextEvent.startTime)
            } else {
                // No upcoming events in next 3 hours - use a default slot
                FreeSlot(now, now + (3 * 60 * 60 * 1000))
            }

            // Only consider it a valid free slot if it's at least MIN_FREE_SLOT_MINUTES
            val isFree = freeSlot.isAtLeast(MIN_FREE_SLOT_MINUTES)

            Log.d(TAG, "Free slot duration: ${freeSlot.durationMinutes}min, isFree: $isFree")

            Pair(isFree, freeSlot)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking calendar availability", e)
            // In case of error, assume user is free
            Pair(true, null)
        }
    }
}
