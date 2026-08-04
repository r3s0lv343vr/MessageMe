package com.unbound.messageme.domain

import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import java.time.Instant
import java.time.LocalTime

/**
 * On-device heuristic suggestions (no cloud AI / API key required).
 * Uses completed-task history to propose likely productive hours.
 */
object AiScheduleSuggestions {
    data class Suggestion(
        val label: String,
        val time: LocalTime,
        val rationale: String
    )

    fun suggest(tasks: List<TaskEntity>, limit: Int = 3): List<Suggestion> {
        val completedHours = tasks
            .filter { it.status == TaskStatus.COMPLETED && it.completedAtEpochMillis != null }
            .map {
                Instant.ofEpochMilli(it.completedAtEpochMillis!!)
                    .atZone(TimeDefaults.zoneId())
                    .hour
            }

        val ranked = if (completedHours.isEmpty()) {
            listOf(9, 11, 15)
        } else {
            completedHours.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .map { it.key }
        }

        return ranked.take(limit).mapIndexed { index, hour ->
            Suggestion(
                label = when (index) {
                    0 -> "Best focus window"
                    1 -> "Secondary slot"
                    else -> "Backup slot"
                },
                time = LocalTime.of(hour.coerceIn(0, 23), 0),
                rationale = if (completedHours.isEmpty()) {
                    "Starter suggestion — refine as you complete tasks"
                } else {
                    "Based on when you usually finish tasks"
                }
            )
        }
    }
}
