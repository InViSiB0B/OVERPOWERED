package com.example.overpowered.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.overpowered.today.Task
import com.example.overpowered.data.RecurrenceType

@Composable
fun TaskItem(
    task: Task,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .combinedClickable(
                onClick = { /* No action on regular click */ },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                }
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (task.dueDate != null) {
                DateBadge(task.dueDate, modifier = Modifier.size(56.dp))
            } else {
                Box(
                    modifier = Modifier.size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Title with recurring indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Show repeat icon for recurring tasks
                    if (task.isRecurring) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Recurring",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF667EEA)
                        )
                    }
                }

                // Show recurrence type
                if (task.isRecurring && task.recurrenceType != null) {
                    val type = RecurrenceType.fromValue(task.recurrenceType)
                    Text(
                        text = "Repeats ${type?.displayName ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF667EEA)
                    )
                }

                task.description?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (task.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        task.tags.forEach { tag ->
                            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondary) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35F))
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Complete Task", tint = MaterialTheme.colorScheme.onTertiary)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}
