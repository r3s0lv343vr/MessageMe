package com.unbound.messageme.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object TimeDefaults {
    const val DEFAULT_HOUR = 3
    const val DEFAULT_MINUTE = 0

    /** Daytime reminder hours used when task time was not explicitly chosen. */
    val DAYTIME_REMINDER_HOURS = listOf(8, 10, 15)

    /** Unacked follow-ups after task time: +30m, then +60m, then +90m. */
    val UNACKED_OFFSETS_AFTER_TASK_MINUTES = listOf(30L, 90L, 180L)

    const val COMPLETION_CHECK_AFTER_TASK_MINUTES = 60L
    const val COMPLETION_RETRY_AFTER_CHECK_MINUTES = 60L
    const val RESCHEDULE_AFTER_RETRY_MINUTES = 60L

    fun zoneId(): ZoneId = ZoneId.systemDefault()
    fun nowMillis(): Long = System.currentTimeMillis()
    fun minutesToMillis(minutes: Long): Long = TimeUnit.MINUTES.toMillis(minutes)

    fun resolveDueAt(date: LocalDate, time: LocalTime?, zoneId: ZoneId = zoneId()): Pair<Long, Boolean> {
        val chosen = time ?: LocalTime.of(DEFAULT_HOUR, DEFAULT_MINUTE)
        val millis = LocalDateTime.of(date, chosen).atZone(zoneId).toInstant().toEpochMilli()
        return millis to (time != null)
    }

    fun validateReminderInput(title: String, dueAtEpochMillis: Long, now: Long = nowMillis()): String? {
        if (title.isBlank()) return "Title is required"
        if (dueAtEpochMillis < now - minutesToMillis(1)) return "Due time must be in the future"
        return null
    }
}
