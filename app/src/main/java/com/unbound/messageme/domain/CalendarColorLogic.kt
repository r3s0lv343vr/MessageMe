package com.unbound.messageme.domain

import com.unbound.messageme.data.local.CalendarDayStatus
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object CalendarColorLogic {
    fun statusForDay(
        date: LocalDate,
        tasks: List<TaskEntity>,
        today: LocalDate = LocalDate.now(TimeDefaults.zoneId()),
        zoneId: ZoneId = TimeDefaults.zoneId(),
        messages: List<ChatMessageEntity> = emptyList()
    ): CalendarDayStatus {
        if (SelfHonestyLogic.unopenedLetters(messages, tasks, date, zoneId).isNotEmpty()) {
            return CalendarDayStatus.UNOPENED
        }
        val ackedUnfinished = SelfHonestyLogic.acknowledgedUnfinished(tasks, date, zoneId)
        val dayTasks = tasks.filter {
            !it.deleted &&
                Instant.ofEpochMilli(it.dueAtEpochMillis).atZone(zoneId).toLocalDate() == date &&
                it.status != TaskStatus.DISMISSED
        }
        if (ackedUnfinished.isNotEmpty() && dayTasks.none { it.status == TaskStatus.PENDING }) {
            return CalendarDayStatus.ACKNOWLEDGED_UNFINISHED
        }

        if (dayTasks.isEmpty()) return CalendarDayStatus.FREE

        val completed = dayTasks.count { it.status == TaskStatus.COMPLETED }
        val neverOpenedOpen = dayTasks.count {
            it.status == TaskStatus.PENDING ||
                it.status == TaskStatus.NEEDS_RESCHEDULE ||
                it.status == TaskStatus.SHELVED_UNACKNOWLEDGED
        }
        val open = neverOpenedOpen + ackedUnfinished.size

        return when {
            completed > 0 && open > 0 -> CalendarDayStatus.MIXED
            completed == dayTasks.size -> CalendarDayStatus.COMPLETED
            ackedUnfinished.isNotEmpty() -> CalendarDayStatus.ACKNOWLEDGED_UNFINISHED
            date.isBefore(today) && neverOpenedOpen > 0 -> CalendarDayStatus.OVERDUE
            else -> CalendarDayStatus.HAS_PENDING
        }
    }
}
