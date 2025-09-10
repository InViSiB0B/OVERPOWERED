package com.example.overpowered.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RewardsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Rewards",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF4A5568)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your rewards will appear here",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}