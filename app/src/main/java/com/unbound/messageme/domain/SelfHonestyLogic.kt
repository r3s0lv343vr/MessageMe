package com.unbound.messageme.domain

import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WeekMailHonesty(
    val weekStart: LocalDate,
    val unopenedCount: Int,
    val acknowledgedUnfinishedCount: Int
) {
    val isQuiet: Boolean get() = unopenedCount == 0 && acknowledgedUnfinishedCount == 0

    fun body(): String = when {
        isQuiet -> "This week’s mail is quiet. Nothing waiting to be opened or finished."
        else -> buildList {
            if (unopenedCount > 0) {
                add("$unopenedCount unopened ${if (unopenedCount == 1) "letter" else "letters"}")
            }
            if (acknowledgedUnfinishedCount > 0) {
                add(
                    "$acknowledgedUnfinishedCount acknowledged, not finished"
                )
            }
        }.joinToString(" · ").let { "This week: $it." }
    }
}

object SelfHonestyLogic {
    const val WEEK_HEADLINE = "Week of unopened mail"

    fun closedTaskIds(tasks: List<TaskEntity>): Set<String> =
        tasks.filter {
            it.deleted || it.status == TaskStatus.COMPLETED || it.status == TaskStatus.DISMISSED
        }.map { it.id }.toSet()

    fun unopenedLetters(
        messages: List<ChatMessageEntity>,
        tasks: List<TaskEntity>,
        date: LocalDate,
        zone: ZoneId = TimeDefaults.zoneId()
    ): List<ChatMessageEntity> {
        val closed = closedTaskIds(tasks)
        return InboxLogic.receivedOn(messages, date, zone).filter { message ->
            message.isUnread && (message.taskId == null || message.taskId !in closed)
        }
    }

    fun acknowledgedUnfinished(
        tasks: List<TaskEntity>,
        date: LocalDate,
        zone: ZoneId = TimeDefaults.zoneId()
    ): List<TaskEntity> = tasks.filter { task ->
        !task.deleted &&
            task.status == TaskStatus.ACKNOWLEDGED &&
            Instant.ofEpochMilli(task.dueAtEpochMillis).atZone(zone).toLocalDate() == date
    }

    fun weekStartSunday(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value % 7).toLong())

    fun weekHonesty(
        messages: List<ChatMessageEntity>,
        tasks: List<TaskEntity>,
        dateInWeek: LocalDate,
        zone: ZoneId = TimeDefaults.zoneId()
    ): WeekMailHonesty {
        val start = weekStartSunday(dateInWeek)
        val days = (0L..6L).map { start.plusDays(it) }
        val unopened = days.sumOf { unopenedLetters(messages, tasks, it, zone).size }
        val acked = days.sumOf { acknowledgedUnfinished(tasks, it, zone).size }
        return WeekMailHonesty(
            weekStart = start,
            unopenedCount = unopened,
            acknowledgedUnfinishedCount = acked
        )
    }
}
