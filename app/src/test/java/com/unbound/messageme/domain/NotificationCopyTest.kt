package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import com.unbound.messageme.data.local.TaskEntity
import org.junit.Test

class NotificationCopyTest {
    private fun task(title: String, body: String = "") = TaskEntity(
        id = "1",
        title = title,
        body = body,
        dueAtEpochMillis = 1,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0
    )

    @Test
    fun usesBodyWhenPresent() {
        assertThat(NotificationCopy.personalNote(task("Study", "Study chapters 2, 3 & 4 for Bio exam")))
            .isEqualTo("Study\nStudy chapters 2, 3 & 4 for Bio exam")
    }

    @Test
    fun usesTitleWhenBodyBlank() {
        assertThat(NotificationCopy.personalNote(task("Study chapters 2–4")))
            .isEqualTo("Study chapters 2–4")
    }

    @Test
    fun doesNotDuplicateTitleInsideBody() {
        assertThat(NotificationCopy.personalNote(task("Walk the dog", "Walk the dog")))
            .isEqualTo("Walk the dog")
    }
}
