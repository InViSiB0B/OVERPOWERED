package com.example.overpowered.today.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun formatDate(millis: Long): String {
    // Simple, locale-aware formatter
    val formatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy")
    }
    val date = Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return date.format(formatter)
}