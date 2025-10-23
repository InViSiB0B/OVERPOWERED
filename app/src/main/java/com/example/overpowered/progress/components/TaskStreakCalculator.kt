package com.example.overpowered.progress.components

import com.example.overpowered.progress.TaskHistoryItem
import java.time.LocalDate

object StreakCalculator {

    /**
     * Daily streak: consecutive days (including today) with >= 1 completed task.
     * Derives from history only; no side effects.
     */

    fun computeDailyStreak(history: List<TaskHistoryItem>, today: LocalDate = LocalDate.now()): Int {
        if (history.isEmpty()) return 0
        val completionDays: Set<LocalDate> = history.map { it.date }.toSet()

        var streak = 0
        var cursor = today
        while (completionDays.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
