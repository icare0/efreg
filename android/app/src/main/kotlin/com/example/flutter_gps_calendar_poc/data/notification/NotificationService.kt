package com.example.flutter_gps_calendar_poc.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flutter_gps_calendar_poc.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing notifications.
 *
 * This replaces notification_service.dart from the Flutter POC, adapted to
 * native Android with NotificationCompat and proper channel management.
 *
 * Channels (similar to Flutter POC):
 * - GEOFENCE_CHANNEL: Location-based task reminders
 * - CALENDAR_CHANNEL: Calendar event notifications
 * - GENERAL_CHANNEL: General app notifications
 */
@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        // Channel IDs (matching Flutter POC concept)
        private const val GEOFENCE_CHANNEL_ID = "geofence_notifications"
        private const val CALENDAR_CHANNEL_ID = "calendar_notifications"
        private const val GENERAL_CHANNEL_ID = "general_notifications"

        // Notification IDs
        private const val GEOFENCE_NOTIFICATION_ID = 1001
        private const val CALENDAR_NOTIFICATION_ID = 1002
        private const val GENERAL_NOTIFICATION_ID = 1003
    }

    init {
        createNotificationChannels()
    }

    /**
     * Creates notification channels for Android O and above.
     * This replaces the channel setup from notification_service.dart.
     */
    private fun createNotificationChannels() {
        // Geofence channel (HIGH priority for location-based reminders)
        val geofenceChannel = NotificationChannel(
            GEOFENCE_CHANNEL_ID,
            "Location Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications when you arrive at task locations"
            enableVibration(true)
            enableLights(true)
        }

        // Calendar channel (DEFAULT priority)
        val calendarChannel = NotificationChannel(
            CALENDAR_CHANNEL_ID,
            "Calendar Events",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications about your calendar events"
            enableVibration(true)
        }

        // General channel (LOW priority)
        val generalChannel = NotificationChannel(
            GENERAL_CHANNEL_ID,
            "General Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "General app notifications"
        }

        // Register channels
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(geofenceChannel)
        manager.createNotificationChannel(calendarChannel)
        manager.createNotificationChannel(generalChannel)
    }

    /**
     * Shows a geofence notification when user enters a task location.
     * This replaces ShowGPSNotification from the Flutter POC.
     *
     * Enhanced with action buttons for AI feedback learning.
     *
     * @param taskTitle Title of the task at this location.
     * @param taskDescription Description of the task.
     * @param locationName Name of the location.
     * @param taskId Task ID for feedback tracking.
     */
    fun showGeofenceNotification(
        taskTitle: String,
        taskDescription: String,
        locationName: String,
        taskId: Long = -1L
    ) {
        if (!hasNotificationPermission()) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📍 You're near: $taskTitle")
            .setContentText("Location: $locationName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$taskDescription\n\nLocation: $locationName")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))

        // Add action buttons for AI feedback if taskId is valid
        if (taskId != -1L) {
            val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COMPLETE
                putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, GEOFENCE_NOTIFICATION_ID)
            }
            val completePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                completeIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DISMISS
                putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, GEOFENCE_NOTIFICATION_ID)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt() + 10000,
                dismissIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .addAction(
                    android.R.drawable.ic_dialog_info,
                    "✅ Complete",
                    completePendingIntent
                )
                .addAction(
                    android.R.drawable.ic_dialog_info,
                    "❌ Dismiss",
                    dismissPendingIntent
                )
        }

        notificationManager.notify(GEOFENCE_NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Shows a calendar event notification.
     * This replaces ShowCalendarNotification from the Flutter POC.
     *
     * @param eventTitle Title of the calendar event.
     * @param eventTime Time string for the event.
     * @param location Location of the event.
     */
    fun showCalendarNotification(
        eventTitle: String,
        eventTime: String,
        location: String = ""
    ) {
        if (!hasNotificationPermission()) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (location.isNotEmpty()) {
            "$eventTime • $location"
        } else {
            eventTime
        }

        val notification = NotificationCompat.Builder(context, CALENDAR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📅 $eventTitle")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(CALENDAR_NOTIFICATION_ID, notification)
    }

    /**
     * Shows a contextual notification when user is free and near a task location.
     * This is the smart notification that checks calendar availability.
     *
     * Enhanced with action buttons for AI feedback learning.
     *
     * @param taskTitle Title of the task.
     * @param taskDescription Description of the task.
     * @param locationName Name of the location.
     * @param freeSlotDuration Duration of the current free slot in minutes.
     * @param taskId Task ID for feedback tracking.
     */
    fun showContextualTaskNotification(
        taskTitle: String,
        taskDescription: String,
        locationName: String,
        freeSlotDuration: Long,
        taskId: Long = -1L
    ) {
        if (!hasNotificationPermission()) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✨ Perfect timing! You're free now")
            .setContentText("📍 $taskTitle at $locationName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "$taskDescription\n\n" +
                                "📍 Location: $locationName\n" +
                                "⏱️ You have ${freeSlotDuration}min free"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setColor(0xFF6200EE.toInt()) // Primary color

        // Add action buttons for AI feedback if taskId is valid
        if (taskId != -1L) {
            val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COMPLETE
                putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, GEOFENCE_NOTIFICATION_ID)
            }
            val completePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                completeIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DISMISS
                putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, GEOFENCE_NOTIFICATION_ID)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt() + 10000,
                dismissIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .addAction(
                    android.R.drawable.ic_dialog_info,
                    "✅ Complete",
                    completePendingIntent
                )
                .addAction(
                    android.R.drawable.ic_dialog_info,
                    "❌ Dismiss",
                    dismissPendingIntent
                )
        }

        notificationManager.notify(GEOFENCE_NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Shows a general test notification.
     * This replaces ShowTestNotification from the Flutter POC.
     */
    fun showTestNotification(title: String, message: String) {
        if (!hasNotificationPermission()) {
            return
        }

        val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(GENERAL_NOTIFICATION_ID, notification)
    }

    /**
     * Cancels all notifications.
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Checks if the app has notification permission (Android 13+).
     */
    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
