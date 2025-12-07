package com.example.flutter_gps_calendar_poc.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flutter_gps_calendar_poc.data.ai.AdaptiveScoringEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic worker that cleans up old AI feedback data.
 *
 * Runs daily to remove feedback entries older than 90 days,
 * keeping the database lean and maintaining privacy.
 *
 * This helps:
 * - Reduce database size
 * - Maintain user privacy (old data is removed)
 * - Keep AI learning focused on recent patterns
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scoringEngine: AdaptiveScoringEngine
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "CleanupWorker"
        const val WORK_NAME = "cleanup_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily cleanup of old AI feedback data")

            // Clean up old feedback (>90 days)
            scoringEngine.cleanupOldData()

            Log.d(TAG, "Cleanup completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            Result.failure()
        }
    }
}
