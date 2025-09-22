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
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.zIndex


enum class Tab { Today, Rewards, Shop }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    var tab by remember { mutableStateOf(Tab.Today) }
    var showProfile by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var playerName by remember { mutableStateOf("Player Name") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var playerMoney by remember { mutableStateOf(100) } // Starting with $100 for testing
    var purchasedItems by remember { mutableStateOf(setOf<String>()) }
    var selectedFrame by remember { mutableStateOf<String?>(null) }
    var selectedTitle by remember { mutableStateOf<String?>(null) }
    var selectedTheme by remember { mutableStateOf<String?>(null) }


    Scaffold(
        topBar = {
            TopStatusBar(
                currentTab = tab,
                showProfile = showProfile,
                showEditProfile = showEditProfile,
                profileImageUri = profileImageUri,
                playerMoney = playerMoney,
                onProfileClick = { showProfile = !showProfile }
            )
        },

        bottomBar = {
            // A container to layer the bar and the FAB
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()

            ) {
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    containerColor = Color(0xFF4A5568),
                    contentColor = Color.White,
                    tonalElevation = 4.dp,
                    windowInsets = BottomAppBarDefaults.windowInsets, // use default window insets
                ) {
                    // Left action
                    Spacer(Modifier.weight(.1f))
                    IconButton(
                        onClick = {
                            if (showProfile || showEditProfile) { showProfile = false; showEditProfile = false }
                            tab = Tab.Rewards
                        }
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "Rewards", modifier = Modifier.size(28.dp))
                    }

                    Spacer(Modifier.weight(.5f)) // make room for center FAB

                    // Right action
                    IconButton(
                        onClick = {
                            if (showProfile || showEditProfile) { showProfile = false; showEditProfile = false }
                            tab = Tab.Shop
                        }
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Shop", modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.weight(.1f))
                }

                // The oversized center FAB
                LargeFloatingActionButton(
                    onClick = {
                        if (showProfile || showEditProfile) { showProfile = false; showEditProfile = false }
                        tab = Tab.Today
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)    // center horizontally relative to the bar
                        .offset(y = (-32).dp)         // negative to overflow above the bar
                        .size(96.dp)           // make it big
                        .zIndex(1f),        // ensure it draws above the bar
                    shape = CircleShape,
                    containerColor = if (tab == Tab.Today && !showProfile)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Today",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFFF7FAFC)
        ) {
            if (showEditProfile) {
                EditProfileScreen(
                    playerName = playerName,
                    profileImageUri = profileImageUri,
                    purchasedItems = purchasedItems,
                    selectedFrame = selectedFrame,
                    selectedTitle = selectedTitle,
                    selectedTheme = selectedTheme,
                    onPlayerNameChange = { playerName = it},
                    onProfileImageChange = { profileImageUri = it },
                    onFrameSelect = { selectedFrame = it },
                    onTitleSelect = { selectedTitle = it },
                    onThemeSelect = { selectedTheme = it },
                    onBackClick = { showEditProfile = false }
                )
            } else if (showProfile) {
                ProfileScreen(
                    playerName = playerName,
                    profileImageUri = profileImageUri,
                    playerMoney = playerMoney,
                    onEditClick = { showEditProfile = true })
            } else {
                when (tab) {
                    Tab.Today -> TodayScreen()
                    Tab.Rewards -> RewardsScreen()
                    Tab.Shop -> ShopScreen(
                        playerMoney = playerMoney,
                        purchasedItems = purchasedItems,
                        onPurchase = { price, itemId ->
                            playerMoney -= price
                            purchasedItems = purchasedItems + itemId
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TopStatusBar(
    currentTab: Tab,
    showProfile: Boolean,
    showEditProfile: Boolean,
    profileImageUri: Uri?,
    playerMoney: Int,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF667EEA))
    ) {
        Box(Modifier.fillMaxSize()) {
            // centered title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "OVERPOWERED",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (showEditProfile) {
                        "Edit Profile"
                    } else if (showProfile) {
                        "Profile"
                    } else {
                        when (currentTab) {
                            Tab.Today -> "Today"
                            Tab.Rewards -> "Rewards"
                            Tab.Shop -> "Shop"
                        }
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, top = 32.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
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
                                if (profileImageUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(profileImageUri),
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
                    }
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
                            text = "999",
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
        TopStatusBar(
            currentTab = Tab.Today,
            showProfile = false,
            showEditProfile = false,
            profileImageUri = null,
            playerMoney = 0,
            onProfileClick = {})
    }
}