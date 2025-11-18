package com.example.overpowered.today.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.overpowered.today.Task


@Composable
fun TaskList(
    tasks: List<Task>,
    onComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(tasks, key = { it.id }) { task ->
            TaskItem(
                task = task,
                onComplete = { onComplete(task) },
                onDelete = { onDelete(task) },
                onEdit = { onEdit(task) }
            )
        }
    }
}
