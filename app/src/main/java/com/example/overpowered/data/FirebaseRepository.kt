package com.example.overpowered.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import android.net.Uri
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

    // Task operations
    suspend fun addTask(task: FirebaseTask): FirebaseResult<String> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Explicitly set required fields
            val taskWithUserId = task.copy(
                userId = userId,
                createdAt = Date(),
                isCompleted = false
            )

            // Convert to map to ensure proper serialization
            val taskMap = mapOf(
                "title" to taskWithUserId.title,
                "description" to taskWithUserId.description,
                "isCompleted" to taskWithUserId.isCompleted,
                "createdAt" to taskWithUserId.createdAt,
                "completedAt" to taskWithUserId.completedAt,
                "userId" to taskWithUserId.userId
            )

            val docRef = firestore.collection("tasks")
                .add(taskMap)
                .await()

            FirebaseResult.Success(docRef.id)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun updateTask(taskId: String, updates: Map<String, Any>): FirebaseResult<Unit> {
        return try {
            val updatesWithTimestamp = updates + ("lastUpdated" to Date())
            firestore.collection("tasks")
                .document(taskId)
                .update(updatesWithTimestamp)
                .await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun deleteTask(taskId: String): FirebaseResult<Unit> {
        return try {
            firestore.collection("tasks")
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

    // Real-time task updates
    fun observeUserTasks(): Flow<FirebaseResult<List<FirebaseTask>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val listener = firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", false) // Only get active tasks
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

    fun observeCompletedTasks(): kotlinx.coroutines.flow.Flow<FirebaseResult<List<FirebaseTask>>> {
        val userId = getCurrentUserId()
        if (userId == null) return kotlinx.coroutines.flow.flowOf(FirebaseResult.Error(Exception("User not authenticated")))

        return kotlinx.coroutines.flow.callbackFlow {
            val listener = FirebaseFirestore.getInstance()
                .collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", true)
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING) // newest first
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error)); return@addSnapshotListener
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