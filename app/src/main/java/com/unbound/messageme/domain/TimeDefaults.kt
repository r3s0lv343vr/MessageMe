package com.unbound.messageme.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

object TimeDefaults {
    const val DEFAULT_HOUR = EnvelopeHour.DEFAULT_HOUR
    const val DEFAULT_MINUTE = EnvelopeHour.DEFAULT_MINUTE

    /** AlarmManager ignores triggers that are already in the past; nudge send-now a few seconds ahead. */
    const val SEND_NOW_OFFSET_MS = 5_000L

    /** Unacked follow-ups after task time: +30m, then +60m, then +90m. */
    val UNACKED_OFFSETS_AFTER_TASK_MINUTES = listOf(30L, 90L, 180L)

    const val COMPLETION_CHECK_AFTER_TASK_MINUTES = 60L
    const val COMPLETION_RETRY_AFTER_CHECK_MINUTES = 60L
    const val RESCHEDULE_AFTER_RETRY_MINUTES = 60L

    fun zoneId(): ZoneId = ZoneId.systemDefault()
    fun nowMillis(): Long = System.currentTimeMillis()
    fun minutesToMillis(minutes: Long): Long = TimeUnit.MINUTES.toMillis(minutes)

    /** Material DatePicker stores UTC midnight of the selected calendar day. */
    fun localDateFromUtcMillis(utcEpochMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcEpochMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun utcMillisFromLocalDate(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun resolveDueAt(
        date: LocalDate,
        time: LocalTime?,
        zoneId: ZoneId = zoneId(),
        nowEpochMillis: Long = nowMillis(),
        envelopeHour: EnvelopeHour = EnvelopeHour.DEFAULT
    ): Pair<Long, Boolean> {
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
        val explicit = time != null
        val chosen = time ?: envelopeHour.toLocalTime()
        var millis = LocalDateTime.of(date, chosen).atZone(zoneId).toInstant().toEpochMilli()
        val alreadyPassed = millis < nowEpochMillis - minutesToMillis(1)
        if (alreadyPassed) {
            if (explicit && !date.isBefore(today)) {
                // Same-day (or later) clock time already passed: deliver now, do not error.
                millis = nowEpochMillis + SEND_NOW_OFFSET_MS
            } else if (!explicit) {
                // Overnight letter: if this morning's envelope hour passed, use the next morning.
                millis = LocalDateTime.of(date.plusDays(1), chosen).atZone(zoneId).toInstant().toEpochMilli()
            }
        }
        return millis to explicit
    }

    fun validateReminderInput(title: String, dueAtEpochMillis: Long, now: Long = nowMillis()): String? {
        if (title.isBlank()) return "Title is required"
        if (dueAtEpochMillis < now - minutesToMillis(1)) return "Date cannot be in the past"
        return null
    }
}
