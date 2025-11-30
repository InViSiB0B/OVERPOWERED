package com.example.overpowered.progress


import com.example.overpowered.data.UserProfile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overpowered.viewmodel.AppViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.overpowered.data.FirebaseTask
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.overpowered.data.GoalSize
import com.example.overpowered.data.LongTermGoal
import com.example.overpowered.progress.components.StreakCalculator
import com.example.overpowered.profile.components.FramedProfilePicture
import com.example.overpowered.profile.components.CompactPlayerNameWithTitle

// ---------- UI models  -----------
data class PlayerStats(
    val exp: Int,
    val level: Int,
    val gold: Int,
    val expForNextLevel: Int
)

data class Goal(
    val id: String,
    val title: String,
    val current: Int,
    val target: Int,
    val unit: String
)

data class TaskHistoryItem(
    val id: String?,
    val title: String,
    val date: LocalDate,
    val rewardExp: Int,
    val rewardGold: Int
)

// Leaderboard models
data class LeaderboardEntry(
    val userId: String,
    val playerName: String,
    val profileImageUrl: String?,
    val selectedFrame: String?,
    val selectedTitle: String?,
    val level: Int,
    val tasksCompleted: Int,
    val rank: Int
)

enum class LeaderboardTimeframe {
    WEEKLY, LIFETIME
}

enum class LeaderboardRankingType {
    LEVEL, TASKS
}

