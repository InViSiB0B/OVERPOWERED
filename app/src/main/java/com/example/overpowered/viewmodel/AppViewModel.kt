package com.example.overpowered.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overpowered.data.*
import com.example.overpowered.today.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.net.Uri
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.FirebaseException
import com.example.overpowered.data.PhoneAuthCallback
import com.example.overpowered.progress.LeaderboardTimeframe
import com.example.overpowered.progress.LeaderboardRankingType
import com.example.overpowered.progress.LeaderboardEntry
import com.example.overpowered.data.LongTermGoal
import com.example.overpowered.data.GoalSize
import com.example.overpowered.data.FirebaseTask
import com.example.overpowered.data.FirebaseResult

sealed class PhoneAuthState {
    object Initial : PhoneAuthState()
    object SendingCode : PhoneAuthState()
    data class CodeSent(val verificationId: String, val phoneNumber: String) : PhoneAuthState()
    object VerifyingCode : PhoneAuthState()
    object Success : PhoneAuthState()
    data class Error(val message: String) : PhoneAuthState()
}
class AppViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // Authentication states
    private val _phoneAuthState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Initial)
    val phoneAuthState: StateFlow<PhoneAuthState> = _phoneAuthState.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Onboarding state
    private val _isOnboarded = MutableStateFlow(false)
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

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

    // Enriched friends state with current profile data (frames, titles)
    private val _enrichedFriends = MutableStateFlow<List<Friendship>>(emptyList())
    val enrichedFriends: StateFlow<List<Friendship>> = _enrichedFriends.asStateFlow()

    // Searched user state
    private val _searchedUser = MutableStateFlow<UserProfile?>(null)
    val searchedUser: StateFlow<UserProfile?> = _searchedUser.asStateFlow()

    // Long Term Goals State
    private val _longTermGoals = MutableStateFlow<List<LongTermGoal>>(emptyList())
    val longTermGoals: StateFlow<List<LongTermGoal>> = _longTermGoals.asStateFlow()

    // Leaderboard state
    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()

    private val _isLoadingLeaderboard = MutableStateFlow(false)
    val isLoadingLeaderboard: StateFlow<Boolean> = _isLoadingLeaderboard.asStateFlow()

    // Completed goal state for popup dialog
    private val _completedGoalInfo = MutableStateFlow<CompletedGoalInfo?>(null)
    val completedGoalInfo: StateFlow<CompletedGoalInfo?> = _completedGoalInfo.asStateFlow()

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
        dueDate = this.dueDate,
        isRecurring = this.isRecurring,
        recurrenceType = this.recurrenceType,
        recurrenceParentId = this.recurrenceParentId
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

            val currentUser = repository.getCurrentUserId()

            if (currentUser != null && !repository.isAnonymousUser()) {
                // User is already signed in with phone
                try {
                    val onboarded = repository.isUserOnboarded()
                    _isOnboarded.value = onboarded

                    if (onboarded) {
                        repository.initializeTaskTracking()
                        loadUserData()
                        _phoneAuthState.value = PhoneAuthState.Success
                    }
                } catch (e: Exception) {
                    _isOnboarded.value = false
                }
            } else {
                // No authenticated user - show phone auth
                _phoneAuthState.value = PhoneAuthState.Initial
            }

            _isLoading.value = false
        }
    }

    // Phone authentication methods
    fun startPhoneAuth(phoneNumber: String, activity: android.app.Activity) {
        _phoneAuthState.value = PhoneAuthState.SendingCode

        repository.startPhoneVerification(
            phoneNumber = phoneNumber,
            activity = activity,
            callback = object : PhoneAuthCallback {
                override fun onCodeSent(verificationId: String) {
                    _phoneAuthState.value = PhoneAuthState.CodeSent(verificationId, phoneNumber)
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch {
                        handlePhoneSignIn(credential, phoneNumber)
                    }
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    _phoneAuthState.value = PhoneAuthState.Error(exception.message ?: "Verification failed")
                }
            }
        )
    }

    fun verifyPhoneCode(verificationId: String, code: String, phoneNumber: String) {
        viewModelScope.launch {
            _phoneAuthState.value = PhoneAuthState.VerifyingCode

            when (val result = repository.verifyPhoneCode(verificationId, code)) {
                is FirebaseResult.Success -> {
                    // Check if user is onboarded
                    val onboarded = repository.isUserOnboarded()
                    if (onboarded) {
                        _isOnboarded.value = true
                        _phoneAuthState.value = PhoneAuthState.Success
                        repository.initializeTaskTracking()
                        loadUserData()
                    } else {
                        // New user - proceed to onboarding with phone number
                        _phoneAuthState.value = PhoneAuthState.Success
                        _isOnboarded.value = false
                        // Phone number will be passed to onboarding
                    }
                }
                is FirebaseResult.Error -> {
                    _phoneAuthState.value = PhoneAuthState.Error(result.exception.message ?: "Verification failed")
                }
                else -> {}
            }
        }
    }

    private suspend fun handlePhoneSignIn(credential: PhoneAuthCredential, phoneNumber: String) {
        when (val result = repository.signInWithPhoneCredential(credential)) {
            is FirebaseResult.Success -> {
                val onboarded = repository.isUserOnboarded()
                if (onboarded) {
                    _isOnboarded.value = true
                    _phoneAuthState.value = PhoneAuthState.Success
                    repository.initializeTaskTracking()
                    loadUserData()
                } else {
                    _phoneAuthState.value = PhoneAuthState.Success
                    _isOnboarded.value = false
                }
            }
            is FirebaseResult.Error -> {
                _phoneAuthState.value = PhoneAuthState.Error(result.exception.message ?: "Sign in failed")
            }
            else -> {}
        }
    }

    fun resetPhoneAuthState() {
        _phoneAuthState.value = PhoneAuthState.Initial
    }

    fun completeOnboarding(username: String, phoneNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = repository.completeOnboarding(username, phoneNumber)) {
                is FirebaseResult.Success -> {
                    kotlinx.coroutines.delay(500)
                    _isOnboarded.value = true
                    repository.initializeTaskTracking()
                    loadUserData()
                }
                is FirebaseResult.Error -> {
                    _error.value = "Failed to complete onboarding: ${result.exception.message}"
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

        // Check for missed days and update strikes on startup
        viewModelScope.launch {
            repository.checkAndUpdateStrikes()
        }

        // Observe long term goals
        repository.observeLongTermGoals().onEach { result ->
            when (result) {
                is FirebaseResult.Success -> {
                    _longTermGoals.value = result.data
                    android.util.Log.d("AppViewModel", "Long-term goals updated: ${result.data.size}")
                }
                is FirebaseResult.Error -> {
                    android.util.Log.e("AppViewModel", "Error observing long-term goals: ${result.exception.message}")
                }
                else -> {}
            }
        }.launchIn(viewModelScope)

        // Observe friends list
        viewModelScope.launch {
            repository.observeFriends().collect { result ->
                when (result) {
                    is FirebaseResult.Success -> {
                        _friends.value = result.data
                        // Enrich friends with current profile data
                        enrichFriendsWithCurrentProfiles()
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
            _isLoading.value = true
            when (val result = repository.updatePlayerNameWithDiscriminator(name)) {
                is FirebaseResult.Success -> {
                    _error.value = "Name updated successfully!"
                }
                is FirebaseResult.Error -> {
                    _error.value = "Failed to update name: ${result.exception.message}"
                }
                else -> {}
            }
            _isLoading.value = false
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
        dueDate: Long?,
        isRecurring: Boolean = false,
        recurrenceType: String? = null
    ) {
        viewModelScope.launch {
            val task = FirebaseTask(
                title = title,
                description = description,
                tags = tags,
                dueDate = dueDate,
                isRecurring = isRecurring,
                recurrenceType = recurrenceType,
                recurrenceParentId = null
            )

            when (val result = repository.addTask(task)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to add task: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    fun updateTask(
        task: Task,
        title: String,
        description: String?,
        tags: List<String>,
        dueDate: Long?,
        isRecurring: Boolean,
        recurrenceType: String?
    ) {
        viewModelScope.launch {
            try {
                // Find the corresponding FirebaseTask by matching the local task ID
                val firebaseTask = _tasks.value.find {
                    it.id.hashCode().toLong() == task.id
                }

                if (firebaseTask == null) {
                    _error.value = "Task not found"
                    return@launch
                }

                // Create updates map (filter out null values)
                val updates = buildMap<String, Any> {
                    put("title", title)
                    description?.let { put("description", it) }
                    put("tags", tags)
                    dueDate?.let { put("dueDate", it) }
                    put("isRecurring", isRecurring)
                    recurrenceType?.let { put("recurrenceType", it) }
                }

                // Update in Firebase
                when (val result = repository.updateTask(firebaseTask.id ?: "", updates)) {
                    is FirebaseResult.Error -> {
                        _error.value = "Failed to update task: ${result.exception.message}"
                    }
                    else -> {
                        // Success - the observer will automatically update _tasks
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error updating task: ${e.message}"
            }
        }
    }

    fun completeTask(taskId: String?, experienceReward: Int = 10, moneyReward: Int = 10) {
        if (taskId == null) {
            _error.value = "Cannot complete a task with a null ID."
            return
        }
        viewModelScope.launch {
            val task = _tasks.value.find { it.id == taskId }
            val taskTags = task?.tags ?: emptyList()

            repository.completeTask(taskId, experienceReward, moneyReward)

            if (taskTags.isNotEmpty()) {
                try {
                    val result = repository.updateGoalProgressForTask(taskTags)
                    if (result is FirebaseResult.Success && result.data.isNotEmpty()) {
                        // Show popup for the first completed goal
                        // (If multiple goals complete, show them one at a time)
                        _completedGoalInfo.value = result.data.first()
                    }
                } catch (e: Exception) {
                    // Silent fail
                }
            }

            kotlinx.coroutines.delay(300)
            when (val profileResult = repository.getUserProfile()) {
                is FirebaseResult.Success -> {
                    _userProfile.value = profileResult.data
                }
                else -> {}
            }

            refreshCompletedTasks()
        }
    }

    fun clearCompletedGoalInfo() {
        _completedGoalInfo.value = null
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

    // Delete a single instance
    fun deleteSingleRecurringOccurrence(taskId: String?) {
        if (taskId == null) {
            _error.value = "Cannot delete a task with a null ID."
            android.util.Log.e("AppViewModel", "deleteSingleRecurringOccurrence was called with a null taskId.")
            return
        }
        viewModelScope.launch {
            when (val result = repository.deleteSingleRecurringOccurrence(taskId)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to delete task: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    // Delete all Recurring Instances
    fun deleteAllRecurringInstances(recurrenceParentId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteAllRecurringInstances(recurrenceParentId)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to delete recurring tasks: ${result.exception.message}"
                }
                else -> {
                    android.util.Log.d("AppViewModel", "Successfully deleted all recurring instances")
                }
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
    fun searchUser(playerName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _searchedUser.value = null // Clear previous search

            when (val result = repository.searchUser(playerName)) {
                is FirebaseResult.Success -> {
                    _searchedUser.value = result.data
                }
                is FirebaseResult.Error -> {
                    _error.value = result.exception.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun clearSearchedUser() {
        _searchedUser.value = null
    }

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

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            when (val result = repository.removeFriend(friendId)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to remove friend: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    // Enrich friends list with current profile data (frames, titles, etc.)
    private fun enrichFriendsWithCurrentProfiles() {
        viewModelScope.launch {
            val friendIds = _friends.value.map { it.friendId }

            if (friendIds.isEmpty()) {
                _enrichedFriends.value = emptyList()
                return@launch
            }

            // Fetch current profiles for all friends
            val profilesResult = repository.getFriendProfiles(friendIds)
            val profiles = (profilesResult as? FirebaseResult.Success)?.data ?: emptyList()

            // Map profiles by userId for quick lookup
            val profileMap = profiles.associateBy { it.userId }

            // Update friendship data with current profile information
            val enriched = _friends.value.map { friendship ->
                val currentProfile = profileMap[friendship.friendId]
                if (currentProfile != null) {
                    friendship.copy(
                        friendProfileImageUrl = currentProfile.profileImageUrl,
                        selectedFrame = currentProfile.selectedFrame,
                        selectedTitle = currentProfile.selectedTitle
                    )
                } else {
                    friendship
                }
            }

            _enrichedFriends.value = enriched
        }
    }

    // Long term goal operations
    companion object {
        const val GOAL_CREATION_COST = 100
    }

    fun createLongTermGoal(
        name: String,
        description: String?,
        tags: List<String>,
        size: String
    ) {
        viewModelScope.launch {
            val currentProfile = _userProfile.value
            if (currentProfile.playerMoney < GOAL_CREATION_COST) {
                _error.value = "Insufficient funds - need $GOAL_CREATION_COST coins to create a goal"
                return@launch
            }

            // Deduct coins first
            val updatedProfile = currentProfile.copy(
                playerMoney = currentProfile.playerMoney - GOAL_CREATION_COST
            )
            when (val saveResult = repository.saveUserProfile(updatedProfile)) {
                is FirebaseResult.Error -> {
                    _error.value = "Failed to deduct coins: ${saveResult.exception.message}"
                    return@launch
                }
                else -> {}
            }

            // Now create the goal
            when (val result = repository.createLongTermGoal(name, description, tags, size)) {
                is FirebaseResult.Success -> {
                    android.util.Log.d("AppViewModel", "Long-term goal created successfully")
                }
                is FirebaseResult.Error -> {
                    // Refund coins if goal creation failed
                    repository.saveUserProfile(currentProfile)
                    android.util.Log.e("AppViewModel", "Failed to create long-term goal: ${result.exception.message}")
                    _error.value = "Failed to create goal: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    fun updateLongTermGoal(
        goal: LongTermGoal,
        name: String,
        description: String?,
        tags: List<String>
    ) {
        viewModelScope.launch {
            try {
                // Build updates map - only name, description, and tags are editable
                // Size/duration cannot be changed after creation
                val updates = buildMap<String, Any> {
                    put("name", name)
                    description?.let { put("description", it) }
                    put("tags", tags)
                }

                // Update in Firebase
                when (val result = repository.updateLongTermGoal(goal.id, updates)) {
                    is FirebaseResult.Error -> {
                        _error.value = "Failed to update goal: ${result.exception.message}"
                    }
                    else -> {
                        // Success - update local state (preserve size-related fields)
                        val updatedGoal = goal.copy(
                            name = name,
                            description = description,
                            tags = tags
                        )

                        val currentGoals = _longTermGoals.value.toMutableList()
                        val index = currentGoals.indexOfFirst { it.id == goal.id }
                        if (index != -1) {
                            currentGoals[index] = updatedGoal
                            _longTermGoals.value = currentGoals
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error updating goal: ${e.message}"
            }
        }
    }

    fun deleteLongTermGoal(goalId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteLongTermGoal(goalId)) {
                is FirebaseResult.Success -> {
                    android.util.Log.d("AppViewModel", "Long-term goal deleted successfully")
                }
                is FirebaseResult.Error -> {
                    android.util.Log.e("AppViewModel", "Failed to delete long-term goal: ${result.exception.message}")
                }
                else -> {}
            }
        }
    }

    // Load leaderboard data
    fun loadLeaderboard(timeframe: LeaderboardTimeframe, rankingType: LeaderboardRankingType) {
        viewModelScope.launch {
            _isLoadingLeaderboard.value = true

            try {
                // Get friend IDss
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
                        selectedFrame = profile.selectedFrame,
                        selectedTitle = profile.selectedTitle,
                        level = profile.playerLevel,
                        tasksCompleted = taskCounts[profile.userId] ?: 0,
                        rank = 0 // Will be assigned after sorting.
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

    fun logout() {
        viewModelScope.launch {
            repository.signOut()
            _phoneAuthState.value = PhoneAuthState.Initial
            _isOnboarded.value = false
            _userProfile.value = UserProfile()
            _tasks.value = emptyList()
            _completedTasks.value = emptyList()
            _pendingFriendRequests.value = emptyList()
            _friends.value = emptyList()
            _enrichedFriends.value = emptyList()
            _longTermGoals.value = emptyList()
            _leaderboardEntries.value = emptyList()
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