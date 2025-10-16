package com.example.overpowered.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.*

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Authentication
    suspend fun signInAnonymously(): FirebaseResult<String> {
        return try {
            val result = auth.signInAnonymously().await()
            val userId = result.user?.uid ?: throw Exception("Failed to get user ID")
            FirebaseResult.Success(userId)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Profile operations
    suspend fun saveUserProfile(profile: UserProfile): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val profileWithUserId = profile.copy(userId = userId, lastUpdated = Date())
            firestore.collection("users")
                .document(userId)
                .set(profileWithUserId)
                .await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun getUserProfile(): FirebaseResult<UserProfile> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                    ?: throw Exception("Failed to parse profile")
                FirebaseResult.Success(profile)
            } else {
                // Create default profile if it doesn't exist
                val defaultProfile = UserProfile(userId = userId, createdAt = Date())
                saveUserProfile(defaultProfile)
                FirebaseResult.Success(defaultProfile)
            }
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Real-time profile updates
    fun observeUserProfile(): Flow<FirebaseResult<UserProfile>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val listener = firestore.collection("users")
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val profile = snapshot.toObject(UserProfile::class.java)
                                ?: throw Exception("Failed to parse profile")
                            trySend(FirebaseResult.Success(profile))
                        } catch (e: Exception) {
                            trySend(FirebaseResult.Error(e))
                        }
                    } else {
                        trySend(FirebaseResult.Error(Exception("Profile not found")))
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    // ==================== TASK OPERATIONS (SUBCOLLECTION) ====================
    suspend fun addTask(task: FirebaseTask): FirebaseResult<String> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val taskWithUserId = task.copy(
                userId = userId,
                createdAt = Date(),
                isCompleted = false
            )

            val taskMap = mapOf(
                "title" to taskWithUserId.title,
                "description" to taskWithUserId.description,
                "isCompleted" to taskWithUserId.isCompleted,
                "createdAt" to taskWithUserId.createdAt,
                "completedAt" to taskWithUserId.completedAt,
                "userId" to taskWithUserId.userId
            )

            val docRef = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .add(taskMap)
                .await()

            FirebaseResult.Success(docRef.id)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun updateTask(taskId: String, updates: Map<String, Any>): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val updatesWithTimestamp = updates + ("lastUpdated" to Date())

            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .update(updatesWithTimestamp)
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun deleteTask(taskId: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .delete()
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun completeTask(taskId: String, experienceReward: Int = 10, moneyReward: Int = 10): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            android.util.Log.d("FirebaseRepo", "Completing task: $taskId")

            // Mark task as completed
            val updates = mapOf(
                "isCompleted" to true,
                "completedAt" to Date(),
                "experienceReward" to experienceReward,
                "moneyReward" to moneyReward
            )
            updateTask(taskId, updates)
            android.util.Log.d("FirebaseRepo", "Task marked as completed")

            // Update profile with stats AND counters
            val profileResult = getUserProfile()
            if (profileResult is FirebaseResult.Success) {
                val profile = profileResult.data
                android.util.Log.d("FirebaseRepo", "Current profile - weeklyTasks: ${profile.weeklyTasksCompleted}, lifetimeTasks: ${profile.lifetimeTasksCompleted}")

                // Calculate new stats
                val newExperience = profile.playerExperience + experienceReward
                val newMoney = profile.playerMoney + moneyReward
                val newLevel = (newExperience / 100) + 1

                // Check if we need to reset weekly counter
                val needsReset = isNewWeek(profile.weekStartDate)
                android.util.Log.d("FirebaseRepo", "Needs weekly reset: $needsReset")

                val updatedProfile = if (needsReset) {
                    // Reset weekly counter for new week
                    profile.copy(
                        playerExperience = newExperience,
                        playerMoney = newMoney,
                        playerLevel = newLevel,
                        weeklyTasksCompleted = 1,
                        weekStartDate = getWeekStartDate(),
                        lifetimeTasksCompleted = profile.lifetimeTasksCompleted + 1,
                        lastUpdated = Date()
                    )
                } else {
                    // Increment both counters
                    profile.copy(
                        playerExperience = newExperience,
                        playerMoney = newMoney,
                        playerLevel = newLevel,
                        weeklyTasksCompleted = profile.weeklyTasksCompleted + 1,
                        lifetimeTasksCompleted = profile.lifetimeTasksCompleted + 1,
                        lastUpdated = Date()
                    )
                }

                android.util.Log.d("FirebaseRepo", "Updated profile - weeklyTasks: ${updatedProfile.weeklyTasksCompleted}, lifetimeTasks: ${updatedProfile.lifetimeTasksCompleted}")
                saveUserProfile(updatedProfile)
                android.util.Log.d("FirebaseRepo", "Profile saved successfully")
            } else {
                android.util.Log.e("FirebaseRepo", "Failed to get profile")
            }

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepo", "Error completing task: ${e.message}", e)
            FirebaseResult.Error(e)
        }
    }

    suspend fun addTag(taskId: String, vararg tags: String): FirebaseResult<Unit> {
        // TODO: call getCurrentUserId()?

        if (tags.isEmpty()) {
            return FirebaseResult.Error(IllegalArgumentException("No tags supplied"))
        }

        return try {
            val updates = mapOf(
                "tags" to FieldValue.arrayUnion(*tags)
            )
            firestore.collection("tasks")
                .document(taskId)
                .update(updates)
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun removeTag(taskId: String, vararg tags: String): FirebaseResult<Unit> {
        // TODO: call getCurrentUserId()?

        if (tags.isEmpty()) {
            return FirebaseResult.Error(IllegalArgumentException("No tags supplied"))
        }

        return try {
            val updates = mapOf(
                "tags" to FieldValue.arrayRemove(*tags)
            )
            firestore.collection("tasks")
                .document(taskId)
                .update(updates)
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun setDeadline(taskId: String, deadline: Date?): FirebaseResult<Unit> {
        // TODO: call getCurrentUserId()?

        val updates = if (deadline != null) {
            mapOf(
                "deadline" to deadline,
                "lastUpdated" to Date()
            )
        } else {
            // Delete the "deadline" field using FieldValue.delete() instead of setting "deadline" to null
            mapOf(
                "deadline" to FieldValue.delete(),
                "lastUpdated" to Date()
            )
        }

        return try {
            firestore.collection("tasks")
                .document(taskId)
                .update(updates)
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Real-time task updates (active tasks only)
    fun observeUserTasks(): Flow<FirebaseResult<List<FirebaseTask>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val listener = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .whereEqualTo("isCompleted", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val tasks = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(FirebaseTask::class.java)?.copy(id = doc.id)
                            }
                            trySend(FirebaseResult.Success(tasks))
                        } catch (e: Exception) {
                            trySend(FirebaseResult.Error(e))
                        }
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    // Real-time completed tasks
    fun observeCompletedTasks(): Flow<FirebaseResult<List<FirebaseTask>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val listener = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .whereEqualTo("isCompleted", true)
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val tasks = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(FirebaseTask::class.java)?.copy(id = doc.id)
                            }
                            trySend(FirebaseResult.Success(tasks))
                        } catch (e: Exception) {
                            trySend(FirebaseResult.Error(e))
                        }
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    fun observeTasksWithTag(tag: String): Flow<FirebaseResult<List<FirebaseTask>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val listener = firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereArrayContains("tags", tag)
                .whereEqualTo("isCompleted", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val tasks = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(FirebaseTask::class.java)?.copy(id = doc.id)
                            }
                            trySend(FirebaseResult.Success(tasks))
                        } catch (e: Exception) {
                            trySend(FirebaseResult.Error(e))
                        }
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    fun observeUpcomingTasks(limit: Long = 20): Flow<FirebaseResult<List<FirebaseTask>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val now = Date()
            val listener = firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", false)
                .whereGreaterThanOrEqualTo("deadline", now)
                .orderBy("deadline", Query.Direction.ASCENDING)
                .limit(limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val tasks = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(FirebaseTask::class.java)?.copy(id = doc.id)
                            }
                            trySend(FirebaseResult.Success(tasks))
                        } catch (e: Exception) {
                            trySend(FirebaseResult.Error(e))
                        }
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    // ==================== FRIEND REQUEST OPERATIONS (SUBCOLLECTION) ====================
    suspend fun sendFriendRequest(playerName: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Get current user's profile
            val currentUserProfile = when (val result = getUserProfile()) {
                is FirebaseResult.Success -> result.data
                is FirebaseResult.Error -> throw result.exception
                else -> throw Exception("Failed to get current user profile")
            }

            // Search for user by player name
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("playerName", playerName)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return FirebaseResult.Error(Exception("Player not found"))
            }

            val targetUserDoc = querySnapshot.documents.first()
            val targetUser = targetUserDoc.toObject(UserProfile::class.java)
                ?: return FirebaseResult.Error(Exception("Failed to parse user profile"))

            if (targetUser.userId == userId) {
                return FirebaseResult.Error(Exception("You cannot send a friend request to yourself"))
            }

            // Create friend request in RECIPIENT's subcollection
            val friendRequest = mapOf(
                "fromUserId" to userId,
                "fromUserName" to currentUserProfile.playerName,
                "toUserId" to targetUser.userId,
                "toUserName" to targetUser.playerName,
                "status" to "pending",
                "createdAt" to Date()
            )

            firestore.collection("users")
                .document(targetUser.userId)
                .collection("friendRequests")
                .add(friendRequest)
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Observe pending friend requests (in current user's subcollection)
    fun observePendingFriendRequests(): Flow<FirebaseResult<List<FriendRequest>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val listener = firestore.collection("users")
                .document(userId)
                .collection("friendRequests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val requests = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                            }.filter { it.status == "pending" }

                            trySend(FirebaseResult.Success(requests))
                        } catch (e: Exception) {
                            trySend(FirebaseResult.Error(e))
                        }
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    suspend fun acceptFriendRequest(requestId: String, request: FriendRequest): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Check if already friends first (prevent duplicates)
            val existingFriendship = firestore.collection("users")
                .document(userId)
                .collection("friends")
                .get()
                .await()

            val alreadyFriends = existingFriendship.documents.any { doc ->
                doc.toObject(Friendship::class.java)?.friendId == request.fromUserId
            }

            if (alreadyFriends) {
                return FirebaseResult.Error(Exception("Already friends with this player"))
            }

            // Get both user profiles
            val toUserDoc = firestore.collection("users").document(request.toUserId).get().await()
            val fromUserDoc = firestore.collection("users").document(request.fromUserId).get().await()

            val toUser = toUserDoc.toObject(UserProfile::class.java)
            val fromUser = fromUserDoc.toObject(UserProfile::class.java)

            if (toUser == null || fromUser == null) {
                return FirebaseResult.Error(Exception("Failed to get user profiles"))
            }

            // Create friendship for Person B (recipient/toUser)
            val friendship1 = mapOf(
                "friendId" to request.fromUserId,
                "friendName" to request.fromUserName,
                "friendProfileImageUrl" to fromUser.profileImageUrl,
                "createdAt" to Date()
            )

            // Create friendship for Person A (sender/fromUser)
            val friendship2 = mapOf(
                "friendId" to request.toUserId,
                "friendName" to request.toUserName,
                "friendProfileImageUrl" to toUser.profileImageUrl,
                "createdAt" to Date()
            )

            // Add to Person B's (recipient) friends subcollection
            firestore.collection("users")
                .document(request.toUserId)
                .collection("friends")
                .add(friendship1)
                .await()

            // Add to Person A's (sender) friends subcollection
            firestore.collection("users")
                .document(request.fromUserId)
                .collection("friends")
                .add(friendship2)
                .await()

            // Delete the request
            firestore.collection("users")
                .document(userId)
                .collection("friendRequests")
                .document(requestId)
                .delete()
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun ignoreFriendRequest(requestId: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("friendRequests")
                .document(requestId)
                .delete()
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Observe friends list (in current user's subcollection)
    fun observeFriends(): Flow<FirebaseResult<List<Friendship>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val listener = firestore.collection("users")
                .document(userId)
                .collection("friends")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val friends = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                            }
                            trySend(FirebaseResult.Success(friends))
                        } catch (e: Exception) {
                            trySend(FirebaseResult.Error(e))
                        }
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    // Profile image operations
    suspend fun uploadProfileImage(imageUri: Uri): FirebaseResult<String> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val imageRef = storage.reference
                .child("profile_images")
                .child("$userId.jpg")

            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()

            FirebaseResult.Success(downloadUrl.toString())
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Batch operations for efficiency
    suspend fun updatePlayerStats(
        experienceIncrease: Int,
        moneyIncrease: Int
    ): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Get current profile
            val currentProfile = when (val result = getUserProfile()) {
                is FirebaseResult.Success -> result.data
                is FirebaseResult.Error -> throw result.exception
                else -> throw Exception("Failed to get profile")
            }

            // Calculate new values
            val newExperience = currentProfile.playerExperience + experienceIncrease
            val newMoney = currentProfile.playerMoney + moneyIncrease
            val newLevel = (newExperience / 100) + 1

            // Update profile
            val updatedProfile = currentProfile.copy(
                playerExperience = newExperience,
                playerMoney = newMoney,
                playerLevel = newLevel,
                lastUpdated = Date()
            )

            saveUserProfile(updatedProfile)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Get profiles of all friends
    suspend fun getFriendProfiles(friendIds: List<String>): FirebaseResult<List<UserProfile>> {
        if (friendIds.isEmpty()) {
            return FirebaseResult.Success(emptyList())
        }

        return try {
            val profiles = mutableListOf<UserProfile>()

            // Firestore 'in' query can only handle 10 items at a time
            friendIds.chunked(10).forEach { chunk ->
                val snapshot = firestore.collection("users")
                    .whereIn("userId", chunk)
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    doc.toObject(UserProfile::class.java)?.let { profiles.add(it) }
                }
            }

            FirebaseResult.Success(profiles)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Get task counts for users
    suspend fun getUserTaskCounts(userIds: List<String>, weekly: Boolean): FirebaseResult<Map<String, Int>> {
        if (userIds.isEmpty()) {
            return FirebaseResult.Success(emptyMap())
        }

        return try {
            val taskCounts = mutableMapOf<String, Int>()

            android.util.Log.d("FirebaseRepo", "Getting task counts - weekly: $weekly, users: ${userIds.size}")

            // Both weekly and lifetime now read from profile
            for (userId in userIds) {
                val doc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val profile = doc.toObject(UserProfile::class.java)
                val count = if (weekly) {
                    profile?.weeklyTasksCompleted ?: 0
                } else {
                    profile?.lifetimeTasksCompleted ?: 0
                }
                taskCounts[userId] = count
                android.util.Log.d("FirebaseRepo", "User $userId - ${if (weekly) "Weekly" else "Lifetime"} tasks: $count")
            }

            android.util.Log.d("FirebaseRepo", "Final task counts: $taskCounts")
            FirebaseResult.Success(taskCounts)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepo", "Error getting task counts: ${e.message}", e)
            FirebaseResult.Error(e)
        }
    }

    // Helper to check if we need to reset weekly counter
    private fun isNewWeek(weekStartDate: Date?): Boolean {
        if (weekStartDate == null) return true

        val calendar = java.util.Calendar.getInstance()
        calendar.time = weekStartDate
        val weekStart = calendar.clone() as java.util.Calendar

        val now = java.util.Calendar.getInstance()

        // Check if current date is in a different week
        val weeksDiff = (now.timeInMillis - weekStart.timeInMillis) / (7 * 24 * 60 * 60 * 1000)
        return weeksDiff >= 1
    }

    // Get start of current week
    private fun getWeekStartDate(): Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.time
    }

    suspend fun initializeTaskTracking(): FirebaseResult<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))
            val profileResult = getUserProfile()
            if (profileResult is FirebaseResult.Success) {
                val profile = profileResult.data

                // Only initialize if not already set
                if (profile.weekStartDate == null) {
                    // Count existing completed tasks for lifetime
                    val completedTasksSnapshot = firestore.collection("users")
                        .document(userId)
                        .collection("tasks")
                        .whereEqualTo("isCompleted", true)
                        .get()
                        .await()

                    val lifetimeCount = completedTasksSnapshot.size()

                    val updatedProfile = profile.copy(
                        weeklyTasksCompleted = 0,
                        weekStartDate = getWeekStartDate(),
                        lifetimeTasksCompleted = lifetimeCount  // Initialize with actual count
                    )
                    saveUserProfile(updatedProfile)
                    android.util.Log.d("FirebaseRepo", "Initialized task tracking - lifetime: $lifetimeCount")
                }
            }
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }
}

