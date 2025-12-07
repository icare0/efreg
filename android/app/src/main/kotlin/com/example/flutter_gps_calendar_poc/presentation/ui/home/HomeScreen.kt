package com.example.flutter_gps_calendar_poc.presentation.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flutter_gps_calendar_poc.presentation.ui.components.AddTaskDialog
import com.example.flutter_gps_calendar_poc.presentation.ui.components.EventItemCard
import com.example.flutter_gps_calendar_poc.presentation.ui.components.LocationCard
import com.example.flutter_gps_calendar_poc.presentation.ui.components.TaskItemCard

/**
 * Home screen that displays the daily schedule (calendar events and tasks).
 *
 * This recreates the home_screen.dart from the Flutter POC but with:
 * - Native Material 3 design
 * - Cleaner architecture (MVVM)
 * - Better performance (Jetpack Compose)
 * - Full geofencing and location integration
 * - Runtime permission handling
 *
 * Layout structure:
 * 1. Location indicator (GPS coordinates)
 * 2. Timeline section (Calendar events)
 * 3. Tasks section (To-do list)
 * 4. FAB for adding new tasks
 *
 * @param viewModel The HomeViewModel that provides the UI state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contextual Todo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task"
                )
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is HomeUIState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HomeUIState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is HomeUIState.Success -> {
                HomeContent(
                    state = state,
                    currentLocation = currentLocation,
                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Add task dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { task ->
                viewModel.addTask(task)
                showAddTaskDialog = false
            }
        )
    }
}

/**
 * Content for the home screen in success state.
 */
@Composable
private fun HomeContent(
    state: HomeUIState.Success,
    currentLocation: android.location.Location?,
    onToggleTask: (com.example.flutter_gps_calendar_poc.domain.model.Task) -> Unit,
    onDeleteTask: (com.example.flutter_gps_calendar_poc.domain.model.Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val schedule = state.schedule

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
    ) {
        // GPS Location indicator (connected to real location!)
        item {
            LocationCard(
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude
            )
        }

        // Timeline section (Calendar events)
        if (schedule.calendarEvents.isNotEmpty()) {
            stickyHeader {
                SectionHeader(
                    title = "Timeline",
                    subtitle = "${schedule.calendarEvents.size} events today"
                )
            }

            items(
                items = schedule.calendarEvents,
                key = { it.id }
            ) { event ->
                EventItemCard(event = event)
            }

            // Free slots info
            if (schedule.freeSlots.isNotEmpty()) {
                item {
                    FreeSlotsCard(
                        freeSlots = schedule.getFreeSlotsAtLeast(30)
                    )
                }
            }
        } else {
            item {
                SectionHeader(
                    title = "Timeline",
                    subtitle = "No events today"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tasks section
        stickyHeader {
            SectionHeader(
                title = "To Do",
                subtitle = "${schedule.activeTasks.size} active tasks"
            )
        }

        if (schedule.activeTasks.isNotEmpty()) {
            items(
                items = schedule.activeTasks,
                key = { it.id }
            ) { task ->
                TaskItemCard(
                    task = task,
                    onToggleComplete = onToggleTask,
                    onDelete = onDeleteTask
                )
            }
        } else {
            item {
                EmptyStateCard(message = "No active tasks. Tap + to add one!")
            }
        }

        // Completed tasks section
        if (schedule.completedTasks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            stickyHeader {
                SectionHeader(
                    title = "Completed",
                    subtitle = "${schedule.completedTasks.size} tasks"
                )
            }

            items(
                items = schedule.completedTasks,
                key = { it.id }
            ) { task ->
                TaskItemCard(
                    task = task,
                    onToggleComplete = onToggleTask,
                    onDelete = onDeleteTask
                )
            }
        }
    }
}

/**
 * Section header with sticky behavior.
 */
@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card showing free time slots.
 */
@Composable
private fun FreeSlotsCard(
    freeSlots: List<com.example.flutter_gps_calendar_poc.domain.model.FreeSlot>
) {
    if (freeSlots.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Free Time Slots",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${freeSlots.size} slots available (≥30 min)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Empty state card.
 */
@Composable
private fun EmptyStateCard(
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
