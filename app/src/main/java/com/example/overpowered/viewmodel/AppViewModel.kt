package com.example.overpowered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overpowered.data.*
import com.example.overpowered.today.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.net.Uri
import kotlinx.coroutines.tasks.await
import com.example.overpowered.progress.LeaderboardTimeframe
import com.example.overpowered.progress.LeaderboardRankingType
import com.example.overpowered.progress.LeaderboardEntry

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

    private val _completedTasks = MutableStateFlow<List<FirebaseTask>>(emptyList())
    val completedTasks: StateFlow<List<FirebaseTask>> = _completedTasks.asStateFlow()

    // Friend requests state
    private val _pendingFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingFriendRequests: StateFlow<List<FriendRequest>> = _pendingFriendRequests.asStateFlow()

    // Friends state
    private val _friends = MutableStateFlow<List<Friendship>>(emptyList())
    val friends: StateFlow<List<Friendship>> = _friends.asStateFlow()

    // Leaderboard state
    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()

    private val _isLoadingLeaderboard = MutableStateFlow(false)
    val isLoadingLeaderboard: StateFlow<Boolean> = _isLoadingLeaderboard.asStateFlow()

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
        description = this.description,
        tags = this.tags,
        dueDate = this.dueDate
    )

    fun Task.toFirebaseTask(): FirebaseTask = FirebaseTask(
        title = this.title,
        description = this.description,
        tags = this.tags,
        dueDate = this.dueDate
    )

    init {
        initializeApp()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("AppViewModel", "Initializing app...")

            // Initialize authentication
            when (val authResult = repository.signInAnonymously()) {
                is FirebaseResult.Success -> {
                    android.util.Log.d("AppViewModel", "Auth successful! UserId: ${authResult.data}")

                    // Initialize task tracking for existing users
                    repository.initializeTaskTracking()

                    loadUserData()
                }
                is FirebaseResult.Error -> {
                    android.util.Log.e("AppViewModel", "Auth failed: ${authResult.exception.message}")
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

        // Observe completed tasks
        viewModelScope.launch {
            repository.observeCompletedTasks().collect { result ->
                when (result) {
                    is FirebaseResult.Success -> {
                        _completedTasks.value = result.data
                    }
                    is FirebaseResult.Error -> {
                        // Silently fail for completed tasks
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

    fun getCurrentUserId(): String? = repository.getCurrentUserId()

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
    fun addTask(
        title: String,
        description: String? = null,
        tags: List<String> = emptyList(),
        dueDate: Long?
    ) {
        viewModelScope.launch {
            val task = FirebaseTask(
                title = title,
                description = description,
                tags = tags,
                dueDate = dueDate
            )

            when (val result = repository.addTask(task)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to add task: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    fun completeTask(taskId: String?, experienceReward: Int = 10, moneyReward: Int = 10) {
        if (taskId == null) {
            _error.value = "Cannot complete a task with a null ID."
            android.util.Log.e("AppViewModel", "completeTask was called with a null taskId.")
            return
        }
        viewModelScope.launch {
            android.util.Log.d("AppViewModel", "Completing task: $taskId")

            // Complete the task in Firebase - this will update EVERYTHING including weekly counter
            val result = repository.completeTask(taskId, experienceReward, moneyReward)
            android.util.Log.d("AppViewModel", "Task completion result: $result")

            // Manually refresh the profile to get updated stats
            kotlinx.coroutines.delay(300) // Small delay to ensure Firebase writes complete
            when (val profileResult = repository.getUserProfile()) {
                is FirebaseResult.Success -> {
                    _userProfile.value = profileResult.data
                    android.util.Log.d("AppViewModel", "Profile refreshed - weeklyTasks: ${profileResult.data.weeklyTasksCompleted}, exp: ${profileResult.data.playerExperience}")
                }
                else -> {
                    android.util.Log.e("AppViewModel", "Failed to refresh profile")
                }
            }

            // Refresh completed tasks and leaderboard
            refreshCompletedTasks()
        }
    }

    fun deleteTask(taskId: String?) {
        if (taskId == null) {
            _error.value = "Cannot delete a task with a null ID."
            android.util.Log.e("AppViewModel", "deleteTask was called with a null taskId.")
            return
        }
        viewModelScope.launch {
            when (val result = repository.deleteTask(taskId)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to delete task: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }
    // Tag operations
    fun addTagToTask(taskId: String, tag: String) {
        viewModelScope.launch {
            when (val result = repository.addTag(taskId, tag)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to add tag: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    fun removeTagFromTask(taskId: String, tag: String) {
        viewModelScope.launch {
            when (val result = repository.removeTag(taskId, tag)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to remove tag: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    // Tag statistics
    private val _tagStatistics = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tagStatistics: StateFlow<Map<String, Int>> = _tagStatistics.asStateFlow()

    fun loadTagStatistics() {
        viewModelScope.launch {
            when (val result = repository.getTagStatistics()) {
                is FirebaseResult.Success -> {
                    _tagStatistics.value = result.data
                }
                is FirebaseResult.Error -> {
                    android.util.Log.e("AppViewModel", "Failed to load tag stats: ${result.exception.message}")
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

    private fun refreshCompletedTasks() {
        viewModelScope.launch {
            try {
                val userId = repository.getCurrentUserId()
                if (userId != null) {
                    // Manually fetch completed tasks
                    val tasksSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("tasks")
                        .whereEqualTo("isCompleted", true)
                        .get()
                        .await()

                    val tasks = tasksSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.overpowered.data.FirebaseTask::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.completedAt?.time ?: 0 }

                    _completedTasks.value = tasks
                    android.util.Log.d("AppViewModel", "Manually refreshed completed tasks: ${tasks.size}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Error refreshing completed tasks: ${e.message}")
            }
        }
    }

    // Friend operations
    fun sendFriendRequest(playerName: String) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = repository.sendFriendRequest(playerName)) {
                is FirebaseResult.Success -> {
                    _error.value = "Friend request sent to $playerName!"
                }
                is FirebaseResult.Error -> {
                    _error.value = result.exception.message
                }
                else -> {}
            }
            _isLoading.value = false
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

    // Load leaderboard data
    fun loadLeaderboard(timeframe: LeaderboardTimeframe, rankingType: LeaderboardRankingType) {
        viewModelScope.launch {
            _isLoadingLeaderboard.value = true

            try {
                // Get friend IDs
                val friendIds = _friends.value.map { it.friendId }

                if (friendIds.isEmpty()) {
                    _leaderboardEntries.value = emptyList()
                    _isLoadingLeaderboard.value = false
                    return@launch
                }

                // Add current user to the list
                val currentUserId = repository.getCurrentUserId()
                val allUserIds = if (currentUserId != null) {
                    friendIds + currentUserId
                } else {
                    friendIds
                }

                // Get friend profiles
                val profilesResult = repository.getFriendProfiles(allUserIds)
                val profiles = (profilesResult as? FirebaseResult.Success)?.data ?: emptyList()

                // Get task counts
                val weekly = timeframe == LeaderboardTimeframe.WEEKLY
                val taskCountsResult = repository.getUserTaskCounts(allUserIds, weekly)
                val taskCounts = (taskCountsResult as? FirebaseResult.Success)?.data ?: emptyMap()

                // Create leaderboard entries
                val entries = profiles.map { profile ->
                    LeaderboardEntry(
                        userId = profile.userId,
                        playerName = profile.playerName,
                        profileImageUrl = profile.profileImageUrl,
                        level = profile.playerLevel,
                        tasksCompleted = taskCounts[profile.userId] ?: 0,
                        rank = 0 // Will be assigned after sorting
                    )
                }

                // Sort and rank
                val sortedEntries = when (rankingType) {
                    LeaderboardRankingType.LEVEL -> entries.sortedByDescending { it.level }
                    LeaderboardRankingType.TASKS -> entries.sortedByDescending { it.tasksCompleted }
                }

                val rankedEntries = sortedEntries.mapIndexed { index, entry ->
                    entry.copy(rank = index + 1)
                }

                _leaderboardEntries.value = rankedEntries
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Error loading leaderboard: ${e.message}", e)
                _leaderboardEntries.value = emptyList()
            } finally {
                _isLoadingLeaderboard.value = false
            }
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