package com.example.overpowered.progress


import com.example.overpowered.data.UserProfile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.style.TextAlign
import com.example.overpowered.data.GoalSize
import com.example.overpowered.data.LongTermGoal

// ---------- UI models  ----------
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

    // ---------- UI ----------
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats card
        item {
            StatsSummaryCard(playerStats)
        }
        item {
            LongTermGoalsSection(
                goals = longTermGoals,
                onCreateGoal = { name, desc, tags, size ->
                    viewModel.createLongTermGoal(name, desc, tags, size)
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
fun StatsSummaryCard(stats: PlayerStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Progress Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatPill(Icons.Filled.PlayArrow, "EXP",  stats.exp.toString(),  Color(0xFF667EEA))
                StatPill(Icons.Filled.Star,      "LVL",  stats.level.toString(), Color(0xFF38B2AC))
                StatPill(Icons.Filled.ShoppingCart, "Gold", stats.gold.toString(), Color(0xFFF6AD55))
            }

            val progress = (stats.exp.toFloat() / stats.expForNextLevel.toFloat()).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Next Level", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = Color(0xFFE2E8F0),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Text("${stats.exp} / ${stats.expForNextLevel} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF4A5568))
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Surface(color = Color.White, tonalElevation = 2.dp, shadowElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.widthIn(min = 96.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint)
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF718096))
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GoalsCard(goals: List<Goal>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Long-term Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            goals.forEach { goal ->
                GoalRow(goal)
                Divider(color = Color(0xFFE2E8F0))
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
            Text("${goal.current}/${goal.target} ${goal.unit}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4A5568))
        }
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(10.dp), trackColor = Color(0xFFE2E8F0))
    }
}

@Composable
fun TaskHistoryCard(history: List<TaskHistoryItem>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Task History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (history.isEmpty()) {
                Text("No tasks completed yet.", color = Color(0xFF718096))
            } else {
                history.forEach { item ->
                    HistoryRow(item)
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = Color(0xFFE2E8F0)
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
            Text(item.date.toString(), style = MaterialTheme.typography.labelMedium, color = Color(0xFF718096))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("+${item.rewardExp} XP", style = MaterialTheme.typography.labelLarge, color = Color(0xFF667EEA))
            Text("+${item.rewardGold} $", style = MaterialTheme.typography.labelLarge, color = Color(0xFFF6AD55))
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
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
                        color = Color(0xFF667EEA)
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
                        color = Color(0xFF4A5568)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add friends to compete!",
                        fontSize = 14.sp,
                        color = Color(0xFF718096)
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
                            color = Color(0xFFE2E8F0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Your Rank",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF718096)
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
            .background(Color(0xFFE2E8F0))
    ) {
        options.forEachIndexed { index, option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (selectedIndex == index) Color(0xFF667EEA) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectionChange(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 12.sp,
                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedIndex == index) Color.White else Color(0xFF4A5568)
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
                if (isCurrentUser) Color(0xFF667EEA).copy(alpha = 0.1f) else Color.Transparent,
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
                        else -> Color(0xFFE2E8F0)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.rank.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.rank <= 3) Color.White else Color(0xFF4A5568)
            )
        }

        // Profile picture
        Card(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF667EEA))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (entry.profileImageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(entry.profileImageUrl),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Name
        Text(
            text = entry.playerName,
            fontSize = 14.sp,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            color = Color(0xFF4A5568),
            modifier = Modifier.weight(1f)
        )

        // Stat
        Text(
            text = if (showLevel) "Lv ${entry.level}" else "${entry.tasksCompleted} tasks",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF667EEA)
        )
    }
}

