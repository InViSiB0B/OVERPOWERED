package com.example.overpowered.navigation

import com.example.overpowered.data.FirebaseRepository
import com.example.overpowered.data.FirebaseResult
import com.example.overpowered.data.UserProfile
import com.example.overpowered.data.FirebaseTask

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
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    val id: String,
    val title: String,
    val date: LocalDate,
    val rewardExp: Int,
    val rewardGold: Int
)

// ---------- Screen ----------
@Composable
fun RewardsScreen(
    repo: FirebaseRepository = remember { FirebaseRepository() }
) {
    LaunchedEffect(Unit) {
        repo.observeUserProfile().collect { /* update state */ }
    }
    LaunchedEffect(Unit) {
        repo.observeCompletedTasks().collect { /* update state */ }
    }

    // Observe profile - stats
    val profileFlowState = remember { mutableStateOf<FirebaseResult<UserProfile>>(FirebaseResult.Loading) }
    LaunchedEffect(repo.getCurrentUserId()) {
        // Only attach listener when we have a user
            repo.observeUserProfile().collectLatest { profileFlowState.value = it }
    }

    val playerStats: PlayerStats? = (profileFlowState.value as? FirebaseResult.Success)
        ?.data
        ?.toPlayerStatsForProgress()

    // Observe completed tasks - history list
    val historyState = remember { mutableStateOf<FirebaseResult<List<FirebaseTask>>>(FirebaseResult.Loading) }
    LaunchedEffect(repo.getCurrentUserId()) {
        if (repo.getCurrentUserId() != null) {
            repo.observeCompletedTasks().collectLatest { historyState.value = it }
        }
    }

    val taskHistoryItems: List<TaskHistoryItem> = (historyState.value as? FirebaseResult.Success)
        ?.data
        ?.map { it.toHistoryItem() }
        ?: emptyList()

    // Goals mocked
    val goals = remember {
        listOf(
            Goal("g1", "Complete 100 tasks", current = taskHistoryItems.size, target = 100, unit = "tasks"),
            Goal("g2", "Earn 5000 XP", current = playerStats?.level?.times(100)?.plus(playerStats.exp) ?: 0, target = 5000, unit = "XP"),
            Goal("g3", "Save 1000 gold", current = playerStats?.gold ?: 0, target = 1000, unit = "gold")
        )
    }

    // ---------- UI ----------
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats card (profile-backed)
        item {
            when (val pr = profileFlowState.value) {
                is FirebaseResult.Loading -> LoadingStatsCard()
                is FirebaseResult.Error   -> ErrorStatsCard()
                is FirebaseResult.Success -> StatsSummaryCard(playerStats!!) // success implies non-null
            }
        }

//        // Goals (mock for now)
//        item { GoalsCard(goals) }
//
        // History card (completed tasks)
        item {
            when (val hs = historyState.value) {
                is FirebaseResult.Loading -> LoadingHistoryCard()
                is FirebaseResult.Error   -> ErrorHistoryCard()
                is FirebaseResult.Success -> TaskHistoryCard(taskHistoryItems)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---------- Mapping helpers ----------
private fun UserProfile.toPlayerStatsForProgress(): PlayerStats {
    // Basic: linear leveling — each level requires 100 XP (upgrade later if you want a curve)
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
    // Non-basic: reward values are placeholders until your task model stores rewards
    return TaskHistoryItem(
        id = id,
        title = title.ifBlank { "Completed task" },
        date = localDate,
        rewardExp = 0,   // <-- comment: replace when you add rewards to FirebaseTask
        rewardGold = 0   // <-- comment: replace when you add rewards to FirebaseTask
    )
}

// ---------- Cards (same look as before) ----------
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
