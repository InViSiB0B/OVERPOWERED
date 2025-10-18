package com.example.overpowered.navigation

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.zIndex
import com.example.overpowered.viewmodel.AppViewModel

enum class Tab { Today, Progress, Shop }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    viewModel: AppViewModel = AppViewModel()
) {
    var tab by remember { mutableStateOf(Tab.Today) }
    var showProfile by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    // Observe ViewModel state
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val tasks by viewModel.localTasks.collectAsState()
    val pendingFriendRequests by viewModel.pendingFriendRequests.collectAsState()

    // Show error messages
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopStatusBar(
                currentTab = tab,
                showProfile = showProfile,
                showEditProfile = showEditProfile,
                profileImageUrl = userProfile.profileImageUrl,
                playerMoney = userProfile.playerMoney ?: 100,
                playerExperience = userProfile.playerExperience ?: 0,
                notificationCount = pendingFriendRequests.size,
                onProfileClick = { showProfile = !showProfile },
                onNotificationClick = { showNotifications = true }
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
                    windowInsets = BottomAppBarDefaults.windowInsets,
                ) {
                    // Left action
                    Spacer(Modifier.weight(.1f))
                    IconButton(
                        onClick = {
                            if (showProfile || showEditProfile) {
                                showProfile = false
                                showEditProfile = false
                            }
                            tab = Tab.Progress
                        }
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = "Progress", modifier = Modifier.size(28.dp))
                    }

                    Spacer(Modifier.weight(.5f)) // make room for center FAB

                    // Right action
                    IconButton(
                        onClick = {
                            if (showProfile || showEditProfile) {
                                showProfile = false
                                showEditProfile = false
                            }
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
                        if (showProfile || showEditProfile) {
                            showProfile = false
                            showEditProfile = false
                        }
                        tab = Tab.Today
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-32).dp)
                        .size(96.dp)
                        .zIndex(1f),
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
            if (isLoading) {
                // Show loading screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF667EEA))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading your data...",
                            color = Color(0xFF4A5568)
                        )
                    }
                }
            } else if (showEditProfile) {
                EditProfileScreen(
                    playerName = userProfile.playerName ?: "Player Name",
                    profileImageUrl = userProfile.profileImageUrl,
                    purchasedItems = userProfile.purchasedItems?.toSet() ?: emptySet(),
                    selectedFrame = userProfile.selectedFrame,
                    selectedTitle = userProfile.selectedTitle,
                    selectedTheme = userProfile.selectedTheme,
                    onPlayerNameChange = { viewModel.updatePlayerName(it) },
                    onProfileImageChange = { uri ->
                        uri?.let { viewModel.uploadProfileImage(it) }
                    },
                    onFrameSelect = { viewModel.updateCustomization(selectedFrame = it) },
                    onTitleSelect = { viewModel.updateCustomization(selectedTitle = it) },
                    onThemeSelect = { viewModel.updateCustomization(selectedTheme = it) },
                    onBackClick = { showEditProfile = false }
                )
            } else if (showProfile) {
                ProfileScreen(
                    playerName = userProfile.playerName ?: "Player Name",
                    profileImageUrl = userProfile.profileImageUrl,
                    playerMoney = userProfile.playerMoney ?: 100,
                    playerExperience = userProfile.playerExperience ?: 0,
                    friends = viewModel.friends.collectAsState().value,
                    onEditClick = { showEditProfile = true },
                    onSendFriendRequest = { playerName ->
                        viewModel.sendFriendRequest(playerName)
                    }
                )
            } else {
                when (tab) {
                    Tab.Today -> TodayScreen(
                        tasks = tasks,
                        onAddTask = { title, description, tags ->
                            viewModel.addTask(title, description, tags)
                        },
                        onCompleteTask = { task ->
                            // Find the Firebase task IDs
                            val firebaseTask = viewModel.findFirebaseTaskById(task.id)
                            firebaseTask?.let {
                                viewModel.completeTask(it.id, experienceReward = 10, moneyReward = 10)
                            }
                        },
                        onDeleteTask = { task ->
                            val firebaseTask = viewModel.findFirebaseTaskById(task.id)
                            firebaseTask?.let {
                                viewModel.deleteTask(it.id)
                            }
                        }
                    )
                    Tab.Progress -> ProgressScreen(viewModel = viewModel)
                    Tab.Shop -> ShopScreen(
                        playerMoney = userProfile.playerMoney ?: 100,
                        purchasedItems = userProfile.purchasedItems?.toSet() ?: emptySet(),
                        onPurchase = { price, itemId ->
                            viewModel.purchaseItem(itemId, price)
                        }
                    )
                }
            }
        }
    }
    if (showNotifications) {
        FriendRequestsDialog(
            friendRequests = pendingFriendRequests,
            onAccept = { request ->
                viewModel.acceptFriendRequest(request)
            },
            onIgnore = { requestId ->
                viewModel.ignoreFriendRequest(requestId)
            },
            onDismiss = { showNotifications = false }
        )
    }
}

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
    // Calculate player level based on experience
    val playerLevel = (playerExperience / 100) + 1

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
                            Tab.Progress -> "Progress"
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side - Profile picture
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

                    // Right side - notification bell
                    NotificationButton(
                        notificationCount = notificationCount,
                        onClick = onNotificationClick
                    )
                }

                // Bottom level - LVL and Money
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left stat - LVL
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

        // Badge for notification count
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