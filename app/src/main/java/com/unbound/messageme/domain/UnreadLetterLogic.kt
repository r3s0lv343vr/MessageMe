package com.unbound.messageme.domain

import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import java.time.ZoneId

data class UnreadLetterSnapshot(
    val unreadCount: Int,
    val preview: String,
    val messageId: String? = null,
    val taskId: String? = null,
    val dayIso: String? = null
) {
    val hasUnread: Boolean get() = unreadCount > 0
    val countLabel: String get() = UnreadLetterLogic.countLabel(unreadCount)
}

object UnreadLetterLogic {
    const val EMPTY_PREVIEW = "No unread letters from you"
    const val SENDER = NotificationCopy.SENDER

    fun countLabel(count: Int): String = when {
        count <= 0 -> ""
        count > 99 -> "99+"
        else -> count.toString()
    }

    fun snapshot(
        messages: List<ChatMessageEntity>,
        tasks: List<TaskEntity>,
        zone: ZoneId = TimeDefaults.zoneId()
    ): UnreadLetterSnapshot {
        val closedIds = tasks.filter {
            it.status == TaskStatus.COMPLETED || it.status == TaskStatus.DISMISSED || it.deleted
        }.map { it.id }.toSet()
        val unread = messages
            .filter { message ->
                InboxLogic.isReceived(message) &&
                    message.isUnread &&
                    (message.taskId == null || message.taskId !in closedIds)
            }
            .sortedByDescending { it.sentAtEpochMillis }
        val latest = unread.firstOrNull() ?: return UnreadLetterSnapshot(
            unreadCount = 0,
            preview = EMPTY_PREVIEW
        )
        val task = latest.taskId?.let { id -> tasks.find { it.id == id } }
        val preview = when {
            task != null -> NotificationCopy.personalNote(task)
            else -> latest.body.removePrefix("✉️").trim()
        }.ifBlank { EMPTY_PREVIEW }
        return UnreadLetterSnapshot(
            unreadCount = unread.size,
            preview = preview.lineSequence().take(3).joinToString("\n").take(160),
            messageId = latest.id,
            taskId = latest.taskId,
            dayIso = InboxLogic.dayOf(latest, zone).toString()
        )
    }
}
