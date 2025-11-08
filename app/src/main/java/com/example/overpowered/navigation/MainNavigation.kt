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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import com.example.overpowered.auth.PhoneAuthScreen
import com.example.overpowered.auth.VerificationCodeScreen
import com.example.overpowered.viewmodel.PhoneAuthState
import com.example.overpowered.onboarding.OnboardingScreen
import com.example.overpowered.profile.ProfileScreen
import com.example.overpowered.profile.components.EditProfileScreen
import com.example.overpowered.profile.components.FramedProfilePicture
import com.example.overpowered.profile.components.FriendRequestsDialog
import com.example.overpowered.progress.ProgressScreen
import com.example.overpowered.shop.ShopScreen
import com.example.overpowered.today.TodayScreen
import com.example.overpowered.viewmodel.AppViewModel


enum class Tab { Today, Progress, Shop }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val viewModel: AppViewModel = viewModel()

    val phoneAuthState by viewModel.phoneAuthState.collectAsState()
    val isOnboarded by viewModel.isOnboarded.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    var savedPhoneNumber by remember { mutableStateOf("") }

    // Handle different authentication states
    when (phoneAuthState) {
        is PhoneAuthState.Initial, is PhoneAuthState.SendingCode -> {
            if (!isLoading) {
                PhoneAuthScreen(
                    onVerificationCodeSent = { _, phoneNumber ->
                        savedPhoneNumber = phoneNumber
                        activity?.let { viewModel.startPhoneAuth(phoneNumber, it) }
                    },
                    onError = { message ->
                        // Show error snackbar or toast
                    }
                )
            } else {
                LoadingScreen()
            }
        }

        is PhoneAuthState.CodeSent -> {
            val state = phoneAuthState as PhoneAuthState.CodeSent
            savedPhoneNumber = state.phoneNumber
            VerificationCodeScreen(
                phoneNumber = state.phoneNumber,
                verificationId = state.verificationId,
                onVerificationComplete = { code ->
                    viewModel.verifyPhoneCode(state.verificationId, code, state.phoneNumber)
                },
                onResendCode = {
                    activity?.let { viewModel.startPhoneAuth(state.phoneNumber, it) }
                },
                onError = { message ->
                    // Show error
                }
            )
        }

        is PhoneAuthState.VerifyingCode -> {
            LoadingScreen(message = "Verifying code...")
        }

        is PhoneAuthState.Success -> {
            // After successful auth, check onboarding
            if (!isOnboarded && !isLoading) {
                OnboardingScreen(
                    onComplete = { username ->
                        viewModel.completeOnboarding(
                            username = username,
                            phoneNumber = savedPhoneNumber
                        )
                    }
                )
            } else if (isOnboarded) {
                // Show main app
                MainAppContent(viewModel = viewModel)
            } else {
                LoadingScreen()
            }
        }

        is PhoneAuthState.Error -> {
            val errorMessage = (phoneAuthState as PhoneAuthState.Error).message
            // Show error screen with retry option
            ErrorScreen(
                message = errorMessage,
                onRetry = {
                    viewModel.resetPhoneAuthState()
                }
            )
        }
    }
}

// Loading screen composable
@Composable
private fun LoadingScreen(message: String = "Loading...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color(0xFF667EEA))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color(0xFF4A5568),
                fontSize = 16.sp
            )
        }
    }
}

// Error screen composable
@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAFC))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 72.sp
            )
            Text(
                text = "Something went wrong",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Try Again")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: AppViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(Tab.Today) }
    var showProfile by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    // Observe ViewModel state
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isOnboarded by viewModel.isOnboarded.collectAsState()
    val tasks by viewModel.localTasks.collectAsState()
    val longTermGoals by viewModel.longTermGoals.collectAsState()
    val pendingFriendRequests by viewModel.pendingFriendRequests.collectAsState()

    val isSelected = (tab == Tab.Today && !showProfile && !showEditProfile)

    // Show error messages
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            viewModel.clearError()
        }
    }

    if (!isOnboarded && !isLoading) {
        OnboardingScreen(
            onComplete = { username ->
                viewModel.completeOnboarding(
                    username = username,
                    phoneNumber = "" // Will be replaced with actual phone number from auth
                )
            }
        )
        return
    }

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
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.secondary,
                    contentColor = if (isSelected)
                        MaterialTheme.colorScheme.onSecondary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
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
            }
            else if (showEditProfile) {
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
                    userProfile = userProfile,
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
                        onAddTask = { title, description, tags, dueDate, isRecurring, recurrenceType ->
                            viewModel.addTask(title, description, tags, dueDate, isRecurring, recurrenceType)
                        },
                        onCompleteTask = { task ->
                            // Find the Firebase task IDss
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
    selectedFrame: String?,
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
                                FramedProfilePicture(
                                    profileImageUrl = profileImageUrl,
                                    frameId = selectedFrame, // Pass the selected frame ID
                                    size = 32.dp,
                                    modifier = Modifier.clickable { onProfileClick() }
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