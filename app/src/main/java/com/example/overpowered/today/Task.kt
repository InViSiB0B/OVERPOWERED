package com.example.overpowered.today

data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val tags: List<String> = emptyList(),
    val isRecurring: Boolean = false,
    val recurrenceType: String? = null,
    val recurrenceParentId: String? = null
)