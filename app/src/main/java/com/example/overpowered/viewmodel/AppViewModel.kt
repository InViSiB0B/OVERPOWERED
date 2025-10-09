package com.example.overpowered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overpowered.data.*
import com.example.overpowered.navigation.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class AppViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // User profile state
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Tasks state - using SnapshotStateList for Compose integration
    private val _tasks = MutableStateFlow<List<FirebaseTask>>(emptyList())
    val tasks: StateFlow<List<FirebaseTask>> = _tasks.asStateFlow()

    // Friend requests state
    private val _pendingFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingFriendRequests: StateFlow<List<FriendRequest>> = _pendingFriendRequests.asStateFlow()

    // Friends state
    private val _friends = MutableStateFlow<List<Friendship>>(emptyList())
    val friends: StateFlow<List<Friendship>> = _friends.asStateFlow()

    // Exposed StateFlow for UI to observe
    val localTasks: StateFlow<List<Task>> = _tasks.map { firebaseTasks ->
        firebaseTasks.map { it.toLocalTask() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    // Conversion between local Task and FirebaseTask
    fun FirebaseTask.toLocalTask(): Task = Task(
        id = this.id.hashCode().toLong(),
        title = this.title,
        description = this.description
    )

    fun Task.toFirebaseTask(): FirebaseTask = FirebaseTask(
        title = this.title,
        description = this.description
    )

    init {
        initializeApp()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            _isLoading.value = true

            // Initialize authentication
            when (val authResult = repository.signInAnonymously()) {
                is FirebaseResult.Success -> {
                    loadUserData()
                }
                is FirebaseResult.Error -> {
                    _error.value = "Authentication failed: ${authResult.exception.message}"
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    private fun loadUserData() {
        // Observe profile changes
        viewModelScope.launch {
            repository.observeUserProfile().collect { result ->
                when (result) {
                    is FirebaseResult.Success -> {
                        _userProfile.value = result.data
                        _error.value = null
                    }
                    is FirebaseResult.Error -> {
                        _error.value = "Failed to load profile: ${result.exception.message}"
                    }
                    else -> {}
                }
            }
        }

        // Observe task changes
        viewModelScope.launch {
            repository.observeUserTasks().collect { result ->
                when (result) {
                    is FirebaseResult.Success -> {
                        _tasks.value = result.data
                        _error.value = null
                    }
                    is FirebaseResult.Error -> {
                        _error.value = "Failed to load tasks: ${result.exception.message}"
                    }
                    else -> {}
                }
            }
        }

        // Observe friend requests
        viewModelScope.launch {
            repository.observePendingFriendRequests().collect { result ->
                when (result) {
                    is FirebaseResult.Success -> {
                        _pendingFriendRequests.value = result.data
                    }
                    is FirebaseResult.Error -> {
                        // Silently fail for friend requests
                    }
                    else -> {}
                }
            }
        }

        // Observe friends list
        viewModelScope.launch {
            repository.observeFriends().collect { result ->
                when (result) {
                    is FirebaseResult.Success -> {
                        _friends.value = result.data
                    }
                    is FirebaseResult.Error -> {
                        // Silently fail for friends list
                    }
                    else -> {}
                }
            }
        }
    }

    // Profile operations
    fun updatePlayerName(name: String) {
        viewModelScope.launch {
            val updatedProfile = _userProfile.value.copy(playerName = name)
            when (val result = repository.saveUserProfile(updatedProfile)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to update name: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val uploadResult = repository.uploadProfileImage(imageUri)) {
                is FirebaseResult.Success -> {
                    val updatedProfile = _userProfile.value.copy(profileImageUrl = uploadResult.data)
                    repository.saveUserProfile(updatedProfile)
                }
                is FirebaseResult.Error -> {
                    _error.value = "Failed to upload image: ${uploadResult.exception.message}"
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    fun updateCustomization(
        selectedFrame: String? = null,
        selectedTitle: String? = null,
        selectedTheme: String? = null
    ) {
        viewModelScope.launch {
            val updatedProfile = _userProfile.value.copy(
                selectedFrame = selectedFrame ?: _userProfile.value.selectedFrame,
                selectedTitle = selectedTitle ?: _userProfile.value.selectedTitle,
                selectedTheme = selectedTheme ?: _userProfile.value.selectedTheme
            )
            repository.saveUserProfile(updatedProfile)
        }
    }

    fun purchaseItem(itemId: String, price: Int) {
        viewModelScope.launch {
            val currentProfile = _userProfile.value
            if (currentProfile.playerMoney >= price) {
                val updatedProfile = currentProfile.copy(
                    playerMoney = currentProfile.playerMoney - price,
                    purchasedItems = currentProfile.purchasedItems + itemId
                )
                when (val result = repository.saveUserProfile(updatedProfile)) {
                    is FirebaseResult.Error -> {
                        _error.value = "Failed to purchase item: ${result.exception.message}"
                    }
                    else -> {}
                }
            } else {
                _error.value = "Insufficient funds"
            }
        }
    }

    // Task operations
    fun addTask(title: String, description: String? = null) {
        viewModelScope.launch {
            val task = FirebaseTask(
                title = title,
                description = description
            )

            when (val result = repository.addTask(task)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to add task: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    fun completeTask(taskId: String, experienceReward: Int = 10, moneyReward: Int = 10) {
        viewModelScope.launch {
            // Optimistically update local state immediately (works offline)
            val currentProfile = _userProfile.value
            val newExperience = currentProfile.playerExperience + experienceReward
            val newMoney = currentProfile.playerMoney + moneyReward
            val newLevel = (newExperience / 100) + 1

            val updatedProfile = currentProfile.copy(
                playerExperience = newExperience,
                playerMoney = newMoney,
                playerLevel = newLevel
            )

            // Update local state first (instant feedback, works offline)
            _userProfile.value = updatedProfile

            // Complete the task in Firebase (queued if offline, synced when online)
            repository.completeTask(taskId)

            // Update profile in Firebase (queued if offline, synced when online)
            repository.saveUserProfile(updatedProfile)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteTask(taskId)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to delete task: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    // Utility functions for UI
    fun clearError() {
        _error.value = null
    }

    fun findFirebaseTaskById(localTaskId: Long): FirebaseTask? {
        return _tasks.value.find { it.id.hashCode().toLong() == localTaskId }
    }

    // Friend operations
    fun sendFriendRequest(playerName: String) {
        viewModelScope.launch {
            when (val result = repository.sendFriendRequest(playerName)) {
                is FirebaseResult.Success -> {
                    _error.value = "Friend request sent!"
                }
                is FirebaseResult.Error -> {
                    _error.value = result.exception.message
                }
                else -> {}
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            when (val result = repository.acceptFriendRequest(request.id, request)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to accept friend request: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    fun ignoreFriendRequest(requestId: String) {
        viewModelScope.launch {
            repository.ignoreFriendRequest(requestId)
        }
    }

    // Computed properties for UI
    val playerName: String get() = _userProfile.value.playerName
    val playerMoney: Int get() = _userProfile.value.playerMoney
    val playerExperience: Int get() = _userProfile.value.playerExperience
    val playerLevel: Int get() = _userProfile.value.playerLevel
    val purchasedItems: Set<String> get() = _userProfile.value.purchasedItems.toSet()
    val selectedFrame: String? get() = _userProfile.value.selectedFrame
    val selectedTitle: String? get() = _userProfile.value.selectedTitle
    val selectedTheme: String? get() = _userProfile.value.selectedTheme
    val profileImageUrl: String? get() = _userProfile.value.profileImageUrl
    val friendRequestCount: Int get() = _pendingFriendRequests.value.size
}