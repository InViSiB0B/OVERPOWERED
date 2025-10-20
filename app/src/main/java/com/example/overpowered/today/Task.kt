package com.example.overpowered.today

data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val tags: List<String> = emptyList()
)