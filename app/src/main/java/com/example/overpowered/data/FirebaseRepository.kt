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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import java.util.*


// Phone authentication callback
interface PhoneAuthCallback {
    fun onCodeSent(verificationId: String)
    fun onVerificationCompleted(credential: PhoneAuthCredential)
    fun onVerificationFailed(exception: FirebaseException)
}

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Start phone verification
    fun startPhoneVerification(
        phoneNumber: String,
        activity: android.app.Activity,
        callback: PhoneAuthCallback
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    callback.onVerificationCompleted(credential)
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    callback.onVerificationFailed(exception)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    callback.onCodeSent(verificationId)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // Sign in with phone credential
    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): FirebaseResult<String> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val userId = result.user?.uid ?: throw Exception("Failed to get user ID")
            FirebaseResult.Success(userId)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Verify phone code
    suspend fun verifyPhoneCode(verificationId: String, code: String): FirebaseResult<String> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneCredential(credential)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Check if current user is anonymous
    fun isAnonymousUser(): Boolean {
        return auth.currentUser?.isAnonymous == true
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Check if username+discriminator combo exists
    suspend fun isUsernameDiscriminatorTaken(username: String, discriminator: String): Boolean {
        return try {
            val snapshot = firestore.collection("usernameDiscriminators")
                .whereEqualTo("username", username.lowercase())
                .whereEqualTo("discriminator", discriminator)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    // Get all discriminators for a username
    suspend fun getExistingDiscriminators(username: String): List<String> {
        return try {
            val snapshot = firestore.collection("usernameDiscriminators")
                .whereEqualTo("username", username.lowercase())
                .get()
                .await()

            snapshot.documents.mapNotNull {
                it.toObject(UsernameDiscriminator::class.java)?.discriminator
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Generate unique discriminator for username
    suspend fun generateUniqueDiscriminator(username: String): String {
        val existingDiscriminators = getExistingDiscriminators(username)

        // If all 10,000 discriminators are taken (0000-9999))
        if (existingDiscriminators.size >= 10000) {
            throw Exception("All discriminators for username '$username' are taken")
        }

        // Generate random 4-digit discriminator that's not taken
        val availableDiscriminators = (0..9999).map { it.toString().padStart(4, '0') }
            .filterNot { it in existingDiscriminators }

        return availableDiscriminators.random()
    }

    // Save username+discriminator combination
    suspend fun saveUsernameDiscriminator(username: String, discriminator: String, userId: String): FirebaseResult<Unit> {
        return try {
            val usernameDiscriminator = UsernameDiscriminator(
                username = username.lowercase(),
                discriminator = discriminator,
                userId = userId,
                createdAt = Date()
            )

            firestore.collection("usernameDiscriminators")
                .add(usernameDiscriminator)
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Delete old username+discriminator when user changes name
    suspend fun deleteOldUsernameDiscriminator(userId: String): FirebaseResult<Unit> {
        return try {
            val snapshot = firestore.collection("usernameDiscriminators")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.forEach { it.reference.delete().await() }

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Update user profile with new name and discriminator
    suspend fun updatePlayerNameWithDiscriminator(newName: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Delete old username+discriminator entry
            deleteOldUsernameDiscriminator(userId)

            // Generate new discriminator
            val newDiscriminator = generateUniqueDiscriminator(newName)

            // Save new username+discriminator
            saveUsernameDiscriminator(newName, newDiscriminator, userId)

            // Update profile
            val profileResult = getUserProfile()
            if (profileResult is FirebaseResult.Success) {
                val updatedProfile = profileResult.data.copy(
                    playerName = newName,
                    discriminator = newDiscriminator
                )
                saveUserProfile(updatedProfile)
            } else {
                throw Exception("Failed to get current profile")
            }
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Phone authentication
    suspend fun signInWithPhone(verificationId: String, code: String): FirebaseResult<String> {
        return try {
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, code)
            val result = auth.signInWithCredential(credential).await()
            val userId = result.user?.uid ?: throw Exception("Failed to get user ID")
            FirebaseResult.Success(userId)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Check if user is new (not onboarded)
    suspend fun isUserOnboarded(): Boolean {
        val profileResult = getUserProfile()
        return if (profileResult is FirebaseResult.Success) {
            profileResult.data.isOnboarded
        } else {
            false
        }
    }

    // Complete onboarding
    suspend fun completeOnboarding(username: String, phoneNumber: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val discriminator = generateUniqueDiscriminator(username)
            saveUsernameDiscriminator(username, discriminator, userId)

            val profileResult = getUserProfile()

            when (profileResult) {
                is FirebaseResult.Success -> {
                    val updatedProfile = profileResult.data.copy(
                        playerName = username,
                        discriminator = discriminator,
                        isOnboarded = true,
                        phoneNumber = phoneNumber
                    )

                    val saveResult = saveUserProfile(updatedProfile)

                    when (saveResult) {
                        is FirebaseResult.Success -> FirebaseResult.Success(Unit)
                        is FirebaseResult.Error -> throw saveResult.exception
                        else -> throw Exception("Unknown save result")
                    }
                }
                is FirebaseResult.Error -> throw profileResult.exception
                else -> throw Exception("Unknown error getting profile")
            }
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }


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
                "userId" to taskWithUserId.userId,
                "tags" to taskWithUserId.tags,
                "dueDate" to taskWithUserId.dueDate,
                "isRecurring" to taskWithUserId.isRecurring,
                "recurrenceType" to taskWithUserId.recurrenceType,
                "recurrenceParentId" to taskWithUserId.recurrenceParentId
            )

            val docRef = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .add(taskMap)
                .await()

            if (taskWithUserId.isRecurring && taskWithUserId.recurrenceParentId == null) {
                firestore.collection("users")
                    .document(userId)
                    .collection("tasks")
                    .document(docRef.id)
                    .update("recurrenceParentId", docRef.id)
                    .await()
            }

            FirebaseResult.Success(docRef.id)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Calculate the next occurrence date based on recurrence type
    private fun calculateNextOccurrence(currentDueDate: Long?, recurrenceType: String): Long? {
        if (currentDueDate == null) return null

        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentDueDate

        when (recurrenceType) {
            "DAILY" -> calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            "WEEKLY" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(java.util.Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(java.util.Calendar.YEAR, 1)
        }

        return calendar.timeInMillis
    }

    // Delete all instances of a recurring task
    suspend fun deleteAllRecurringInstances(recurrenceParentId: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val tasksSnapshot = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .whereEqualTo("recurrenceParentId", recurrenceParentId)
                .get()
                .await()

            val originalTaskSnapshot = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(recurrenceParentId)
                .get()
                .await()

            val batch = firestore.batch()

            tasksSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            if (originalTaskSnapshot.exists()) {
                batch.delete(originalTaskSnapshot.reference)
            }

            batch.commit().await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Helper to filter tasks to show only next occurrence of recurring tasks
    private fun filterToNextOccurrences(tasks: List<FirebaseTask>): List<FirebaseTask> {
        // Group recurring tasks by their parent ID
        val recurringGroups = tasks
            .filter { it.isRecurring && it.recurrenceParentId != null }
            .groupBy { it.recurrenceParentId!! }

        // For each group, keep only the task with the earliest due date
        val nextOccurrences = recurringGroups.mapNotNull { (_, group) ->
            group.minByOrNull { it.dueDate ?: Long.MAX_VALUE }
        }

        // Get all non-recurring tasks
        val nonRecurring = tasks.filter { !it.isRecurring }

        // Combine and sort by due date
        return (nextOccurrences + nonRecurring).sortedBy { it.dueDate ?: Long.MAX_VALUE }
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

    suspend fun deleteSingleRecurringOccurrence(taskId: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val taskDoc = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .get()
                .await()

            val task = taskDoc.toObject(FirebaseTask::class.java)?.copy(id = taskId)

            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .delete()
                .await()

            if (task?.isRecurring == true && task.recurrenceType != null && task.dueDate != null) {
                val nextDueDate = calculateNextOccurrence(task.dueDate, task.recurrenceType)

                val nextTask = task.copy(
                    id = null,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = Date(),
                    dueDate = nextDueDate,
                    recurrenceParentId = task.recurrenceParentId ?: taskId
                )

                addTask(nextTask)
            }

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun completeTask(taskId: String, experienceReward: Int = 10, moneyReward: Int = 10): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val taskDoc = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .get()
                .await()

            val task = taskDoc.toObject(FirebaseTask::class.java)?.copy(id = taskId)

            val updates = mapOf(
                "isCompleted" to true,
                "completedAt" to Date(),
                "experienceReward" to experienceReward,
                "moneyReward" to moneyReward
            )
            updateTask(taskId, updates)

            if (task?.isRecurring == true && task.recurrenceType != null && task.dueDate != null) {
                val nextDueDate = calculateNextOccurrence(task.dueDate, task.recurrenceType)

                val nextTask = task.copy(
                    id = null,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = Date(),
                    dueDate = nextDueDate,
                    recurrenceParentId = task.recurrenceParentId ?: taskId
                )

                addTask(nextTask)
            }

            val profileResult = getUserProfile()
            if (profileResult is FirebaseResult.Success) {
                val profile = profileResult.data

                val newExperience = profile.playerExperience + experienceReward
                val newMoney = profile.playerMoney + moneyReward
                val newLevel = (newExperience / 100) + 1

                val needsReset = isNewWeek(profile.weekStartDate)

                val updatedProfile = if (needsReset) {
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
                    profile.copy(
                        playerExperience = newExperience,
                        playerMoney = newMoney,
                        playerLevel = newLevel,
                        weeklyTasksCompleted = profile.weeklyTasksCompleted + 1,
                        lifetimeTasksCompleted = profile.lifetimeTasksCompleted + 1,
                        lastUpdated = Date()
                    )
                }

                saveUserProfile(updatedProfile)
            }

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun addTag(taskId: String, vararg tags: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        if (tags.isEmpty()) {
            return FirebaseResult.Error(IllegalArgumentException("No tags supplied"))
        }

        return try {
            val updates = mapOf(
                "tags" to FieldValue.arrayUnion(*tags),
                "lastUpdated" to Date()
            )
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .update(updates)
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun removeTag(taskId: String, vararg tags: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        if (tags.isEmpty()) {
            return FirebaseResult.Error(IllegalArgumentException("No tags supplied"))
        }

        return try {
            val updates = mapOf(
                "tags" to FieldValue.arrayRemove(*tags),
                "lastUpdated" to Date()
            )
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId)
                .update(updates)
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun setDueDateMillis(taskId: String, dueDateMillis: Long?): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        val updates = if (dueDateMillis != null) {
            mapOf("dueDate" to dueDateMillis, "lastUpdated" to Date())
        } else {
            mapOf("dueDate" to FieldValue.delete(), "lastUpdated" to Date())
        }

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("tasks")
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

                            // Filter to show only next occurrence of recurring tasks
                            val filteredTasks = filterToNextOccurrences(tasks)

                            trySend(FirebaseResult.Success(filteredTasks))
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
        if (userId == null) return flowOf(FirebaseResult.Error(Exception("User not authenticated")))

        return callbackFlow {
            val nowMillis = System.currentTimeMillis()
            val listener = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .whereEqualTo("isCompleted", false)
                .whereGreaterThanOrEqualTo("dueDate", nowMillis)   // ← use dueDate (Long)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .limit(limit)
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

    // Get tag statistics
    suspend fun getTagStatistics(): FirebaseResult<Map<String, Int>> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val tasksSnapshot = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .whereEqualTo("isCompleted", true)
                .get()
                .await()

            val tagCounts = mutableMapOf<String, Int>()

            tasksSnapshot.documents.forEach { doc ->
                val task = doc.toObject(FirebaseTask::class.java)
                task?.tags?.forEach { tag ->
                    tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
                }
            }

            FirebaseResult.Success(tagCounts)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // ==================== FRIEND REQUEST OPERATIONS (SUBCOLLECTION) ====================
    suspend fun searchUser(playerNameWithDiscriminator: String): FirebaseResult<UserProfile> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Parse username and discriminator (e.g., "Bob#1234")
            val parts = playerNameWithDiscriminator.split("#")
            if (parts.size != 2) {
                return FirebaseResult.Error(Exception("Invalid format. Use Username#1234"))
            }

            val targetUsername = parts[0]
            val targetDiscriminator = parts[1]

            if (targetDiscriminator.length != 4 || !targetDiscriminator.all { it.isDigit() }) {
                return FirebaseResult.Error(Exception("Invalid discriminator. Must be 4 digits"))
            }

            // Search for user by username AND discriminator
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("playerName", targetUsername)
                .whereEqualTo("discriminator", targetDiscriminator)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return FirebaseResult.Error(Exception("Player not found"))
            }

            val targetUserDoc = querySnapshot.documents.first()
            val targetUser = targetUserDoc.toObject(UserProfile::class.java)
                ?: return FirebaseResult.Error(Exception("Failed to parse user profile"))

            if (targetUser.userId == userId) {
                return FirebaseResult.Error(Exception("You cannot add yourself as a friend"))
            }

            FirebaseResult.Success(targetUser)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun sendFriendRequest(playerNameWithDiscriminator: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Parse username and discriminator (e.g., "Bob#1234")
            val parts = playerNameWithDiscriminator.split("#")
            if (parts.size != 2) {
                return FirebaseResult.Error(Exception("Invalid format. Use Username#1234"))
            }

            val targetUsername = parts[0]
            val targetDiscriminator = parts[1]

            if (targetDiscriminator.length != 4 || !targetDiscriminator.all { it.isDigit() }) {
                return FirebaseResult.Error(Exception("Invalid discriminator. Must be 4 digits"))
            }

            // Get current user's profile
            val currentUserProfile = when (val result = getUserProfile()) {
                is FirebaseResult.Success -> result.data
                is FirebaseResult.Error -> throw result.exception
                else -> throw Exception("Failed to get current user profile")
            }

            // Search for user by username AND discriminator
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("playerName", targetUsername)
                .whereEqualTo("discriminator", targetDiscriminator)
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

            // Create friend request
            val friendRequest = mapOf(
                "fromUserId" to userId,
                "fromUserName" to "${currentUserProfile.playerName}#${currentUserProfile.discriminator}",
                "fromProfileImageUrl" to currentUserProfile.profileImageUrl,
                "fromSelectedFrame" to currentUserProfile.selectedFrame,
                "toUserId" to targetUser.userId,
                "toUserName" to "${targetUser.playerName}#${targetUser.discriminator}",
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
                "selectedFrame" to fromUser.selectedFrame,
                "selectedTitle" to fromUser.selectedTitle,
                "createdAt" to Date()
            )

            // Create friendship for Person A (sender/fromUser)
            val friendship2 = mapOf(
                "friendId" to request.toUserId,
                "friendName" to request.toUserName,
                "friendProfileImageUrl" to toUser.profileImageUrl,
                "selectedFrame" to toUser.selectedFrame,
                "selectedTitle" to toUser.selectedTitle,
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

    // Remove a friend (deletes friendship from both users)
    suspend fun removeFriend(friendId: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Find and delete friendship document from current user's friends subcollection
            val currentUserFriendships = firestore.collection("users")
                .document(userId)
                .collection("friends")
                .whereEqualTo("friendId", friendId)
                .get()
                .await()

            currentUserFriendships.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            // Find and delete friendship document from friend's friends subcollection
            val friendUserFriendships = firestore.collection("users")
                .document(friendId)
                .collection("friends")
                .whereEqualTo("friendId", userId)
                .get()
                .await()

            friendUserFriendships.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
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

    suspend fun createLongTermGoal(
        name: String,
        description: String?,
        tags: List<String>,
        size: String
    ): FirebaseResult<String> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val config = GoalSize.getConfig(size)
            val todayString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(Date())

            val goal = LongTermGoal(
                userId = userId,
                name = name,
                description = description,
                tags = tags,
                size = size,
                targetDays = config.targetDays,
                completedDays = 0,
                completedDates = emptyList(),
                strikes = 0,
                lastCheckedDate = todayString,
                totalWeeks = config.weeks,
                rewardXP = config.rewardXP,
                rewardMoney = config.rewardMoney,
                createdAt = Date()
            )

            val goalMap = mapOf(
                "userId" to goal.userId,
                "name" to goal.name,
                "description" to goal.description,
                "tags" to goal.tags,
                "size" to goal.size,
                "targetDays" to goal.targetDays,
                "completedDays" to goal.completedDays,
                "completedDates" to goal.completedDates,
                "strikes" to goal.strikes,
                "lastCheckedDate" to goal.lastCheckedDate,
                "weeklyProgress" to goal.weeklyProgress,
                "currentWeek" to goal.currentWeek,
                "totalWeeks" to goal.totalWeeks,
                "createdAt" to goal.createdAt,
                "completedAt" to goal.completedAt,
                "isCompleted" to goal.isCompleted,
                "rewardXP" to goal.rewardXP,
                "rewardMoney" to goal.rewardMoney
            )

            val docRef = firestore.collection("users")
                .document(userId)
                .collection("longTermGoals")
                .add(goalMap)
                .await()

            FirebaseResult.Success(docRef.id)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun updateLongTermGoal(goalId: String, updates: Map<String, Any>): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("longTermGoals")
                .document(goalId)
                .update(updates + ("lastUpdated" to Date()))
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    suspend fun deleteLongTermGoal(goalId: String): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("longTermGoals")
                .document(goalId)
                .delete()
                .await()

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Observe all long-term goals for the current user
    fun observeLongTermGoals(): Flow<FirebaseResult<List<LongTermGoal>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return flowOf(FirebaseResult.Error(Exception("User not authenticated")))
        }

        return callbackFlow {
            val listener = firestore.collection("users")
                .document(userId)
                .collection("longTermGoals")
                .whereEqualTo("isCompleted", false) // Only show active goals
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(FirebaseResult.Error(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val goals = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(LongTermGoal::class.java)?.copy(id = doc.id)
                            }
                            trySend(FirebaseResult.Success(goals))
                        } catch (e: Exception) {
                            trySend(FirebaseResult.Error(e))
                        }
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    // Update goal progress when a task is completed
    // Goals now track days - user must complete at least one task with a matching tag per day
    // Returns a list of completed goals (for UI popup)
    suspend fun updateGoalProgressForTask(taskTags: List<String>): FirebaseResult<List<CompletedGoalInfo>> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            // Get today's date as string (format: YYYY-MM-DD)
            val todayString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(Date())

            // Get all active goals
            val goalsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("longTermGoals")
                .whereEqualTo("isCompleted", false)
                .get()
                .await()

            val goals = goalsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(LongTermGoal::class.java)?.copy(id = doc.id)
            }

            // Find goals with matching tags
            val matchingGoals = goals.filter { goal ->
                goal.tags.any { tag -> taskTags.contains(tag) }
            }

            // Track completed goals for UI popup
            val completedGoals = mutableListOf<CompletedGoalInfo>()

            // Update each matching goal
            for (goal in matchingGoals) {
                val currentWeek = calculateWeekNumber(goal.createdAt ?: Date(), Date())

                // Skip if goal is already complete
                if (goal.isCompleted || goal.completedDays >= goal.targetDays) {
                    continue
                }

                // Check if today has already been counted for this goal
                if (goal.completedDates.contains(todayString)) {
                    // Already progressed this goal today, skip
                    continue
                }

                // Add today to completed dates and increment days count
                val newCompletedDays = goal.completedDays + 1

                // Update weekly progress (track days per week)
                val weeklyProgress = goal.weeklyProgress.toMutableMap()
                val weekKey = "week_$currentWeek"
                weeklyProgress[weekKey] = (weeklyProgress[weekKey] ?: 0) + 1

                // Check if goal is now complete (reached target days)
                val isCompleted = newCompletedDays >= goal.targetDays

                // Build updates map
                val updates = mutableMapOf<String, Any>(
                    "completedDays" to newCompletedDays,
                    "completedDates" to FieldValue.arrayUnion(todayString),
                    "weeklyProgress" to weeklyProgress,
                    "currentWeek" to currentWeek
                )

                if (isCompleted) {
                    updates["isCompleted"] = true
                    updates["completedAt"] = Date()

                    // Calculate adjusted rewards based on strikes (25% penalty per strike)
                    val strikeMultiplier = 1.0f - (goal.strikes * 0.25f)
                    val adjustedXP = (goal.rewardXP * strikeMultiplier).toInt()
                    val adjustedMoney = (goal.rewardMoney * strikeMultiplier).toInt()

                    // Award completion rewards (adjusted for strikes)
                    val profileResult = getUserProfile()
                    if (profileResult is FirebaseResult.Success) {
                        val profile = profileResult.data
                        val updatedProfile = profile.copy(
                            playerExperience = profile.playerExperience + adjustedXP,
                            playerMoney = profile.playerMoney + adjustedMoney,
                            playerLevel = ((profile.playerExperience + adjustedXP) / 100) + 1
                        )
                        saveUserProfile(updatedProfile)
                    }

                    // Add to completed goals list for UI popup
                    completedGoals.add(
                        CompletedGoalInfo(
                            goalName = goal.name,
                            strikes = goal.strikes,
                            experienceReward = adjustedXP,
                            moneyReward = adjustedMoney
                        )
                    )
                }

                updateLongTermGoal(goal.id, updates)
            }

            FirebaseResult.Success(completedGoals)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    // Helper function to calculate which week we're in
    private fun calculateWeekNumber(startDate: Date, currentDate: Date): Int {
        val millisInWeek = 7L * 24 * 60 * 60 * 1000
        val diffMillis = currentDate.time - startDate.time
        return (diffMillis / millisInWeek).toInt()
    }

    // Check for missed days and add strikes to goals
    // Called when goals are loaded to ensure strikes are up to date
    suspend fun checkAndUpdateStrikes(): FirebaseResult<Unit> {
        val userId = getCurrentUserId() ?: return FirebaseResult.Error(Exception("User not authenticated"))

        return try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val todayString = dateFormat.format(Date())
            val today = dateFormat.parse(todayString) ?: Date()

            // Get all active goals
            val goalsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("longTermGoals")
                .whereEqualTo("isCompleted", false)
                .get()
                .await()

            val goals = goalsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(LongTermGoal::class.java)?.copy(id = doc.id)
            }

            for (goal in goals) {
                // Skip if already at max strikes
                if (goal.strikes >= 3) {
                    // Just update lastCheckedDate
                    if (goal.lastCheckedDate != todayString) {
                        updateLongTermGoal(goal.id, mapOf("lastCheckedDate" to todayString))
                    }
                    continue
                }

                // Get the last checked date, or use creation date if never checked
                val lastCheckedString = goal.lastCheckedDate
                    ?: dateFormat.format(goal.createdAt ?: Date())
                val lastChecked = dateFormat.parse(lastCheckedString) ?: Date()

                // Calculate days between last check and today
                val millisInDay = 24L * 60 * 60 * 1000
                val daysBetween = ((today.time - lastChecked.time) / millisInDay).toInt()

                // If more than 1 day has passed, check for missed days
                if (daysBetween > 1) {
                    var missedDays = 0
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = lastChecked

                    // Check each day between lastChecked and today (exclusive of both)
                    for (i in 1 until daysBetween) {
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                        val checkDateString = dateFormat.format(calendar.time)

                        // If this date is not in completedDates, it's a missed day
                        if (!goal.completedDates.contains(checkDateString)) {
                            missedDays++
                        }
                    }

                    // Add strikes for missed days (cap at 3 total)
                    if (missedDays > 0) {
                        val newStrikes = minOf(goal.strikes + missedDays, 3)
                        updateLongTermGoal(goal.id, mapOf(
                            "strikes" to newStrikes,
                            "lastCheckedDate" to todayString
                        ))
                    } else {
                        // Just update lastCheckedDate
                        updateLongTermGoal(goal.id, mapOf("lastCheckedDate" to todayString))
                    }
                } else if (goal.lastCheckedDate != todayString) {
                    // Update lastCheckedDate even if no days missed
                    updateLongTermGoal(goal.id, mapOf("lastCheckedDate" to todayString))
                }
            }

            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }
}

