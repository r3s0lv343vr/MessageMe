package com.unbound.messageme.domain

import com.unbound.messageme.data.local.ReminderType
import com.unbound.messageme.data.local.ScheduledReminderEntity
import java.util.UUID

object ReminderPlanner {
    private val PRE_TASK = listOf(
        ReminderType.T_MINUS_3H to 180L,
        ReminderType.T_MINUS_1H to 60L,
        ReminderType.T_MINUS_30M to 30L,
        ReminderType.T_MINUS_5M to 5L
    )

    @Suppress("UNUSED_PARAMETER")
    fun planForTask(
        taskId: String,
        dueAtEpochMillis: Long,
        timeWasExplicitlyChosen: Boolean,
        nowEpochMillis: Long = TimeDefaults.nowMillis()
    ): List<ScheduledReminderEntity> {
        val out = mutableListOf<ScheduledReminderEntity>()

        PRE_TASK.forEach { (type, minutes) ->
            val trigger = dueAtEpochMillis - TimeDefaults.minutesToMillis(minutes)
            if (trigger > nowEpochMillis) {
                out += reminder(taskId, type, trigger)
            }
        }

        // The personal message itself, at the time the user chose.
        if (dueAtEpochMillis > nowEpochMillis) {
            out += reminder(taskId, ReminderType.AT_DUE, dueAtEpochMillis)
        }

        TimeDefaults.UNACKED_OFFSETS_AFTER_TASK_MINUTES.zip(
            listOf(ReminderType.UNACKED_1, ReminderType.UNACKED_2, ReminderType.UNACKED_3)
        ).forEach { (offset, type) ->
            val trigger = dueAtEpochMillis + TimeDefaults.minutesToMillis(offset)
            if (trigger > nowEpochMillis) out += reminder(taskId, type, trigger)
        }

        val completion = dueAtEpochMillis +
            TimeDefaults.minutesToMillis(TimeDefaults.COMPLETION_CHECK_AFTER_TASK_MINUTES)
        if (completion > nowEpochMillis) {
            out += reminder(taskId, ReminderType.COMPLETION_CHECK, completion)
        }

        val retry = completion +
            TimeDefaults.minutesToMillis(TimeDefaults.COMPLETION_RETRY_AFTER_CHECK_MINUTES)
        if (retry > nowEpochMillis) {
            out += reminder(taskId, ReminderType.COMPLETION_CHECK_RETRY, retry)
        }

        val reschedule = retry +
            TimeDefaults.minutesToMillis(TimeDefaults.RESCHEDULE_AFTER_RETRY_MINUTES)
        if (reschedule > nowEpochMillis) {
            out += reminder(taskId, ReminderType.RESCHEDULE_REQUEST, reschedule)
        }

        return out
    }

    private fun reminder(taskId: String, type: ReminderType, trigger: Long) =
        ScheduledReminderEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            type = type,
            triggerAtEpochMillis = trigger
        )
}
