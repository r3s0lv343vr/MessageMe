package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.MessageKind
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class InboxLogicTest {
    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 8, 20)

    private fun task(id: String, status: TaskStatus = TaskStatus.PENDING) = TaskEntity(
        id = id,
        title = "Note $id",
        body = "body",
        dueAtEpochMillis = 1,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
        status = status
    )

    private fun message(
        id: String,
        taskId: String?,
        kind: MessageKind,
        hour: Int,
        reminderStyle: Boolean = false
    ) = ChatMessageEntity(
        id = id,
        taskId = taskId,
        body = "msg $id",
        kind = kind,
        sentAtEpochMillis = day.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli(),
        isReminderStyle = reminderStyle
    )

    @Test
    fun `compose notes stay scheduled until a reminder is received`() {
        val tasks = listOf(task("a"), task("b"))
        val messages = listOf(
            message("1", "a", MessageKind.USER_COMPOSE, 8),
            message("2", "b", MessageKind.USER_COMPOSE, 9),
            message("3", "b", MessageKind.REMINDER, 10, reminderStyle = true)
        )
        val scheduled = InboxLogic.scheduledTasks(tasks, messages)
        assertThat(scheduled.map { it.id }).containsExactly("a")
    }

    @Test
    fun `received messages are grouped by calendar day`() {
        val otherDay = message("x", "a", MessageKind.REMINDER, 11, true).copy(
            sentAtEpochMillis = day.plusDays(1).atTime(LocalTime.of(11, 0)).atZone(zone).toInstant().toEpochMilli()
        )
        val messages = listOf(
            message("1", "a", MessageKind.USER_COMPOSE, 8),
            message("2", "a", MessageKind.REMINDER, 9, true),
            message("3", "a", MessageKind.FOLLOW_UP_UNACKED, 10, true),
            otherDay
        )
        val received = InboxLogic.receivedOn(messages, day, zone)
        assertThat(received.map { it.id }).containsExactly("2", "3").inOrder()
    }

    @Test
    fun `note to read uses title when body is blank`() {
        assertThat(InboxLogic.noteToRead(task("a").copy(body = "  "))).isEqualTo("Note a")
    }

    @Test
    fun `note to read joins title and body`() {
        assertThat(InboxLogic.noteToRead(task("a"))).isEqualTo("Note a\n\nbody")
    }

    @Test
    fun `message to open prefers compose note and shows title plus body`() {
        val t = task("a")
        val opened = InboxLogic.messageToOpen(
            t,
            listOf(message("1", "a", MessageKind.USER_COMPOSE, 8).copy(body = "📌 old"))
        )
        assertThat(opened.id).isEqualTo("1")
        assertThat(opened.body).isEqualTo("Note a\n\nbody")
    }
}
