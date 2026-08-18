package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TimeDefaultsTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun `default time is 3 AM when time omitted`() {
        val date = LocalDate.of(2026, 8, 10)
        val now = LocalDate.of(2026, 8, 9).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (millis, explicit) = TimeDefaults.resolveDueAt(date, null, zone, now)
        val local = java.time.Instant.ofEpochMilli(millis).atZone(zone)
        assertThat(explicit).isFalse()
        assertThat(local.hour).isEqualTo(3)
        assertThat(local.minute).isEqualTo(0)
        assertThat(local.toLocalDate()).isEqualTo(date)
    }

    @Test
    fun `explicit time is preserved`() {
        val date = LocalDate.of(2026, 8, 10)
        val time = LocalTime.of(14, 30)
        val now = LocalDate.of(2026, 8, 9).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (millis, explicit) = TimeDefaults.resolveDueAt(date, time, zone, now)
        val local = java.time.Instant.ofEpochMilli(millis).atZone(zone)
        assertThat(explicit).isTrue()
        assertThat(local.toLocalTime()).isEqualTo(time)
    }

    @Test
    fun `omitted time rolls to next morning when 3 AM already passed`() {
        val date = LocalDate.of(2026, 8, 10)
        val now = date.atTime(15, 0).atZone(zone).toInstant().toEpochMilli()
        val (millis, explicit) = TimeDefaults.resolveDueAt(date, null, zone, now)
        val local = java.time.Instant.ofEpochMilli(millis).atZone(zone)
        assertThat(explicit).isFalse()
        assertThat(local.toLocalDate()).isEqualTo(date.plusDays(1))
        assertThat(local.hour).isEqualTo(3)
    }

    @Test
    fun `validation rejects blank title`() {
        assertThat(TimeDefaults.validateReminderInput("  ", System.currentTimeMillis() + 60_000))
            .isEqualTo("Title is required")
    }

    @Test
    fun `validation rejects past due`() {
        assertThat(TimeDefaults.validateReminderInput("Task", System.currentTimeMillis() - 120_000))
            .isNotNull()
    }
}
