package com.example.overpowered.data

import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDate
import java.util.*

// User profile data model for Firebase
data class UserProfile(
    val userId: String = "",
    val playerName: String = "Player Name",
    val profileImageUrl: String? = null, // Firebase Storage URL
    val playerExperience: Int = 0,
    val playerLevel: Int = 1,
    val playerMoney: Int = 100,
    val purchasedItems: List<String> = emptyList(),
    val selectedFrame: String? = null,
    val selectedTitle: String? = null,
    val selectedTheme: String? = null,
    @ServerTimestamp
    val lastUpdated: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)

// Task data model for Firebase
data class FirebaseTask(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Date? = null,
    val completedAt: Date? = null,
    val userId: String = "",
    val tags: List<String> = emptyList(),
    val deadline: Date? = null
)

// Friend request data model
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val toUserId: String = "",
    val toUserName: String = "",
    val status: String = "pending", // "pending, "accepted", "ignored"
    @ServerTimestamp
    val createdAt: Date? = null
)

// Friendship data model
data class Friendship(
    val id: String = "",
    val userId: String = "",
    val friendId: String = "",
    val friendName: String = "",
    val friendProfileImageUrl: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)

// App state wrapper for easy syncing
data class AppState(
    val profile: UserProfile = UserProfile(),
    val tasks: List<FirebaseTask> = emptyList()
)

// Result wrapper for async operations
sealed class FirebaseResult<out T> {
    data class Success<T>(val data: T) : FirebaseResult<T>()
    data class Error(val exception: Exception) : FirebaseResult<Nothing>()
    object Loading : FirebaseResult<Nothing>()





}