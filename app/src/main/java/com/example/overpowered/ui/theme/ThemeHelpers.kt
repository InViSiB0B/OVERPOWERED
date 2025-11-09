package com.example.overpowered.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

@Composable
fun headerGradient(): Brush {
    val cs = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        listOf(cs.primary, cs.primary.copy(alpha = 0.85f))
    )
}

val ColorScheme.cardTint: androidx.compose.ui.graphics.Color
    get() = onSurface.copy(alpha = 0.06f)

val ColorScheme.subtleBorder: androidx.compose.ui.graphics.Color
    get() = outline.copy(alpha = 0.5f)