// ---------- Screen ----------
@Composable
fun ProgressScreen(
    viewModel: AppViewModel
) {
    // Get data from ViewModel
    val userProfile by viewModel.userProfile.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val leaderboardEntries by viewModel.leaderboardEntries.collectAsState()
    val longTermGoals by viewModel.longTermGoals.collectAsState()
    val isLoadingLeaderboard by viewModel.isLoadingLeaderboard.collectAsState()

    // Leaderboard controls
    var selectedTimeframe by remember { mutableStateOf(LeaderboardTimeframe.WEEKLY) }
    var selectedRankingType by remember { mutableStateOf(LeaderboardRankingType.LEVEL) }

    // Load leaderboard when timeframe or ranking changes
    LaunchedEffect(selectedTimeframe, selectedRankingType) {
        viewModel.loadLeaderboard(selectedTimeframe, selectedRankingType)
    }

    // Refresh when completedTasks changes
    LaunchedEffect(completedTasks.size) {
        if (completedTasks.isNotEmpty()) {
            viewModel.loadLeaderboard(selectedTimeframe, selectedRankingType)
        }
    }


    // Convert to UI models
    val playerStats: PlayerStats = userProfile.toPlayerStatsForProgress()
    val taskHistoryItems: List<TaskHistoryItem> = completedTasks.map { it.toHistoryItem() }

    val dailyStreak = remember(taskHistoryItems) {
        StreakCalculator.computeDailyStreak(taskHistoryItems)
    }

    // ---------- UI ----------
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats card
        item {
            StatsSummaryCard(stats = playerStats, dailyStreak = dailyStreak)
        }
        item {
            LongTermGoalsSection(
                goals = longTermGoals,
                playerMoney = userProfile.playerMoney,
                goalCreationCost = AppViewModel.GOAL_CREATION_COST,
                onCreateGoal = { name, desc, tags, size ->
                    viewModel.createLongTermGoal(name, desc, tags, size)
                },
                onUpdateGoal = { goal, name, desc, tags ->
                    viewModel.updateLongTermGoal(goal, name, desc, tags)
                },
                onDeleteGoal = { goalId ->
                    viewModel.deleteLongTermGoal(goalId)
                }
            )
        }
        // Leaderboard card
        item {
            LeaderboardCard(
                entries = leaderboardEntries,
                currentUserId = viewModel.getCurrentUserId() ?: "",
                isLoading = isLoadingLeaderboard,
                selectedTimeframe = selectedTimeframe,
                selectedRankingType = selectedRankingType,
                onTimeframeChange = { selectedTimeframe = it },
                onRankingTypeChange = { selectedRankingType = it }
            )
        }

        // History card
        item {
            TaskHistoryCard(taskHistoryItems)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---------- Mapping helpers ----------
private fun UserProfile.toPlayerStatsForProgress(): PlayerStats {
    // Basic: linear leveling — each level requires 100 XP (upgrade later for curve)
    val expCap = 100
    val expWithinLevel = playerExperience % expCap
    return PlayerStats(
        exp = expWithinLevel,
        level = playerLevel,
        gold = playerMoney,
        expForNextLevel = expCap
    )
}

private fun FirebaseTask.toHistoryItem(): TaskHistoryItem {
    val localDate = (completedAt ?: createdAt)?.let { date ->
        // Non-basic: use java.time formatting i18n; for now just LocalDate
        Instant.ofEpochMilli(date.time).atZone(ZoneId.systemDefault()).toLocalDate()
    } ?: LocalDate.now()
    return TaskHistoryItem(
        id = id,
        title = title.ifBlank { "Completed task" },
        date = localDate,
        rewardExp = experienceReward,
        rewardGold = moneyReward
    )
}

// ---------- Cards ----------
@Composable
fun StatsSummaryCard(stats: PlayerStats, dailyStreak: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Progress Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(icon = Icons.Filled.PlayArrow, label = "EXP", value = stats.exp.toString(), tint = MaterialTheme.colorScheme.primary)
                StatPill(icon = Icons.Filled.Star, label = "LVL", value = stats.level.toString(), tint = MaterialTheme.colorScheme.secondary)
                StatPill(emoji = "🪙", label = "Coins", value = stats.gold.toString(), tint = MaterialTheme.colorScheme.tertiary)
                StatPill(icon = Icons.Filled.Check, label = "Streak", value = dailyStreak.toString(), tint = MaterialTheme.colorScheme.error)
            }

            val progress = (stats.exp.toFloat() / stats.expForNextLevel.toFloat()).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Next Level", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Text("${stats.exp} / ${stats.expForNextLevel} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector? = null,
    emoji: String? = null,
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.widthIn(min = 96.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (emoji != null) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleLarge
                )
            } else if (icon != null) {
                Icon(icon, contentDescription = label, tint = tint)
            }
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
fun GoalsCard(goals: List<Goal>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Long-term Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            goals.forEach { goal ->
                GoalRow(goal)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun GoalRow(goal: Goal) {
    val progress = (goal.current.toFloat() / goal.target.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(goal.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("${goal.current}/${goal.target} ${goal.unit}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
fun TaskHistoryCard(history: List<TaskHistoryItem>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Task History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (history.isEmpty()) {
                Text("No tasks completed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                history.forEach { item ->
                    HistoryRow(item)
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: TaskHistoryItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(item.date.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("+${item.rewardExp} XP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("+🪙${item.rewardGold}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun LeaderboardCard(
    entries: List<LeaderboardEntry>,
    currentUserId: String,
    isLoading: Boolean,
    selectedTimeframe: LeaderboardTimeframe,
    selectedRankingType: LeaderboardRankingType,
    onTimeframeChange: (LeaderboardTimeframe) -> Unit,
    onRankingTypeChange: (LeaderboardRankingType) -> Unit
) {
    // When switching to Level ranking, automatically set to Lifetime
    LaunchedEffect(selectedRankingType) {
        if (selectedRankingType == LeaderboardRankingType.LEVEL && selectedTimeframe == LeaderboardTimeframe.WEEKLY) {
            onTimeframeChange(LeaderboardTimeframe.LIFETIME)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Leaderboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Toggle controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Only show timeframe toggle when Tasks is selected
                if (selectedRankingType == LeaderboardRankingType.TASKS) {
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        options = listOf("Weekly", "Lifetime"),
                        selectedIndex = if (selectedTimeframe == LeaderboardTimeframe.WEEKLY) 0 else 1,
                        onSelectionChange = { index ->
                            onTimeframeChange(
                                if (index == 0) LeaderboardTimeframe.WEEKLY else LeaderboardTimeframe.LIFETIME
                            )
                        }
                    )
                }

                // Ranking type toggle
                SegmentedButton(
                    modifier = Modifier.weight(1f),
                    options = listOf("Level", "Tasks"),
                    selectedIndex = if (selectedRankingType == LeaderboardRankingType.LEVEL) 0 else 1,
                    onSelectionChange = { index ->
                        onRankingTypeChange(
                            if (index == 0) LeaderboardRankingType.LEVEL else LeaderboardRankingType.TASKS
                        )
                    }
                )
            }

            // Leaderboard content
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No friends yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add friends to compete!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Top 10 entries
                    entries.take(10).forEach { entry ->
                        LeaderboardEntryRow(
                            entry = entry,
                            isCurrentUser = entry.userId == currentUserId,
                            showLevel = selectedRankingType == LeaderboardRankingType.LEVEL
                        )
                    }
                    // Show current user if they're not in top 10
                    val currentUserEntry = entries.find { it.userId == currentUserId }
                    if (currentUserEntry != null && currentUserEntry.rank > 10) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Your Rank",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LeaderboardEntryRow(
                            entry = currentUserEntry,
                            isCurrentUser = true,
                            showLevel = selectedRankingType == LeaderboardRankingType.LEVEL
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedButton(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        options.forEachIndexed { index, option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (selectedIndex == index) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectionChange(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 12.sp,
                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedIndex == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LeaderboardEntryRow(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    showLevel: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    when (entry.rank) {
                        1 -> Color(0xFFFFD700) // Gold
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.rank.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.rank <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Profile picture
        Card(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (entry.profileImageUrl != null) {
                    FramedProfilePicture(
                        profileImageUrl = entry.profileImageUrl,
                        frameId = entry.selectedFrame,
                        size = 36.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Name + Title
        CompactPlayerNameWithTitle(
            playerName = entry.playerName,
            titleId = entry.selectedTitle,
            nameSize = 14.sp,
            titleSize = 12.sp,
            nameColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        // Stat
        Text(
            text = if (showLevel) "Lv ${entry.level}" else "${entry.tasksCompleted} tasks",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ---------- Lightweight placeholders (commented as non-basic) ----------
@Composable private fun LoadingStatsCard() {
    // Non-basic: skeleton shimmer is nicer; this is a minimal placeholder
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text("Progress Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(10.dp))
            Spacer(Modifier.height(8.dp))
            Text("Loading stats…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun ErrorStatsCard() {
    // Non-basic: add retry action, analytics, etc.
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Progress Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Couldn’t load stats.", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
@Composable private fun LoadingHistoryCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text("Task History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(10.dp))
            Spacer(Modifier.height(8.dp))
            Text("Loading history…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun ErrorHistoryCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Task History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Couldn’t load history.", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun LongTermGoalsSection(
    goals: List<LongTermGoal>,
    playerMoney: Int,
    goalCreationCost: Int,
    onCreateGoal: (String, String?, List<String>, String) -> Unit,
    onUpdateGoal: (LongTermGoal, String, String?, List<String>) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<LongTermGoal?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<LongTermGoal?>(null) }

    Column(modifier = modifier) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Goals",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { showCreateDialog = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Goal",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Goals List
        if (goals.isEmpty()) {
            EmptyGoalsState(onCreateClick = { showCreateDialog = true })
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                goals.forEach { goal ->
                    LongTermGoalCard(
                        goal = goal,
                        onDelete = {
                            goalToDelete = goal
                            showDeleteDialog = true
                        },
                        onEdit = {
                            editingGoal = goal
                            isEditMode = true
                            showCreateDialog = true
                        }
                    )
                }
            }
        }
    }

    // Create Goal Dialog
    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = {
                showCreateDialog = false
                editingGoal = null
                isEditMode = false
            },
            onCreate = { name, desc, tags, size ->
                if (isEditMode && editingGoal != null) {
                    onUpdateGoal(editingGoal!!, name, desc, tags)
                } else {
                    onCreateGoal(name, desc, tags, size)
                }
                showCreateDialog = false
                editingGoal = null
                isEditMode = false
            },
            initialGoal = editingGoal,
            isEditMode = isEditMode,
            playerMoney = playerMoney,
            goalCreationCost = goalCreationCost
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && goalToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                goalToDelete = null
            },
            title = {
                Text(
                    text = "Delete Goal?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete the \"${goalToDelete!!.name}\" goal? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGoal(goalToDelete!!.id)
                        showDeleteDialog = false
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        goalToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyGoalsState(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Goals Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create a goal and complete tasks with matching tags to track your progress!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Your First Goal")
            }
        }
    }
}

@Composable
fun LongTermGoalCard(
    goal: LongTermGoal,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val config = GoalSize.getConfig(goal.size)
    val progress = goal.completedDays.toFloat() / goal.targetDays.toFloat()
    val weeksElapsed = goal.currentWeek + 1

    // Calculate strike penalty (25% reduction per strike)
    val strikeMultiplier = 1.0f - (goal.strikes * 0.25f)
    val adjustedRewardXP = (goal.rewardXP * strikeMultiplier).toInt()
    val adjustedRewardMoney = (goal.rewardMoney * strikeMultiplier).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* No action on regular click */ },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    goal.description?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Goal",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Tags
            if (goal.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    goal.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Strikes Display (always show to keep user aware)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Strikes:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (goal.strikes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                repeat(3) { index ->
                    Text(
                        text = if (index < goal.strikes) "✕" else "○",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (index < goal.strikes)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (goal.strikes > 0) {
                    Text(
                        text = "(-${(goal.strikes * 25)}% reward)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${goal.completedDays} / ${goal.targetDays} days",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${config.displayName} Goal",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$weeksElapsed / ${goal.totalWeeks} weeks",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rewards",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Show adjusted rewards with strikethrough for original if penalized
                        if (goal.strikes > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+$adjustedRewardXP XP",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+🪙$adjustedRewardMoney",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        } else {
                            Text(
                                text = "+${goal.rewardXP} XP",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "+🪙${goal.rewardMoney}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?, tags: List<String>, size: String) -> Unit,
    initialGoal: LongTermGoal? = null,
    isEditMode: Boolean = false,
    playerMoney: Int = 0,
    goalCreationCost: Int = 100
) {
    val canAfford = playerMoney >= goalCreationCost
    var goalName by remember(initialGoal) { mutableStateOf(initialGoal?.name ?: "") }
    var goalDescription by remember(initialGoal) { mutableStateOf(initialGoal?.description ?: "") }
    var goalTags by remember(initialGoal) { mutableStateOf(initialGoal?.tags?.joinToString(", ") ?: "") }
    var selectedSize by remember(initialGoal) { mutableStateOf(initialGoal?.size ?: GoalSize.SHORT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = if (isEditMode) "Edit Goal" else "Create Goal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track progress by completing tasks with matching tags",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (!isEditMode) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cost: 🪙$goalCreationCost",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        if (!canAfford) {
                            Text(
                                text = " (You have 🪙$playerMoney)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // Strike system warning
                    Text(
                        text = "⚠️ Complete a task with a matching tag daily or receive a strike. Each strike reduces your reward by 25%.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Goal Name
                OutlinedTextField(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g., Get better at guitar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Goal Description
                OutlinedTextField(
                    value = goalDescription,
                    onValueChange = { goalDescription = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("What are you working towards?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Tags
                OutlinedTextField(
                    value = goalTags,
                    onValueChange = { goalTags = it },
                    label = { Text("Related Tags") },
                    placeholder = { Text("guitar, music, practice") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            "Tasks with these tags will count toward this goal",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Goal Size Selection - only show when creating, not editing
                if (!isEditMode) {
                    Column {
                        Text(
                            text = "Goal Duration",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoalSizeOption(
                                size = GoalSize.SHORT,
                                isSelected = selectedSize == GoalSize.SHORT,
                                onClick = { selectedSize = GoalSize.SHORT }
                            )
                            GoalSizeOption(
                                size = GoalSize.MEDIUM,
                                isSelected = selectedSize == GoalSize.MEDIUM,
                                onClick = { selectedSize = GoalSize.MEDIUM }
                            )
                            GoalSizeOption(
                                size = GoalSize.LONG,
                                isSelected = selectedSize == GoalSize.LONG,
                                onClick = { selectedSize = GoalSize.LONG }
                            )
                        }
                    }
                } else {
                    // Show current duration as read-only info when editing
                    val config = GoalSize.getConfig(selectedSize)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Goal Duration",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${config.displayName} (${config.weeks} weeks)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isFormValid = goalName.isNotBlank() && goalTags.isNotBlank()
            val canCreate = isFormValid && (isEditMode || canAfford)

            Button(
                onClick = {
                    if (isFormValid) {
                        val tags = goalTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onCreate(
                            goalName,
                            goalDescription.takeIf { it.isNotBlank() },
                            tags,
                            selectedSize
                        )
                    }
                },
                enabled = canCreate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (!isEditMode) {
                    Text("Create Goal (🪙$goalCreationCost)")
                } else {
                    Text("Update Goal")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GoalSizeOption(
    size: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val config = GoalSize.getConfig(size)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${config.weeks} weeks • ${config.targetDays} days target",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Rewards: ${config.rewardXP} XP, 🪙${config.rewardMoney}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

