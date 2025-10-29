package com.example.overpowered.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color(0xFFF7FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo/title
            Text(
                text = "OP",
                fontSize = 72.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to OVERPOWERED!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose your username to get started",
                fontSize = 16.sp,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Username input
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Username",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            if (it.length <= 20) { // Limit username length
                                username = it
                                errorMessage = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your username") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667EEA),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    Text(
                        text = "You'll be assigned a unique 4-digit number (e.g., ${username.ifBlank { "Player" }}#1234)",
                        fontSize = 12.sp,
                        color = Color(0xFF718096)
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            fontSize = 12.sp,
                            color = Color(0xFFE53E3E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = {
                    if (username.isBlank()) {
                        errorMessage = "Please enter a username"
                    } else if (username.length < 3) {
                        errorMessage = "Username must be at least 3 characters"
                    } else {
                        isLoading = true
                        onComplete(username)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}