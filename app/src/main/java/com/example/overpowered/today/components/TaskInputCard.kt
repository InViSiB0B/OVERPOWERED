package com.example.overpowered.today.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskInputCard(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    dueDate: Long?,
    onDueDateChange: (Long?) -> Unit,
    onSubmit: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(Modifier.padding(top = 32.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Task Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Task Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tags,
            onValueChange = onTagsChange,
            label = { Text("Tags (comma-separated, optional)") },
            placeholder = { Text("work, urgent, project") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            DateSelectorButton(
                dueDate = dueDate,
                onClick = { showDatePicker = true }
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onSubmit,
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Add Task") }
        }
    }

    if (showDatePicker) {
        val todayStart = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: todayStart,
            yearRange = (2023..2030),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayStart
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            val normalized = normalizePickerMillisToLocalMidnight(picked)
                            onDueDateChange(normalized)   // save local-midnight millis
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("Set") }
            },
            dismissButton = { TextButton({ showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DateSelectorButton(
    dueDate: Long?,
    onClick: () -> Unit
) {
    val label = if (dueDate != null)
        com.example.overpowered.today.components.formatDate(dueDate)
    else
        "Set due date"

    if (dueDate != null) {
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        // When not set: subtle outlined style
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

