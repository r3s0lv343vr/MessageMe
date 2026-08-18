package com.unbound.messageme.domain

import com.unbound.messageme.data.local.TaskEntity

/** Last-write-wins merge for Room ↔ Firestore task documents. */
object SyncConflictLogic {
    fun shouldApplyRemote(local: TaskEntity?, remote: TaskEntity): Boolean {
        if (local == null) return true
        return remote.updatedAtEpochMillis >= local.updatedAtEpochMillis
    }
}
