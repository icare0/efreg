package com.example.flutter_gps_calendar_poc.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.model.TaskType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component for displaying a task item.
 *
 * @param task The task to display.
 * @param onToggleComplete Callback when the checkbox is clicked.
 * @param onDelete Callback when the delete button is clicked.
 * @param modifier Optional modifier.
 */
@Composable
fun TaskItemCard(
    task: Task,
    onToggleComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task type icon
            Icon(
                imageVector = getTaskTypeIcon(task.type),
                contentDescription = task.type.name,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp),
                tint = getTaskTypeColor(task.type)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                // Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) {
                        TextDecoration.LineThrough
                    } else {
                        null
                    }
                )

                // Description
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Due date
                if (task.dueDate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Due date",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(task.dueDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Location
                if (task.locationName.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.locationName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete(task) }
            )

            // Delete button
            IconButton(onClick = { onDelete(task) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Returns the icon for a task type.
 */
@Composable
private fun getTaskTypeIcon(type: TaskType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        TaskType.TASK -> Icons.Default.CheckCircle
        TaskType.SHOPPING -> Icons.Default.ShoppingCart
        TaskType.WORK -> Icons.Default.Work
        TaskType.SPORT -> Icons.Default.FitnessCenter
    }
}

/**
 * Returns the color for a task type.
 */
@Composable
private fun getTaskTypeColor(type: TaskType): androidx.compose.ui.graphics.Color {
    return when (type) {
        TaskType.TASK -> MaterialTheme.colorScheme.primary
        TaskType.SHOPPING -> MaterialTheme.colorScheme.secondary
        TaskType.WORK -> MaterialTheme.colorScheme.tertiary
        TaskType.SPORT -> MaterialTheme.colorScheme.error
    }
}

/**
 * Formats a timestamp to a readable date string.
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
