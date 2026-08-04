package com.unbound.messageme.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val dueAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: Priority = Priority.NORMAL,
    val category: String = "General",
    val recurrence: Recurrence = Recurrence.NONE,
    val customRecurrenceDays: Int? = null,
    val timeWasExplicitlyChosen: Boolean = false,
    val acknowledgedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    val snoozeUntilEpochMillis: Long? = null,
    val deleted: Boolean = false,
    val firebaseDocId: String? = null
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("sentAtEpochMillis")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val taskId: String?,
    val body: String,
    val kind: MessageKind,
    val sentAtEpochMillis: Long,
    val isReminderStyle: Boolean = false,
    val isUnread: Boolean = false,
    val requiresAck: Boolean = false,
    val requiresCompletionAnswer: Boolean = false,
    val requiresReschedule: Boolean = false
)

@Entity(
    tableName = "scheduled_reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("triggerAtEpochMillis")]
)
data class ScheduledReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val type: ReminderType,
    val triggerAtEpochMillis: Long,
    val delivered: Boolean = false,
    val cancelled: Boolean = false
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val createdAtEpochMillis: Long,
    val attempts: Int = 0
)
