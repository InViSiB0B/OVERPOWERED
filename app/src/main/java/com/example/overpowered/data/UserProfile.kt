package com.example.overpowered.data

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

enum class RecurrenceType(val displayName: String, val value: String) {
    DAILY("Daily", "DAILY"),
    WEEKLY("Weekly", "WEEKLY"),
    MONTHLY("Monthly", "MONTHLY"),
    YEARLY("Yearly", "YEARLY");

    companion object {
        fun fromValue(value: String?): RecurrenceType? {
            return values().find { it.value == value }
        }
    }
}

// User profile data model for Firebase
data class UserProfile(
    val userId: String = "",
    val playerName: String = "Player Name",
    val discriminator: String = "0000", // 4-digit identifier
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

    @get:PropertyName("isOnboarded") @set:PropertyName("isOnboarded")
    var isOnboarded: Boolean = false,  // Change to var and add annotation

    val phoneNumber: String? = null,



    @ServerTimestamp
    val lastUpdated: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)

// Track username+discriminator combinations
data class UsernameDiscriminator(
    val username: String = "",
    val discriminator: String = "",
    val userId: String = "",
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

    @get:PropertyName("isRecurring") @set:PropertyName("isRecurring")
    var isRecurring: Boolean = false,
    val recurrenceType: String? = null, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val recurrenceParentId: String? = null, // Links all instances of a recurring task
)


// Friend request data model
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromProfileImageUrl: String? = null,
    val fromSelectedFrame: String? = null,
    val fromSelectedTitle: String? = null,
    val toUserId: String = "",
    val toUserName: String = "",
    val status: String = "pending", // "pending", "accepted", "ignored"
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
    val selectedFrame: String? = null,
    val selectedTitle: String? = null,
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

    // Progress tracking - days-based
    val targetDays: Int = 20,           // Total days needed to complete goal
    val completedDays: Int = 0,         // Number of days with at least one matching task completed
    val completedDates: List<String> = emptyList(), // Dates when goal was progressed (format: "YYYY-MM-DD")

    // Strike system - strikes accrue when user misses a day
    val strikes: Int = 0,               // Number of strikes (max 3), each reduces reward by 25%
    val lastCheckedDate: String? = null, // Last date the goal was checked for missed days (format: "YYYY-MM-DD")

    // Weekly tracking
    val weeklyProgress: Map<String, Int> = emptyMap(), // "week_0" -> days completed that week
    val currentWeek: Int = 0,
    val totalWeeks: Int = 4,

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
        val targetDays: Int,  // Number of days user must complete a matching task
        val weeks: Int,
        val rewardXP: Int,
        val rewardMoney: Int
    )

    fun getConfig(size: String): GoalConfig {
        return when (size) {
            // Short: 1 month (30 days) - 5x return on 100 coin investment
            SHORT -> GoalConfig("Short", 30, 4, 500, 500)
            // Medium: 3 months (90 days) - 20x return on 100 coin investment
            MEDIUM -> GoalConfig("Medium", 90, 13, 2000, 2000)
            // Long: 6 months (180 days) - 50x return on 100 coin investment
            LONG -> GoalConfig("Long", 180, 26, 5000, 5000)
            else -> GoalConfig("Short", 30, 4, 500, 500)
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