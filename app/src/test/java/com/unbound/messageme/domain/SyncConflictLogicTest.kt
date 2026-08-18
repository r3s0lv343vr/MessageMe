package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import com.unbound.messageme.data.local.TaskEntity
import org.junit.Test

class SyncConflictLogicTest {
    private fun task(id: String, updatedAt: Long) = TaskEntity(
        id = id,
        title = "t",
        body = "",
        dueAtEpochMillis = 1,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = updatedAt
    )

    @Test
    fun appliesRemoteWhenLocalMissing() {
        assertThat(SyncConflictLogic.shouldApplyRemote(null, task("a", 10))).isTrue()
    }

    @Test
    fun lastWriteWins() {
        val local = task("a", 20)
        assertThat(SyncConflictLogic.shouldApplyRemote(local, task("a", 21))).isTrue()
        assertThat(SyncConflictLogic.shouldApplyRemote(local, task("a", 19))).isFalse()
        assertThat(SyncConflictLogic.shouldApplyRemote(local, task("a", 20))).isTrue()
    }
}
