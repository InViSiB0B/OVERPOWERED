package com.example.overpowered.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onVerificationCodeSent: (String, String) -> Unit, // (verificationId, phoneNumber)
    onError: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+1") }
    var isLoading by remember { mutableStateOf(false) }

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
                text = "Enter your phone number to get started",
                fontSize = 16.sp,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Phone input card
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
                        text = "Phone Number",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Country code
                        OutlinedTextField(
                            value = countryCode,
                            onValueChange = { countryCode = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF667EEA),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        // Phone number
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("555-123-4567") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF667EEA),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    Text(
                        text = "We'll send you a verification code via SMS",
                        fontSize = 12.sp,
                        color = Color(0xFF718096)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = {
                    if (phoneNumber.isBlank()) {
                        onError("Please enter a phone number")
                    } else {
                        isLoading = true
                        val fullPhoneNumber = countryCode + phoneNumber.replace(Regex("[^0-9]"), "")
                        onVerificationCodeSent("", fullPhoneNumber) // verificationId will be set in callback
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
                        text = "Send Code",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationCodeScreen(
    phoneNumber: String,
    verificationId: String,
    onVerificationComplete: (String) -> Unit, // code
    onResendCode: () -> Unit,
    onError: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
            Text(
                text = "📱",
                fontSize = 72.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enter Verification Code",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We sent a code to $phoneNumber",
                fontSize = 16.sp,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Code input card
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
                        text = "Verification Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568)
                    )

                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            if (it.length <= 6) code = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("000000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667EEA),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    TextButton(
                        onClick = onResendCode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Didn't receive code? Resend",
                            color = Color(0xFF667EEA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Verify button
            Button(
                onClick = {
                    if (code.length != 6) {
                        onError("Please enter a 6-digit code")
                    } else {
                        isLoading = true
                        onVerificationComplete(code)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && code.length == 6,
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
                        text = "Verify",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}