package com.example.overpowered.data

import com.google.firebase.firestore.ServerTimestamp
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
    val createdAt: Date? = null, //
    val completedAt: Date? = null,
    val userId: String = ""
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