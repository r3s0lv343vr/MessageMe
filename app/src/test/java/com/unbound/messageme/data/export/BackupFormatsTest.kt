package com.unbound.messageme.data.export

import com.google.common.truth.Truth.assertThat
import com.unbound.messageme.data.local.Priority
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import org.junit.Test

class BackupFormatsTest {
    private val task = TaskEntity(
        id = "t1",
        title = "Buy \"milk\"",
        body = "line1",
        dueAtEpochMillis = 1_700_000_000_000L,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 2,
        status = TaskStatus.PENDING,
        priority = Priority.HIGH,
        category = "Personal"
    )

    @Test
    fun jsonRoundTripPreservesTasks() {
        val json = BackupFormats.toJson(listOf(task), exportedAtEpochMillis = 99)
        val payload = BackupFormats.fromJson(json)
        assertThat(payload.exportedAtEpochMillis).isEqualTo(99)
        assertThat(payload.tasks).hasSize(1)
        assertThat(payload.tasks[0].id).isEqualTo("t1")
        assertThat(payload.tasks[0].title).isEqualTo("Buy \"milk\"")
        assertThat(payload.tasks[0].priority).isEqualTo(Priority.HIGH)
    }

    @Test
    fun csvEscapesQuotesAndIncludesHeader() {
        val csv = BackupFormats.toCsv(listOf(task))
        assertThat(csv).startsWith("id,title,body,dueAt,status,priority,category,recurrence")
        assertThat(csv).contains("\"Buy \"\"milk\"\"\"")
        assertThat(csv).contains("HIGH")
        assertThat(csv).contains("Personal")
    }
}