// ---------- Lightweight placeholders (commented as non-basic) ----------
@Composable private fun LoadingStatsCard() {
    // Non-basic: skeleton shimmer is nicer; this is a minimal placeholder
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))) {
        Column(Modifier.padding(16.dp)) {
            Text("Progress Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(10.dp))
            Spacer(Modifier.height(8.dp))
            Text("Loading stats…", color = Color(0xFF718096))
        }
    }
}
@Composable private fun ErrorStatsCard() {
    // Non-basic: add retry action, analytics, etc.
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
        Column(Modifier.padding(16.dp)) {
            Text("Progress Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Couldn’t load stats.", color = Color(0xFFB00020))
        }
    }
}
@Composable private fun LoadingHistoryCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))) {
        Column(Modifier.padding(16.dp)) {
            Text("Task History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(10.dp))
            Spacer(Modifier.height(8.dp))
            Text("Loading history…", color = Color(0xFF718096))
        }
    }
}
@Composable private fun ErrorHistoryCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
        Column(Modifier.padding(16.dp)) {
            Text("Task History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Couldn’t load history.", color = Color(0xFFB00020))
        }
    }
}

@Composable
fun LongTermGoalsSection(
    goals: List<LongTermGoal>,
    onCreateGoal: (String, String?, List<String>, String) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Long Term Goals",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A5568)
            )

            IconButton(
                onClick = { showCreateDialog = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF667EEA)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Goal",
                    tint = Color.White
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
                        onDelete = { onDeleteGoal(goal.id) }
                    )
                }
            }
        }
    }

    // Create Goal Dialog
    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, tags, size ->
                onCreateGoal(name, desc, tags, size)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun EmptyGoalsState(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
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
                text = "No Long Term Goals Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create a goal and complete tasks with matching tags to track your progress!",
                fontSize = 14.sp,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
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
    onDelete: () -> Unit
) {
    val config = GoalSize.getConfig(goal.size)
    val progress = goal.currentPoints.toFloat() / goal.targetPoints.toFloat()
    val weeksElapsed = goal.currentWeek + 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
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
                        color = Color(0xFF4A5568)
                    )
                    goal.description?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = Color(0xFF718096),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFFED7D7),
                        contentColor = Color(0xFFE53E3E)
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
                            color = Color(0xFF667EEA).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF667EEA)
                            )
                        }
                    }
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
                        text = "${goal.currentPoints} / ${goal.targetPoints} points",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF667EEA)
                    )
                }

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFF667EEA),
                    trackColor = Color(0xFFE2E8F0)
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
                        color = Color(0xFF718096)
                    )
                    Text(
                        text = "$weeksElapsed / ${goal.totalWeeks} weeks",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rewards",
                        fontSize = 12.sp,
                        color = Color(0xFF718096)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "+${goal.rewardXP} XP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFED8936)
                        )
                        Text(
                            text = "+$${goal.rewardMoney}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF48BB78)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?, tags: List<String>, size: String) -> Unit
) {
    var goalName by remember { mutableStateOf("") }
    var goalDescription by remember { mutableStateOf("") }
    var goalTags by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf(GoalSize.SHORT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Create Long Term Goal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A5568)
                )
                Text(
                    text = "Track progress by completing tasks with matching tags",
                    fontSize = 14.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(top = 4.dp)
                )
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
                        focusedBorderColor = Color(0xFF667EEA),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
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
                        focusedBorderColor = Color(0xFF667EEA),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
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
                            color = Color(0xFF718096)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667EEA),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Goal Size Selection
                Column {
                    Text(
                        text = "Goal Duration",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568),
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (goalName.isNotBlank() && goalTags.isNotBlank()) {
                        val tags = goalTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onCreate(
                            goalName,
                            goalDescription.takeIf { it.isNotBlank() },
                            tags,
                            selectedSize
                        )
                    }
                },
                enabled = goalName.isNotBlank() && goalTags.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF718096)
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
            containerColor = if (isSelected) Color(0xFF667EEA).copy(alpha = 0.1f) else Color(0xFFF7FAFC)
        ),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF667EEA)) else null
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
                    color = if (isSelected) Color(0xFF667EEA) else Color(0xFF4A5568)
                )
                Text(
                    text = "${config.weeks} weeks • ${config.points} points",
                    fontSize = 14.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Rewards: ${config.rewardXP} XP, $${config.rewardMoney}",
                    fontSize = 12.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF667EEA)
                )
            )
        }
    }
}

