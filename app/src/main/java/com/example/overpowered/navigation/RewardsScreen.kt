package com.example.overpowered.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// ---------- Data models + mock data ----------
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
    val unit: String // e.g., "tasks", "days", "XP"
)

data class TaskHistoryItem(
    val id: String,
    val title: String,
    val date: LocalDate,
    val rewardExp: Int,
    val rewardGold: Int
)

@Composable
fun mockPlayerStats() = remember {
    PlayerStats(
        exp = 1450,
        level = 7,
        gold = 320,
        expForNextLevel = 2000
    )
}

@Composable
fun mockGoals() = remember {
    listOf(
        Goal("g1", "Complete 100 tasks", current = 42, target = 100, unit = "tasks"),
        Goal("g2", "Earn 5000 XP", current = 1450, target = 5000, unit = "XP"),
        Goal("g3", "Save 1000 gold", current = 320, target = 1000, unit = "gold")
    )
}

@Composable
fun mockHistory() = remember {
    listOf(
        TaskHistoryItem("t1", "Morning workout", LocalDate.now().minusDays(0), rewardExp = 50, rewardGold = 10),
        TaskHistoryItem("t2", "Deep work: 90 min", LocalDate.now().minusDays(1), rewardExp = 80, rewardGold = 15),
        TaskHistoryItem("t3", "Clean kitchen", LocalDate.now().minusDays(2), rewardExp = 30, rewardGold = 5),
        TaskHistoryItem("t4", "Plan week", LocalDate.now().minusDays(3), rewardExp = 40, rewardGold = 10)
    )
}

// ---------- Screen ----------
@Composable
fun RewardsScreen() {
    val stats = mockPlayerStats()
    val goals = mockGoals()
    val history = mockHistory()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatsSummaryCard(stats)
        }
        item {
            GoalsCard(goals)
        }
        item {
            TaskHistoryCard(history)
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
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

            // Row of three quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill(
                    icon = Icons.Filled.PlayArrow,
                    label = "EXP",
                    value = stats.exp.toString(),
                    tint = Color(0xFF667EEA)
                )
                StatPill(
                    icon = Icons.Filled.Star,
                    label = "LVL",
                    value = stats.level.toString(),
                    tint = Color(0xFF38B2AC)
                )
                StatPill(
                    icon = Icons.Filled.ShoppingCart,
                    label = "Gold",
                    value = stats.gold.toString(),
                    tint = Color(0xFFF6AD55)
                )
            }

            // EXP to next level progress
            val progress = (stats.exp.toFloat() / stats.expForNextLevel.toFloat()).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Next Level", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    trackColor = Color(0xFFE2E8F0),
                )
                Text(
                    "${stats.exp} / ${stats.expForNextLevel} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF4A5568)
                )
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
    Surface(
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 96.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(goal.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("${goal.current}/${goal.target} ${goal.unit}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4A5568))
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            trackColor = Color(0xFFE2E8F0),
        )
    }
}

@Composable
fun TaskHistoryCard(history: List<TaskHistoryItem>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Task History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (history.isEmpty()) {
                Text("No tasks completed yet.", color = Color(0xFF718096))
            } else {
                // simple list; you can switch to LazyColumn-in-Card if this grows
                history.forEach { item ->
                    HistoryRow(item)
                    Divider(color = Color(0xFFE2E8F0))
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: TaskHistoryItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
