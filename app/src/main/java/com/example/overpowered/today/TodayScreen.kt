package com.example.overpowered.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.overpowered.today.Task
import com.example.overpowered.today.components.RewardDialog
import com.example.overpowered.today.components.TaskInputCard
import com.example.overpowered.today.components.TaskList
import com.example.overpowered.today.components.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    tasks: List<Task> = emptyList(),
    onAddTask: (String, String?, List<String>, Long?) -> Unit = { _, _, _, _ -> },
    onCompleteTask: (Task) -> Unit = { _ -> },
    onDeleteTask: (Task) -> Unit = { _ -> }
) {
    // Local UI state
    var isTaskInputVisible by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var taskTags by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }

    var showRewardDialog by remember { mutableStateOf(false) }
    var completedTaskTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Header card with toggle + input
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TaskDateAssistChip(
                        dueDate = dueDate,
                        onClick = { /* open date picker inside the input card */ }
                    )

                    Text(
                        text = if (isTaskInputVisible) "Add New Task" else "Create a Task",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { isTaskInputVisible = !isTaskInputVisible }
                    ) {
                        Icon(
                            imageVector = if (isTaskInputVisible) Icons.Filled.Delete else Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isTaskInputVisible,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    TaskInputCard(
                        title = taskTitle,
                        onTitleChange = { taskTitle = it },
                        description = taskDescription,
                        onDescriptionChange = { taskDescription = it },
                        tags = taskTags,
                        onTagsChange = { taskTags = it },
                        dueDate = dueDate,
                        onDueDateChange = { dueDate = it },
                        onSubmit = {
                            if (taskTitle.isNotBlank()) {
                                val tags = taskTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                onAddTask(taskTitle, taskDescription.takeIf { it.isNotBlank() }, tags, dueDate)

                                // reset
                                taskTitle = ""
                                taskDescription = ""
                                taskTags = ""
                                dueDate = null
                                isTaskInputVisible = false
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (tasks.isEmpty()) {
            Text(
                text = "No tasks yet. Add some!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Your Tasks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
            )
            TaskList(
                tasks = tasks,
                onComplete = { task ->
                    completedTaskTitle = task.title
                    onCompleteTask(task)
                    showRewardDialog = true
                },
                onDelete = onDeleteTask
            )
        }
    }

    if (showRewardDialog) {
        RewardDialog(
            taskTitle = completedTaskTitle,
            experienceReward = 10,
            moneyReward = 10,
            onDismiss = { showRewardDialog = false }
        )
    }
}

/** Little helper so the header shows current due date or "Set due date" */
@Composable
private fun TaskDateAssistChip(dueDate: Long?, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                if (dueDate != null) formatDate(dueDate)
                else "Set due date"
            )
        }
    )
}
