package com.unbound.messageme.domain

import com.unbound.messageme.data.local.CalendarDayStatus
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
        zoneId: ZoneId = TimeDefaults.zoneId()
    ): CalendarDayStatus {
        val dayTasks = tasks.filter {
            !it.deleted &&
                Instant.ofEpochMilli(it.dueAtEpochMillis).atZone(zoneId).toLocalDate() == date &&
                it.status != TaskStatus.DISMISSED
        }
        if (dayTasks.isEmpty()) return CalendarDayStatus.FREE

        val completed = dayTasks.count { it.status == TaskStatus.COMPLETED }
        val open = dayTasks.count {
            it.status == TaskStatus.PENDING ||
                it.status == TaskStatus.ACKNOWLEDGED ||
                it.status == TaskStatus.NEEDS_RESCHEDULE ||
                it.status == TaskStatus.SHELVED_UNACKNOWLEDGED
        }

        return when {
            completed > 0 && open > 0 -> CalendarDayStatus.MIXED
            completed == dayTasks.size -> CalendarDayStatus.COMPLETED
            date.isBefore(today) && open > 0 -> CalendarDayStatus.OVERDUE
            else -> CalendarDayStatus.HAS_PENDING
        }
    }
}
