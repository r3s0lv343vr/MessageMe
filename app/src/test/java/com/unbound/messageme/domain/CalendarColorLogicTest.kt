package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import com.unbound.messageme.data.local.CalendarDayStatus
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CalendarColorLogicTest {
    private val today = LocalDate.of(2026, 8, 4)
    private val zone = ZoneOffset.UTC

    private fun task(date: LocalDate, status: TaskStatus) = TaskEntity(
        id = "$date-$status",
        title = "t",
        body = "",
        dueAtEpochMillis = date.atStartOfDay().toInstant(zone).toEpochMilli(),
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
        status = status
    )

    @Test
    fun freeWhenNoTasks() {
        assertThat(CalendarColorLogic.statusForDay(today, emptyList(), today, zone))
            .isEqualTo(CalendarDayStatus.FREE)
    }

    @Test
    fun completedWhenAllDone() {
        val tasks = listOf(task(today, TaskStatus.COMPLETED))
        assertThat(CalendarColorLogic.statusForDay(today, tasks, today, zone))
            .isEqualTo(CalendarDayStatus.COMPLETED)
    }

    @Test
    fun overdueForPastOpenTasks() {
        val past = today.minusDays(1)
        val tasks = listOf(task(past, TaskStatus.PENDING))
        assertThat(CalendarColorLogic.statusForDay(past, tasks, today, zone))
            .isEqualTo(CalendarDayStatus.OVERDUE)
    }

    @Test
    fun mixedWhenCompleteAndOpen() {
        val tasks = listOf(
            task(today, TaskStatus.COMPLETED),
            task(today, TaskStatus.PENDING)
        )
        assertThat(CalendarColorLogic.statusForDay(today, tasks, today, zone))
            .isEqualTo(CalendarDayStatus.MIXED)
    }

    @Test
    fun pendingForFutureOpenTasks() {
        val future = today.plusDays(2)
        val tasks = listOf(task(future, TaskStatus.PENDING))
        assertThat(CalendarColorLogic.statusForDay(future, tasks, today, zone))
            .isEqualTo(CalendarDayStatus.HAS_PENDING)
    }
}
