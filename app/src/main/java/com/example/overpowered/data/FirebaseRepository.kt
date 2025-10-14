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

    suspend fun completeTask(taskId: String): FirebaseResult<Unit> {
        return try {
            val updates = mapOf(
                "isCompleted" to true,
                "completedAt" to Date()
            )
            updateTask(taskId, updates)
        } catch (e: Exception) {
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
}