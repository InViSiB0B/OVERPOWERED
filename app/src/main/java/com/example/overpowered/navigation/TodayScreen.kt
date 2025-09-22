package com.example.overpowered.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class to represent a task
data class Task(
    val id: Long = System.currentTimeMillis(), // Simple unique ID
    val title: String,
    val description: String? = null
)

// Rewards data class
data class TaskReward(
    val experiencePoints: Int = 10,
    val money: Int = 10
)

@Composable
fun TodayScreen(onTaskComplete: (Int, Int) -> Unit = { _, _ -> }) {
    var isTaskInputVisible by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    val tasksList = remember { mutableStateListOf<Task>() }

    // Reward dialog state
    var showRewardDialog by remember { mutableStateOf(false) }
    var completedTaskTitle by remember { mutableStateOf("") }
    val currentReward = TaskReward() // 10 XP, 10 Money for now

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF4A5568),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Task Input Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isTaskInputVisible) "Add New Task" else "Create a Task",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(
                        onClick = { isTaskInputVisible = !isTaskInputVisible },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = if (isTaskInputVisible) Icons.Filled.Delete else Icons.Filled.Add,
                            contentDescription = if (isTaskInputVisible) "Hide Task Input" else "Show Task Input",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isTaskInputVisible,
                    enter = expandVertically(animationSpec = tween(durationMillis = 300)),
                    exit = shrinkVertically(animationSpec = tween(durationMillis = 300))
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Task Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = taskDescription,
                            onValueChange = { taskDescription = it },
                            label = { Text("Task Description (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (taskTitle.isNotBlank()) {
                                    tasksList.add(
                                        Task(
                                            title = taskTitle,
                                            description = taskDescription.takeIf { it.isNotBlank() }
                                        )
                                    )
                                    taskTitle = "" // Clear input
                                    taskDescription = "" // Clear input
                                    isTaskInputVisible = false // Collapse the input section
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = taskTitle.isNotBlank() // Enable button only if title is not empty
                        ) {
                            Text("Add Task")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Displaying the list of tasks
        if (tasksList.isEmpty()) {
            Text(
                text = "No tasks yet. Add some!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        } else {
            Text(
                text = "Your Tasks",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(tasksList, key = { task -> task.id }) { task ->
                    TaskItem(
                        task = task,
                        onComplete = {
                            completedTaskTitle = task.title
                            tasksList.remove(task)
                            showRewardDialog = true
                            onTaskComplete(currentReward.experiencePoints, currentReward.money)
                        },
                        onDelete = { tasksList.remove(task) }
                    )
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }
        }
    }

    // Reward Dialog
    if (showRewardDialog) {
        RewardDialog(
            taskTitle = completedTaskTitle,
            experienceReward = currentReward.experiencePoints,
            moneyReward = currentReward.money,
            onDismiss = { showRewardDialog = false }
        )
    }
}

@Composable
fun TaskItem(task: Task, onComplete: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleSmall)
                task.description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Complete button
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Complete Task",
                        tint = Color(0xFF48BB78)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun RewardDialog(
    taskTitle: String,
    experienceReward: Int,
    moneyReward: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎉",
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Task Completed!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A5568),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Great job completing:",
                    fontSize = 16.sp,
                    color = Color(0xFF718096),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$taskTitle\"",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4A5568),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Rewards section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Rewards Earned:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A5568)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Experience reward
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+$experienceReward",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFED8936)
                                )
                                Text(
                                    text = "Experience",
                                    fontSize = 12.sp,
                                    color = Color(0xFF718096)
                                )
                            }

                            // Money reward
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+$$moneyReward",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF48BB78)
                                )
                                Text(
                                    text = "Money",
                                    fontSize = 12.sp,
                                    color = Color(0xFF718096)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Awesome!",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}