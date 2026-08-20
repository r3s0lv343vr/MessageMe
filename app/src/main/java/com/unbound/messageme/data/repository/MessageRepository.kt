package com.unbound.messageme.data.repository

import com.google.gson.Gson
import com.unbound.messageme.data.local.ChatMessageDao
import com.unbound.messageme.data.local.ChatMessageEntity
import com.unbound.messageme.data.local.MessageKind
import com.unbound.messageme.data.local.Priority
import com.unbound.messageme.data.local.Recurrence
import com.unbound.messageme.data.local.ReminderType
import com.unbound.messageme.data.local.ScheduledReminderDao
import com.unbound.messageme.data.local.SyncQueueDao
import com.unbound.messageme.data.local.SyncQueueEntity
import com.unbound.messageme.data.local.TaskDao
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import com.unbound.messageme.data.preferences.UserPreferences
import com.unbound.messageme.data.sync.CloudSync
import com.unbound.messageme.domain.NotificationCopy
import com.unbound.messageme.domain.NotificationDeliveryPolicy
import com.unbound.messageme.domain.ReminderPlanner
import com.unbound.messageme.domain.SyncConflictLogic
import com.unbound.messageme.domain.TimeDefaults
import com.unbound.messageme.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val messageDao: ChatMessageDao,
    private val reminderDao: ScheduledReminderDao,
    private val syncQueueDao: SyncQueueDao,
    private val scheduler: ReminderScheduler,
    private val preferences: UserPreferences,
    private val cloudSync: CloudSync
) {
    private val gson = Gson()
    private val deliverLock = Mutex()

    fun observeMessages(): Flow<List<ChatMessageEntity>> = messageDao.observeAll()
    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeActive()
    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.search(query)
    fun observeCompletedCount(): Flow<Int> = taskDao.observeCompletedCount()
    fun observeOpenCount(): Flow<Int> = taskDao.observeOpenCount()

    suspend fun createReminder(
        title: String,
        body: String,
        date: LocalDate,
        time: LocalTime?,
        priority: Priority = Priority.NORMAL,
        category: String = "General",
        recurrence: Recurrence = Recurrence.NONE,
        customRecurrenceDays: Int? = null
    ): Result<String> {
        val (dueAt, explicit) = TimeDefaults.resolveDueAt(date, time)
        TimeDefaults.validateReminderInput(title, dueAt)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        val now = TimeDefaults.nowMillis()
        val id = UUID.randomUUID().toString()
        val task = TaskEntity(
            id = id,
            title = title.trim(),
            body = body.trim(),
            dueAtEpochMillis = dueAt,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            priority = priority,
            category = category,
            recurrence = recurrence,
            customRecurrenceDays = customRecurrenceDays,
            timeWasExplicitlyChosen = explicit
        )
        taskDao.upsert(task)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = id,
                body = buildComposeBody(task),
                kind = MessageKind.USER_COMPOSE,
                sentAtEpochMillis = now
            )
        )
        scheduleForTask(task)
        enqueueSync("task", id, "upsert", task)
        return Result.success(id)
    }

    suspend fun editReminder(
        taskId: String,
        title: String,
        body: String,
        date: LocalDate,
        time: LocalTime?,
        priority: Priority,
        category: String,
        recurrence: Recurrence,
        customRecurrenceDays: Int?
    ): Result<Unit> {
        val existing = taskDao.getById(taskId) ?: return Result.failure(IllegalArgumentException("Not found"))
        val (dueAt, explicit) = TimeDefaults.resolveDueAt(date, time)
        TimeDefaults.validateReminderInput(title, dueAt)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        cancelPending(taskId)
        val updated = existing.copy(
            title = title.trim(),
            body = body.trim(),
            dueAtEpochMillis = dueAt,
            updatedAtEpochMillis = TimeDefaults.nowMillis(),
            priority = priority,
            category = category,
            recurrence = recurrence,
            customRecurrenceDays = customRecurrenceDays,
            timeWasExplicitlyChosen = explicit,
            status = TaskStatus.PENDING,
            acknowledgedAtEpochMillis = null,
            completedAtEpochMillis = null,
            snoozeUntilEpochMillis = null
        )
        taskDao.upsert(updated)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                body = "Updated: ${buildComposeBody(updated)}",
                kind = MessageKind.SYSTEM,
                sentAtEpochMillis = TimeDefaults.nowMillis()
            )
        )
        scheduleForTask(updated)
        enqueueSync("task", taskId, "upsert", updated)
        return Result.success(Unit)
    }

    suspend fun deleteReminder(taskId: String) {
        val task = taskDao.getById(taskId) ?: return
        cancelPending(taskId)
        val deleted = task.copy(deleted = true, updatedAtEpochMillis = TimeDefaults.nowMillis())
        taskDao.upsert(deleted)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                body = "Deleted reminder: ${task.title}",
                kind = MessageKind.SYSTEM,
                sentAtEpochMillis = TimeDefaults.nowMillis()
            )
        )
        enqueueSync("task", taskId, "upsert", deleted)
    }

    suspend fun acknowledge(taskId: String) {
        val task = taskDao.getById(taskId) ?: return
        if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.ACKNOWLEDGED) return
        val now = TimeDefaults.nowMillis()
        val updated = task.copy(
            status = TaskStatus.ACKNOWLEDGED,
            acknowledgedAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        taskDao.upsert(updated)
        val pendingUnacked = reminderDao.getForTask(taskId).filter {
            !it.delivered && !it.cancelled &&
                it.type in listOf(ReminderType.UNACKED_1, ReminderType.UNACKED_2, ReminderType.UNACKED_3)
        }
        scheduler.cancelReminders(pendingUnacked)
        reminderDao.cancelUnackedForTask(taskId)
        messageDao.markTaskMessagesRead(taskId)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                body = "Acknowledged — delivery received for “${task.title}”.",
                kind = MessageKind.SYSTEM,
                sentAtEpochMillis = now
            )
        )
        enqueueSync("task", taskId, "upsert", updated)
    }

    suspend fun markCompleted(taskId: String) {
        val task = taskDao.getById(taskId) ?: return
        val now = TimeDefaults.nowMillis()
        cancelPending(taskId)
        val updated = task.copy(
            status = TaskStatus.COMPLETED,
            completedAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        taskDao.upsert(updated)
        messageDao.markTaskMessagesRead(taskId)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                body = "Marked complete: ${task.title}",
                kind = MessageKind.SYSTEM,
                sentAtEpochMillis = now
            )
        )
        maybeSpawnRecurrence(updated)
        enqueueSync("task", taskId, "upsert", updated)
    }

    suspend fun dismiss(taskId: String) {
        val task = taskDao.getById(taskId) ?: return
        cancelPending(taskId)
        val updated = task.copy(
            status = TaskStatus.DISMISSED,
            updatedAtEpochMillis = TimeDefaults.nowMillis()
        )
        taskDao.upsert(updated)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                body = "Dismissed — you chose not to attend to “${task.title}”.",
                kind = MessageKind.SYSTEM,
                sentAtEpochMillis = TimeDefaults.nowMillis()
            )
        )
        enqueueSync("task", taskId, "upsert", updated)
    }

    suspend fun reschedule(taskId: String, date: LocalDate, time: LocalTime?) {
        val task = taskDao.getById(taskId) ?: return
        val (dueAt, explicit) = TimeDefaults.resolveDueAt(date, time)
        cancelPending(taskId)
        val updated = task.copy(
            dueAtEpochMillis = dueAt,
            timeWasExplicitlyChosen = explicit,
            status = TaskStatus.PENDING,
            acknowledgedAtEpochMillis = null,
            completedAtEpochMillis = null,
            updatedAtEpochMillis = TimeDefaults.nowMillis()
        )
        taskDao.upsert(updated)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                body = "Rescheduled: ${buildComposeBody(updated)}",
                kind = MessageKind.USER_COMPOSE,
                sentAtEpochMillis = TimeDefaults.nowMillis()
            )
        )
        scheduleForTask(updated)
        enqueueSync("task", taskId, "upsert", updated)
    }

    suspend fun snooze(taskId: String, minutes: Long) {
        val task = taskDao.getById(taskId) ?: return
        val until = TimeDefaults.nowMillis() + TimeDefaults.minutesToMillis(minutes)
        cancelPending(taskId)
        val updated = task.copy(
            snoozeUntilEpochMillis = until,
            dueAtEpochMillis = until,
            updatedAtEpochMillis = TimeDefaults.nowMillis(),
            status = TaskStatus.PENDING
        )
        taskDao.upsert(updated)
        scheduleForTask(updated)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                body = "Snoozed “${task.title}” for $minutes minutes.",
                kind = MessageKind.SYSTEM,
                sentAtEpochMillis = TimeDefaults.nowMillis()
            )
        )
    }

    suspend fun deliverOverdueReminders(nowEpochMillis: Long = TimeDefaults.nowMillis()) {
        reminderDao.getOverdue(nowEpochMillis).forEach { deliverReminder(it.id) }
    }

    suspend fun deliverReminder(reminderId: String) = deliverLock.withLock {
        val reminder = reminderDao.getById(reminderId) ?: return
        if (reminder.delivered || reminder.cancelled) return
        val task = taskDao.getById(reminder.taskId) ?: return
        if (task.deleted || task.status == TaskStatus.DISMISSED || task.status == TaskStatus.COMPLETED) {
            reminderDao.update(reminder.copy(cancelled = true))
            return
        }

        when (reminder.type) {
            ReminderType.UNACKED_1, ReminderType.UNACKED_2, ReminderType.UNACKED_3 -> {
                if (task.status != TaskStatus.PENDING) {
                    reminderDao.update(reminder.copy(cancelled = true))
                    return
                }
            }
            ReminderType.COMPLETION_CHECK, ReminderType.COMPLETION_CHECK_RETRY -> {
                if (task.status != TaskStatus.ACKNOWLEDGED) {
                    reminderDao.update(reminder.copy(cancelled = true))
                    return
                }
            }
            ReminderType.RESCHEDULE_REQUEST -> {
                if (task.status != TaskStatus.ACKNOWLEDGED && task.status != TaskStatus.NEEDS_RESCHEDULE) {
                    reminderDao.update(reminder.copy(cancelled = true))
                    return
                }
            }
            else -> Unit
        }

        val now = TimeDefaults.nowMillis()
        val content = messageFor(reminder.type, task)
        messageDao.upsert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                taskId = task.id,
                body = content.body,
                kind = content.kind,
                sentAtEpochMillis = now,
                isReminderStyle = true,
                isUnread = true,
                requiresAck = content.requiresAck,
                requiresCompletionAnswer = content.requiresCompletion,
                requiresReschedule = content.requiresReschedule
            )
        )

        var updatedTask = task
        if (reminder.type == ReminderType.UNACKED_3) {
            updatedTask = task.copy(status = TaskStatus.SHELVED_UNACKNOWLEDGED, updatedAtEpochMillis = now)
            taskDao.upsert(updatedTask)
        }
        if (reminder.type == ReminderType.RESCHEDULE_REQUEST) {
            updatedTask = task.copy(status = TaskStatus.NEEDS_RESCHEDULE, updatedAtEpochMillis = now)
            taskDao.upsert(updatedTask)
        }

        reminderDao.update(reminder.copy(delivered = true))

        val notificationsOn = preferences.internalNotificationsEnabled.first()
        if (
            NotificationDeliveryPolicy.shouldShowSystemNotification(
                internalNotificationsEnabled = notificationsOn,
                taskDeleted = updatedTask.deleted,
                status = updatedTask.status
            )
        ) {
            scheduler.notifyUser(updatedTask, reminder)
        }
    }

    suspend fun rescheduleAllPendingAlarms() {
        reminderDao.getPending().forEach { scheduler.schedule(it) }
    }

    suspend fun replaceAllFromBackup(tasks: List<TaskEntity>) {
        tasks.forEach { taskDao.upsert(it) }
        taskDao.getActive().forEach { scheduleForTask(it) }
    }

    suspend fun syncNow(): Result<Unit> {
        if (!cloudSync.isConfigured) {
            return Result.failure(IllegalStateException("Firebase is not configured. Add app/google-services.json."))
        }
        if (cloudSync.currentUserId() == null) {
            cloudSync.signInAnonymously().getOrElse { return Result.failure(it) }
        }
        val local = taskDao.getActive()
        cloudSync.pushTasks(local).getOrElse { return Result.failure(it) }
        val remote = cloudSync.pullTasks().getOrElse { return Result.failure(it) }
        mergeRemote(remote)
        syncQueueDao.clear()
        return Result.success(Unit)
    }

    private suspend fun mergeRemote(remote: List<TaskEntity>) {
        remote.forEach { remoteTask ->
            val local = taskDao.getById(remoteTask.id)
            if (SyncConflictLogic.shouldApplyRemote(local, remoteTask)) {
                taskDao.upsert(remoteTask)
                if (!remoteTask.deleted && remoteTask.status != TaskStatus.COMPLETED &&
                    remoteTask.status != TaskStatus.DISMISSED
                ) {
                    cancelPending(remoteTask.id)
                    scheduleForTask(remoteTask)
                }
            }
        }
    }

    private suspend fun scheduleForTask(task: TaskEntity) {
        val planned = ReminderPlanner.planForTask(
            taskId = task.id,
            dueAtEpochMillis = task.dueAtEpochMillis,
            timeWasExplicitlyChosen = task.timeWasExplicitlyChosen
        )
        if (planned.isNotEmpty()) {
            reminderDao.upsertAll(planned)
            planned.forEach { scheduler.schedule(it) }
        }
    }

    private suspend fun cancelPending(taskId: String) {
        val pending = reminderDao.getForTask(taskId).filter { !it.delivered && !it.cancelled }
        scheduler.cancelReminders(pending)
        reminderDao.cancelPendingForTask(taskId)
    }

    private suspend fun maybeSpawnRecurrence(completed: TaskEntity) {
        if (completed.recurrence == Recurrence.NONE) return
        val due = Instant.ofEpochMilli(completed.dueAtEpochMillis)
            .atZone(TimeDefaults.zoneId())
        val next = when (completed.recurrence) {
            Recurrence.DAILY -> due.plusDays(1)
            Recurrence.WEEKLY -> due.plusWeeks(1)
            Recurrence.MONTHLY -> due.plusMonths(1)
            Recurrence.CUSTOM -> due.plusDays((completed.customRecurrenceDays ?: 1).toLong())
            Recurrence.NONE -> return
        }
        createReminder(
            title = completed.title,
            body = completed.body,
            date = next.toLocalDate(),
            time = if (completed.timeWasExplicitlyChosen) next.toLocalTime() else null,
            priority = completed.priority,
            category = completed.category,
            recurrence = completed.recurrence,
            customRecurrenceDays = completed.customRecurrenceDays
        )
    }

    private suspend fun enqueueSync(type: String, id: String, op: String, task: TaskEntity) {
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = type,
                entityId = id,
                operation = op,
                payloadJson = gson.toJson(task),
                createdAtEpochMillis = TimeDefaults.nowMillis()
            )
        )
    }

    private fun buildComposeBody(task: TaskEntity): String {
        val due = Instant.ofEpochMilli(task.dueAtEpochMillis)
            .atZone(TimeDefaults.zoneId())
        val timeNote = if (task.timeWasExplicitlyChosen) {
            due.toLocalTime().toString()
        } else {
            "3:00 AM default"
        }
        val extra = if (task.body.isBlank()) "" else "\n${task.body}"
        return "📌 ${task.title}$extra\nDue ${due.toLocalDate()} · $timeNote · ${task.priority} · ${task.category}"
    }

    private data class ReminderCopy(
        val body: String,
        val kind: MessageKind,
        val requiresAck: Boolean,
        val requiresCompletion: Boolean,
        val requiresReschedule: Boolean
    )

    private fun messageFor(type: ReminderType, task: TaskEntity): ReminderCopy = when (type) {
        ReminderType.T_MINUS_3H ->
            ReminderCopy("✉️ Reminder (3 hours): ${task.title}", MessageKind.REMINDER, true, false, false)
        ReminderType.T_MINUS_1H ->
            ReminderCopy("✉️ Reminder (1 hour): ${task.title}", MessageKind.REMINDER, true, false, false)
        ReminderType.T_MINUS_30M ->
            ReminderCopy("✉️ Reminder (30 minutes): ${task.title}", MessageKind.REMINDER, true, false, false)
        ReminderType.T_MINUS_5M ->
            ReminderCopy("✉️ Reminder (5 minutes): ${task.title}", MessageKind.REMINDER, true, false, false)
        ReminderType.AT_DUE ->
            ReminderCopy("✉️ ${NotificationCopy.personalNote(task)}", MessageKind.REMINDER, true, false, false)
        ReminderType.DAYTIME_8AM ->
            ReminderCopy("✉️ Morning check-in (8:00 AM): ${task.title}", MessageKind.DAYTIME_REMINDER, true, false, false)
        ReminderType.DAYTIME_10AM ->
            ReminderCopy("✉️ Mid-morning check-in (10:00 AM): ${task.title}", MessageKind.DAYTIME_REMINDER, true, false, false)
        ReminderType.DAYTIME_3PM ->
            ReminderCopy("✉️ Afternoon check-in (3:00 PM): ${task.title}", MessageKind.DAYTIME_REMINDER, true, false, false)
        ReminderType.UNACKED_1 ->
            ReminderCopy("✉️ Follow-up: please acknowledge “${task.title}”", MessageKind.FOLLOW_UP_UNACKED, true, false, false)
        ReminderType.UNACKED_2 ->
            ReminderCopy("✉️ Second follow-up: acknowledge “${task.title}”", MessageKind.FOLLOW_UP_UNACKED, true, false, false)
        ReminderType.UNACKED_3 ->
            ReminderCopy("✉️ Final follow-up — shelving as unacknowledged: ${task.title}", MessageKind.FOLLOW_UP_UNACKED, true, false, false)
        ReminderType.COMPLETION_CHECK ->
            ReminderCopy("Did you finish “${task.title}”?", MessageKind.COMPLETION_CHECK, false, true, false)
        ReminderType.COMPLETION_CHECK_RETRY ->
            ReminderCopy("Still waiting — confirm if “${task.title}” is done.", MessageKind.COMPLETION_CHECK, false, true, false)
        ReminderType.RESCHEDULE_REQUEST ->
            ReminderCopy("Still open — reschedule, dismiss, or complete: ${task.title}", MessageKind.RESCHEDULE_REQUEST, false, false, true)
    }
}
