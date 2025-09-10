package com.example.overpowered.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overpowered.ui.theme.OVERPOWEREDTheme

enum class Tab { Today, Rewards, Shop }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    var tab by remember { mutableStateOf(Tab.Today) }
    var showProfile by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopStatusBar(
                currentTab = tab,
                showProfile = showProfile,
                showEditProfile = showEditProfile,
                onProfileClick = { showProfile = !showProfile }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    if (showProfile || showEditProfile) {
                        showProfile = false
                        showEditProfile = false
                        tab = Tab.Today
                    } else {
                        tab = Tab.Today
                    }
                },
                containerColor = if (tab == Tab.Today && !showProfile)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = "Tasks",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFF4A5568),
                actions = {
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            if (showProfile || showEditProfile) {
                                showProfile = false
                                showEditProfile = false
                                tab = Tab.Rewards
                            } else {
                                tab = Tab.Rewards
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Rewards",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.weight(2f))
                    IconButton(
                        onClick = {
                            if (showProfile || showEditProfile) {
                                showProfile = false
                                showEditProfile = false
                                tab = Tab.Shop
                            } else {
                                tab = Tab.Shop
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "Shop",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFFF7FAFC)
        ) {
            if (showEditProfile) {
                EditProfileScreen(onBackClick = { showEditProfile = false })
            } else if (showProfile) {
                ProfileScreen(onEditClick = { showEditProfile = true })
            } else {
                when (tab) {
                    Tab.Today -> TodayScreen()
                    Tab.Rewards -> RewardsScreen()
                    Tab.Shop -> ShopScreen()
                }
            }
        }
    }
}

@Composable
fun TopStatusBar(currentTab: Tab, showProfile: Boolean, showEditProfile: Boolean, onProfileClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF667EEA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top level - Profile Picture, OVERPOWERED Logo, Current Tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left - Profile Picture
                Card(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    onClick = onProfileClick
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Center - OVERPOWERED Logo
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OVERPOWERED",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Right - Current Tab
                Text(
                    text = if (showProfile) {
                        "Profile"
                    } else {
                        when(currentTab) {
                            Tab.Today -> "Today"
                            Tab.Rewards -> "Rewards"
                            Tab.Shop -> "Shop"
                        }
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom level - EXP and Money
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left stat - EXP
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXP",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "playerExperiencePoints",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Right stat - Money
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "playerMoney",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainNavigationPreview() {
    OVERPOWEREDTheme {
        MainNavigation()
    }
}

@Preview(showBackground = true)
@Composable
fun TopStatusBarPreview() {
    OVERPOWEREDTheme {
        TopStatusBar(currentTab = Tab.Today, showProfile = false, showEditProfile = false, onProfileClick = {})
    }
}