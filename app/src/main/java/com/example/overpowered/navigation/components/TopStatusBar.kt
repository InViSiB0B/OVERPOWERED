package com.example.overpowered.navigation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import com.example.overpowered.ui.theme.AppIcons


@Composable
fun TopStatusBar(
    currentTab: Tab,
    showProfile: Boolean,
    showEditProfile: Boolean,
    profileImageUrl: String?,
    playerMoney: Int,
    playerExperience: Int,
    notificationCount: Int,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val playerLevel = (playerExperience / 100) + 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Spacer(Modifier.height(48.dp))

                Image(
                    painter = painterResource(AppIcons.Logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(96.dp),                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(4.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, top = 32.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Profile
                    Card(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        onClick = onProfileClick
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUrl != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(profileImageUrl),
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    NotificationButton(
                        notificationCount = notificationCount,
                        onClick = onNotificationClick
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LVL
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LVL",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playerLevel.toString(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // $
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playerMoney.toString(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationButton(
    notificationCount: Int,
    onClick: () -> Unit
) {
    Box {
        Card(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.2f)
            ),
            onClick = onClick
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (notificationCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                containerColor = Color(0xFFE74C3C)
            ) {
                Text(
                    text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
