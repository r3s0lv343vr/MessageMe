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

class UnreadLetterLogicTest {
    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 8, 21)

    private fun task(id: String, title: String, body: String = "") = TaskEntity(
        id = id,
        title = title,
        body = body,
        dueAtEpochMillis = 1,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0
    )

    private fun message(
        id: String,
        taskId: String,
        hour: Int,
        unread: Boolean,
        kind: MessageKind = MessageKind.REMINDER
    ) = ChatMessageEntity(
        id = id,
        taskId = taskId,
        body = "✉️ $id",
        kind = kind,
        sentAtEpochMillis = day.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli(),
        isReminderStyle = true,
        isUnread = unread
    )

    @Test
    fun `empty inbox shows a single caught-up letter`() {
        val snap = UnreadLetterLogic.snapshot(emptyList(), emptyList(), zone)
        assertThat(snap.hasUnread).isFalse()
        assertThat(snap.preview).isEqualTo(UnreadLetterLogic.EMPTY_PREVIEW)
        assertThat(snap.countLabel).isEmpty()
        assertThat(snap.messageId).isNull()
    }

    @Test
    fun `widget shows latest unread note and total unread count`() {
        val tasks = listOf(
            task("a", "Older"),
            task("b", "Walk the dog", "Take the side path")
        )
        val messages = listOf(
            message("1", "a", 8, unread = true),
            message("2", "b", 10, unread = true),
            message("3", "b", 9, unread = false)
        )
        val snap = UnreadLetterLogic.snapshot(messages, tasks, zone)
        assertThat(snap.unreadCount).isEqualTo(2)
        assertThat(snap.countLabel).isEqualTo("2")
        assertThat(snap.preview).contains("Walk the dog")
        assertThat(snap.messageId).isEqualTo("2")
        assertThat(snap.dayIso).isEqualTo("2026-08-21")
    }

    @Test
    fun `completed notes leave the letter`() {
        val tasks = listOf(task("a", "Done").copy(status = TaskStatus.COMPLETED))
        val messages = listOf(message("1", "a", 8, unread = true))
        val snap = UnreadLetterLogic.snapshot(messages, tasks, zone)
        assertThat(snap.hasUnread).isFalse()
    }

    @Test
    fun `compose notes do not count as unread letters`() {
        val compose = ChatMessageEntity(
            id = "c",
            taskId = "a",
            body = "📌 Walk",
            kind = MessageKind.USER_COMPOSE,
            sentAtEpochMillis = 1,
            isUnread = true
        )
        val snap = UnreadLetterLogic.snapshot(listOf(compose), listOf(task("a", "Walk")), zone)
        assertThat(snap.hasUnread).isFalse()
    }

    @Test
    fun `count caps at 99 plus`() {
        assertThat(UnreadLetterLogic.countLabel(99)).isEqualTo("99")
        assertThat(UnreadLetterLogic.countLabel(100)).isEqualTo("99+")
    }
}
