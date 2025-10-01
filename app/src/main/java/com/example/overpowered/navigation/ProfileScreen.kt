package com.example.overpowered.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image

@Composable
fun ProfileScreen(
    playerName: String,
    profileImageUrl: String?,
    playerMoney: Int,
    playerExperience: Int,
    onEditClick: () -> Unit
) {
    // Calculate player level based on experience (super placeholder right now: level = experience / 100 + 1)
    val playerLevel = (playerExperience / 100) + 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Edit button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onEditClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit Profile",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Profile picture
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF667EEA))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUrl),
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

        // User info
        Text(
            text = playerName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A5568)
        )

        // Stats section
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
                    text = "Stats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A5568)
                )

                // Level Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Player Level",
                        fontSize = 16.sp,
                        color = Color(0xFF4A5568)
                    )
                    Text(
                        text = playerLevel.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF667EEA)
                    )
                }

                // Money Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Money",
                        fontSize = 16.sp,
                        color = Color(0xFF4A5568)
                    )
                    Text(
                        text = "$$playerMoney",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF48BB78)
                    )
                }

                // EXP Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Experience Points",
                        fontSize = 16.sp,
                        color = Color(0xFF4A5568)
                    )
                    Text(
                        text = playerExperience.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFED8936)
                    )
                }
            }
        }
    }
}