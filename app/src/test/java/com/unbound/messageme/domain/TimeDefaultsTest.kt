package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TimeDefaultsTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun `default time is envelope hour when time omitted`() {
        val date = LocalDate.of(2026, 8, 10)
        val now = LocalDate.of(2026, 8, 9).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (millis, explicit) = TimeDefaults.resolveDueAt(date, null, zone, now)
        val local = Instant.ofEpochMilli(millis).atZone(zone)
        assertThat(explicit).isFalse()
        assertThat(local.hour).isEqualTo(3)
        assertThat(local.minute).isEqualTo(0)
        assertThat(local.toLocalDate()).isEqualTo(date)
    }

    @Test
    fun `omitted time uses personal envelope hour`() {
        val date = LocalDate.of(2026, 8, 10)
        val now = LocalDate.of(2026, 8, 9).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val envelope = EnvelopeHour(6, 30)
        val (millis, explicit) = TimeDefaults.resolveDueAt(
            date, null, zone, now, envelopeHour = envelope
        )
        val local = Instant.ofEpochMilli(millis).atZone(zone)
        assertThat(explicit).isFalse()
        assertThat(local.toLocalTime()).isEqualTo(envelope.toLocalTime())
        assertThat(local.toLocalDate()).isEqualTo(date)
    }

    @Test
    fun `explicit time is preserved`() {
        val date = LocalDate.of(2026, 8, 10)
        val time = LocalTime.of(14, 30)
        val now = LocalDate.of(2026, 8, 9).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (millis, explicit) = TimeDefaults.resolveDueAt(date, time, zone, now)
        val local = Instant.ofEpochMilli(millis).atZone(zone)
        assertThat(explicit).isTrue()
        assertThat(local.toLocalTime()).isEqualTo(time)
    }

    @Test
    fun `explicit time later today is kept`() {
        val date = LocalDate.of(2026, 8, 20)
        val time = LocalTime.of(16, 0)
        val now = date.atTime(15, 0).atZone(zone).toInstant().toEpochMilli()
        val (millis, explicit) = TimeDefaults.resolveDueAt(date, time, zone, now)
        val local = Instant.ofEpochMilli(millis).atZone(zone)
        assertThat(explicit).isTrue()
        assertThat(local.toLocalDate()).isEqualTo(date)
        assertThat(local.toLocalTime()).isEqualTo(time)
        assertThat(TimeDefaults.validateReminderInput("Walk", millis, now)).isNull()
    }

    @Test
    fun `explicit time already passed today sends immediately`() {
        val date = LocalDate.of(2026, 8, 20)
        val time = LocalTime.of(2, 11)
        val now = date.atTime(3, 8).atZone(zone).toInstant().toEpochMilli()
        val (millis, explicit) = TimeDefaults.resolveDueAt(date, time, zone, now)
        assertThat(explicit).isTrue()
        assertThat(millis).isEqualTo(now + TimeDefaults.SEND_NOW_OFFSET_MS)
        assertThat(TimeDefaults.validateReminderInput("Note", millis, now)).isNull()
    }

    @Test
    fun `omitted time rolls to next morning when envelope hour already passed`() {
        val date = LocalDate.of(2026, 8, 10)
        val now = date.atTime(15, 0).atZone(zone).toInstant().toEpochMilli()
        val envelope = EnvelopeHour(6, 30)
        val (millis, explicit) = TimeDefaults.resolveDueAt(
            date, null, zone, now, envelopeHour = envelope
        )
        val local = Instant.ofEpochMilli(millis).atZone(zone)
        assertThat(explicit).isFalse()
        assertThat(local.toLocalDate()).isEqualTo(date.plusDays(1))
        assertThat(local.toLocalTime()).isEqualTo(envelope.toLocalTime())
    }

    @Test
    fun `validation rejects blank title`() {
        assertThat(TimeDefaults.validateReminderInput("  ", System.currentTimeMillis() + 60_000))
            .isEqualTo("Title is required")
    }

    @Test
    fun `validation rejects a calendar date before today`() {
        val date = LocalDate.of(2026, 8, 19)
        val time = LocalTime.of(15, 0)
        val now = LocalDate.of(2026, 8, 20).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (millis, _) = TimeDefaults.resolveDueAt(date, time, zone, now)
        assertThat(TimeDefaults.validateReminderInput("Task", millis, now))
            .isEqualTo("Date cannot be in the past")
    }

    @Test
    fun `date picker UTC midnight maps to the same calendar day`() {
        val date = LocalDate.of(2026, 8, 20)
        val utcMillis = TimeDefaults.utcMillisFromLocalDate(date)
        assertThat(TimeDefaults.localDateFromUtcMillis(utcMillis)).isEqualTo(date)
    }
}
