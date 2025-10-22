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
    val weeklyTasksCompleted: Int = 0,
    val weekStartDate: Date? = null,
    val lifetimeTasksCompleted: Int = 0,
    val tags: List<String> = emptyList(),


    @ServerTimestamp
    val lastUpdated: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)

// Task data model for Firebase
data class FirebaseTask(
    val id: String? = "",
    val title: String = "",
    val description: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Date? = null,
    val completedAt: Date? = null,
    val userId: String? = "",
    val experienceReward: Int = 10,
    val moneyReward: Int = 10,
    val tags: List<String> = emptyList(),
    val dueDate: Long? = null,
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

// LongTermGoal data model
data class LongTermGoal(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String? = null,
    val tags: List<String> = emptyList(),

    // Goal size
    val size: String = "SHORT", // "SHORT", "MEDIUM", "LONG"

    // Progress tracking
    val targetPoints: Int = 300,
    val currentPoints: Int = 0,
    val weeklyTargetPoints: Int = 75, // targetPoints / totalWeeks

    // Weekly tracking
    val weeklyProgress: Map<String, Int> = emptyMap(), // "week_0" -> points, "week_1" -> points
    val currentWeek: Int = 0,
    val totalWeeks: Int = 4,

    // Task tracking
    val completedTaskIds: List<String> = emptyList(),

    // Metadata
    @ServerTimestamp
    val createdAt: Date? = null,
    val completedAt: Date? = null,
    val isCompleted: Boolean = false,

    // Rewards
    val rewardXP: Int = 100,
    val rewardMoney: Int = 100,


    @ServerTimestamp
    val lastUpdated: Date? = null
)

// Helper object for goal size constraints
object GoalSize {
    const val SHORT = "SHORT"
    const val MEDIUM = "MEDIUM"
    const val LONG = "LONG"

    data class GoalConfig(
        val displayName: String,
        val points: Int,
        val weeks: Int,
        val rewardXP: Int,
        val rewardMoney: Int
    )

    fun getConfig(size: String): GoalConfig {
        return when (size) {
            SHORT -> GoalConfig("Short", 300, 4, 100, 100)
            MEDIUM -> GoalConfig("Medium", 900, 13, 500, 500)
            LONG -> GoalConfig("Long", 3600, 52, 2000, 2000)
            else -> GoalConfig("Short", 300, 4, 100, 100)
        }
    }
}


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