package com.example.flutter_gps_calendar_poc.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flutter_gps_calendar_poc.domain.model.Task
import com.example.flutter_gps_calendar_poc.domain.model.TaskType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for adding a new task.
 *
 * Enhanced with:
 * - DatePicker for due date selection
 * - Location input with coordinates support
 * - Task type selection with icons
 *
 * @param onDismiss Callback when the dialog is dismissed.
 * @param onConfirm Callback when the task is confirmed (receives the new task).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TaskType.TASK) }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add Task")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = "Title")
                    }
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = "Description")
                    }
                )

                // Due date picker
                OutlinedTextField(
                    value = selectedDate?.let { dateFormatter.format(Date(it)) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due Date (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Due Date")
                    },
                    trailingIcon = {
                        if (selectedDate != null) {
                            IconButton(onClick = { selectedDate = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear date")
                            }
                        }
                    }
                )

                // Task type dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = getTaskTypeIcon(selectedType),
                                contentDescription = "Type"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TaskType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getTaskTypeIcon(type),
                                        contentDescription = type.name
                                    )
                                }
                            )
                        }
                    }
                }

                // Location section
                Text(
                    text = "Location (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Place name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location")
                    },
                    placeholder = { Text("e.g., Supermarket, Gym") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = {
                            // Only allow numbers and decimal point
                            if (it.isEmpty() || it.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                latitude = it
                            }
                        },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("48.8566") }
                    )

                    OutlinedTextField(
                        value = longitude,
                        onValueChange = {
                            // Only allow numbers and decimal point
                            if (it.isEmpty() || it.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                longitude = it
                            }
                        },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("2.3522") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val lat = latitude.toDoubleOrNull()
                        val lng = longitude.toDoubleOrNull()

                        val task = Task(
                            title = title,
                            description = description,
                            locationName = locationName,
                            latitude = lat,
                            longitude = lng,
                            dueDate = selectedDate,
                            type = selectedType
                        )
                        onConfirm(task)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
