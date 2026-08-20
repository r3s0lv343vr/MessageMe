package com.unbound.messageme.domain

import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.MessageKind
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object InboxLogic {
    fun isReceived(message: ChatMessageEntity): Boolean =
        message.isReminderStyle ||
            message.kind == MessageKind.REMINDER ||
            message.kind == MessageKind.DAYTIME_REMINDER ||
            message.kind == MessageKind.FOLLOW_UP_UNACKED ||
            message.kind == MessageKind.COMPLETION_CHECK ||
            message.kind == MessageKind.RESCHEDULE_REQUEST

    fun dayOf(message: ChatMessageEntity, zone: ZoneId = TimeDefaults.zoneId()): LocalDate =
        Instant.ofEpochMilli(message.sentAtEpochMillis).atZone(zone).toLocalDate()

    fun receivedOn(
        messages: List<ChatMessageEntity>,
        date: LocalDate,
        zone: ZoneId = TimeDefaults.zoneId()
    ): List<ChatMessageEntity> =
        messages.filter { isReceived(it) && dayOf(it, zone) == date }
            .sortedBy { it.sentAtEpochMillis }

    fun scheduledTasks(
        tasks: List<TaskEntity>,
        messages: List<ChatMessageEntity>
    ): List<TaskEntity> {
        val receivedTaskIds = messages.filter { isReceived(it) }.mapNotNull { it.taskId }.toSet()
        return tasks.filter { task ->
            !task.deleted &&
                task.status != TaskStatus.COMPLETED &&
                task.status != TaskStatus.DISMISSED &&
                task.id !in receivedTaskIds
        }.sortedBy { it.dueAtEpochMillis }
    }
}
