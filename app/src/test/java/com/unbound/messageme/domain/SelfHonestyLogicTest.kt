package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.MessageKind
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class SelfHonestyLogicTest {
    private val zone = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 8, 21) // Friday

    private fun task(
        id: String,
        status: TaskStatus,
        date: LocalDate = day
    ) = TaskEntity(
        id = id,
        title = id,
        body = "",
        dueAtEpochMillis = date.atStartOfDay().toInstant(zone).toEpochMilli(),
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
        status = status
    )

    private fun letter(id: String, taskId: String, unread: Boolean, date: LocalDate = day) =
        ChatMessageEntity(
            id = id,
            taskId = taskId,
            body = "note $id",
            kind = MessageKind.REMINDER,
            sentAtEpochMillis = date.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli(),
            isReminderStyle = true,
            isUnread = unread
        )

    @Test
    fun `unopened letters skip completed tasks`() {
        val tasks = listOf(task("a", TaskStatus.PENDING), task("b", TaskStatus.COMPLETED))
        val messages = listOf(
            letter("1", "a", unread = true),
            letter("2", "b", unread = true)
        )
        val open = SelfHonestyLogic.unopenedLetters(messages, tasks, day, zone)
        assertThat(open.map { it.id }).containsExactly("1")
    }

    @Test
    fun `acknowledged unfinished is by due day`() {
        val tasks = listOf(
            task("a", TaskStatus.ACKNOWLEDGED),
            task("b", TaskStatus.PENDING),
            task("c", TaskStatus.ACKNOWLEDGED, day.minusDays(1))
        )
        val acked = SelfHonestyLogic.acknowledgedUnfinished(tasks, day, zone)
        assertThat(acked.map { it.id }).containsExactly("a")
    }

    @Test
    fun `quiet week copy is calm not a streak`() {
        val honesty = SelfHonestyLogic.weekHonesty(emptyList(), emptyList(), day, zone)
        assertThat(honesty.isQuiet).isTrue()
        assertThat(honesty.body()).doesNotContain("streak")
        assertThat(honesty.body()).contains("quiet")
    }

    @Test
    fun `week of unopened mail counts letters and unfinished acks`() {
        val tasks = listOf(
            task("a", TaskStatus.PENDING),
            task("b", TaskStatus.ACKNOWLEDGED, day.minusDays(1))
        )
        val messages = listOf(
            letter("1", "a", unread = true),
            letter("2", "a", unread = true)
        )
        val honesty = SelfHonestyLogic.weekHonesty(messages, tasks, day, zone)
        assertThat(honesty.unopenedCount).isEqualTo(2)
        assertThat(honesty.acknowledgedUnfinishedCount).isEqualTo(1)
        assertThat(honesty.body()).contains("2 unopened letters")
        assertThat(honesty.body()).contains("acknowledged, not finished")
        assertThat(honesty.weekStart.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
    }
}
