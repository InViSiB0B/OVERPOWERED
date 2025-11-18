package com.example.overpowered.navigation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.background
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
                    selectedFrame = userProfile.selectedFrame,
                    playerMoney = userProfile.playerMoney ?: 100,
                    playerExperience = userProfile.playerExperience ?: 0,
                    notificationCount = pendingFriendRequests.size,
                    onProfileClick = { showProfile = !showProfile },
                    onNotificationClick = { showNotifications = true }
                )
            },
            bottomBar = {
                MainBottomBar(
                    currentTab = tab,
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
                color = MaterialTheme.colorScheme.background
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
                                onUpdateTask = { task, title, desc, tags, dueDate, isRecurring, recurrenceType ->
                                    viewModel.updateTask(task, title, desc, tags, dueDate, isRecurring, recurrenceType)
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
    currentTab: Tab,
    onSelectProgress: () -> Unit,
    onSelectShop: () -> Unit
) {
    val bottomBarBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    val isProgressSelected = currentTab == Tab.Progress
    val isShopSelected = currentTab == Tab.Shop

    val unselectedBrush = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
            Color.Transparent
        )
    )

    val selectedBrush = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
            Color.Transparent
        )
    )


    BottomAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(bottomBarBrush),
        tonalElevation = 0.dp, // We're using a gradient, so explicit elevation is less needed
        containerColor = Color.Transparent, // Color is handled by the background modifier
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Spacer(Modifier.weight(0.3f))

        IconButton(
            onClick = onSelectProgress,
            modifier = Modifier
                .size(72.dp)
                .offset(y = (-6).dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (isProgressSelected) selectedBrush else unselectedBrush),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(AppIcons.Progress),
                    contentDescription = "Progress",
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(Modifier.weight(1.75f)) // space for center task button

        IconButton(
            onClick = onSelectShop,
            modifier = Modifier
                .size(72.dp)
                .offset(y = (-6).dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (isShopSelected) selectedBrush else unselectedBrush),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(AppIcons.Shop),
                    contentDescription = "Shop",
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            }
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
            modifier = Modifier.size(90.dp), // A circular shape looks better with equal size
            color = if (isSelected)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            shadowElevation = 12.dp,
            // Add a subtle border for more definition
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
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
