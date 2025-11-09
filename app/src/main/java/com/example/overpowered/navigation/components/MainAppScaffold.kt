package com.example.overpowered.navigation.components

import androidx.compose.foundation.content.MediaType.Companion.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.overpowered.onboarding.OnboardingScreen
import com.example.overpowered.profile.ProfileScreen
import com.example.overpowered.profile.components.EditProfileScreen
import com.example.overpowered.profile.components.FriendRequestsDialog
import com.example.overpowered.progress.ProgressScreen
import com.example.overpowered.shop.ShopScreen
import com.example.overpowered.today.TodayScreen
import com.example.overpowered.viewmodel.AppViewModel
import androidx.compose.ui.res.painterResource
import com.example.overpowered.ui.theme.AppIcons
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex


enum class Tab { Today, Progress, Shop }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: AppViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(Tab.Today) }
    var showProfile by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    // Observed state
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isOnboarded by viewModel.isOnboarded.collectAsState()
    val tasks by viewModel.localTasks.collectAsState()
    val pendingFriendRequests by viewModel.pendingFriendRequests.collectAsState()
    val friends by viewModel.friends.collectAsState()

    // Clear transient error
    LaunchedEffect(error) {
        if (error != null) viewModel.clearError()
    }

    // Safety: if something desyncs and onboarding not complete, route there
    if (!isOnboarded && !isLoading) {
        OnboardingScreen(
            onComplete = { username ->
                viewModel.completeOnboarding(
                    username = username,
                    phoneNumber = "" // TODO: wire actual phone from auth layer
                )
            }
        )
        return
    }

    val isTodaySelected = (tab == Tab.Today && !showProfile && !showEditProfile)

    Box(modifier = Modifier.fillMaxSize()) {
        // Core scaffold: top bar + standard bottom bar
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
                MainBottomBar(
                    onSelectProgress = {
                        showProfile = false
                        showEditProfile = false
                        tab = Tab.Progress
                    },
                    onSelectShop = {
                        showProfile = false
                        showEditProfile = false
                        tab = Tab.Shop
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
                when {
                    isLoading -> {
                        LoadingScreen(message = "Loading your data...")
                    }

                    showEditProfile -> {
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
                            onFrameSelect = {
                                viewModel.updateCustomization(selectedFrame = it)
                            },
                            onTitleSelect = {
                                viewModel.updateCustomization(selectedTitle = it)
                            },
                            onThemeSelect = {
                                viewModel.updateCustomization(selectedTheme = it)
                            },
                            onBackClick = { showEditProfile = false }
                        )
                    }

                    showProfile -> {
                        ProfileScreen(
                            userProfile = userProfile,
                            playerMoney = userProfile.playerMoney ?: 100,
                            playerExperience = userProfile.playerExperience ?: 0,
                            friends = friends,
                            onEditClick = { showEditProfile = true },
                            onSendFriendRequest = { playerName ->
                                viewModel.sendFriendRequest(playerName)
                            }
                        )
                    }

                    else -> {
                        when (tab) {
                            Tab.Today -> TodayScreen(
                                tasks = tasks,
                                onAddTask = { title, description, tags, dueDate, isRecurring, recurrenceType ->
                                    viewModel.addTask(
                                        title,
                                        description,
                                        tags,
                                        dueDate,
                                        isRecurring,
                                        recurrenceType
                                    )
                                },
                                onCompleteTask = { task ->
                                    val firebaseTask = viewModel.findFirebaseTaskById(task.id)
                                    firebaseTask?.let {
                                        viewModel.completeTask(
                                            it.id,
                                            experienceReward = 10,
                                            moneyReward = 10
                                        )
                                    }
                                },
                                onDeleteTask = { task ->
                                    val firebaseTask = viewModel.findFirebaseTaskById(task.id)
                                    firebaseTask?.let {
                                        viewModel.deleteTask(it.id)
                                    }
                                },
                                onDeleteSingleRecurring = { task ->
                                    val firebaseTask = viewModel.findFirebaseTaskById(task.id)
                                    firebaseTask?.let {
                                        viewModel.deleteSingleRecurringOccurrence(it.id)
                                    }
                                },
                                onDeleteAllRecurring = { recurrenceParentId ->
                                    viewModel.deleteAllRecurringInstances(recurrenceParentId)
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
        }

        // Big center Task button overlay (NOT part of bottomBar height)
        CenterTaskButton(
            isSelected = isTodaySelected,
            onClick = {
                showProfile = false
                showEditProfile = false
                tab = Tab.Today
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-32).dp) // controls how far it overlaps into content
        )
    }

    if (showNotifications) {
        FriendRequestsDialog(
            friendRequests = pendingFriendRequests,
            onAccept = { request -> viewModel.acceptFriendRequest(request) },
            onIgnore = { requestId -> viewModel.ignoreFriendRequest(requestId) },
            onDismiss = { showNotifications = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainBottomBar(
    onSelectProgress: () -> Unit,
    onSelectShop: () -> Unit
) {
    BottomAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp,
        // Tighten default padding a bit so it doesn’t feel chunky
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Spacer(Modifier.weight(0.3f))

        IconButton(
            onClick = onSelectProgress,
            modifier = Modifier
                .size(64.dp)
                .offset(y = (-6).dp)
        ) {
            Image(
                painter = painterResource(AppIcons.Progress),
                contentDescription = "Progress",
                modifier = Modifier.fillMaxSize(0.8f),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.weight(1.75f)) // space for center task button

        IconButton(
            onClick = onSelectShop,
            modifier = Modifier
                .size(64.dp)
                .offset(y = (-6).dp)
        ) {
            Image(
                painter = painterResource(AppIcons.Shop),
                contentDescription = "Shop",
                modifier = Modifier.fillMaxSize(0.8f),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.weight(0.3f))
    }
}

@Composable
private fun CenterTaskButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .zIndex(10f),
        contentAlignment = Alignment.Center
    ) {
        // Background platform
        Surface(
            onClick = onClick,
            modifier = Modifier
                .width(85.dp)
                .height(100.dp),
            color = if (isSelected)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 12.dp
        ) {}

        // Oversized icon that visually overflows into content & bar
        Image(
            painter = painterResource(AppIcons.Task),
            contentDescription = "Today",
            modifier = Modifier.size(140.dp),
            contentScale = ContentScale.Fit
        )
    }
}
