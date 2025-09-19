package com.example.overpowered.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun EditProfileScreen(
    playerName: String,
    profileImageUri: Uri?,
    onPlayerNameChange: (String) -> Unit,
    onProfileImageChange: (Uri?) -> Unit,
    onBackClick: () -> Unit
) {
    val (tempName, setTempName) = remember { mutableStateOf(playerName) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onProfileImageChange(it) }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Back button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back to Profile",
                    tint = Color(0xFF4A5568),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Picture (editable)
        Card(
            modifier = Modifier
                .size(120.dp)
                .clickable { imagePickerLauncher.launch("image/*") },
            shape = RoundedCornerShape(60.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF667EEA))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = profileImageUri),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(60.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }

        // Change Photo button
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF667EEA)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Change Photo",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Edit form section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A5568)
                )

                // Player Name field
                Column{
                    Text(
                        text = "Player Name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = setTempName,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667EEA),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Save button
            Button(
                onClick = {
                    onPlayerNameChange(tempName)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF48BB78)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Save Changes",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}