package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import com.unbound.messageme.data.local.ReminderType
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun `plans pre-task reminders and unacked follow-ups`() {
        val due = LocalDate.of(2026, 8, 20)
            .atTime(LocalTime.of(18, 0))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val now = due - TimeDefaults.minutesToMillis(24 * 60)
        val planned = ReminderPlanner.planForTask("t1", due, timeWasExplicitlyChosen = true, nowEpochMillis = now)
        val types = planned.map { it.type }
        assertThat(types).containsAtLeast(
            ReminderType.T_MINUS_3H,
            ReminderType.T_MINUS_1H,
            ReminderType.T_MINUS_30M,
            ReminderType.T_MINUS_5M,
            ReminderType.UNACKED_1,
            ReminderType.UNACKED_2,
            ReminderType.UNACKED_3
        )
        assertThat(types).doesNotContain(ReminderType.DAYTIME_8AM)
    }

    @Test
    fun `includes daytime reminders when default 3am time used`() {
        val due = LocalDate.of(2026, 8, 20)
            .atTime(LocalTime.of(3, 0))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val now = due - TimeDefaults.minutesToMillis(48 * 60)
        val planned = ReminderPlanner.planForTask("t1", due, timeWasExplicitlyChosen = false, nowEpochMillis = now)
        val types = planned.map { it.type }
        assertThat(types).containsAtLeast(
            ReminderType.DAYTIME_8AM,
            ReminderType.DAYTIME_10AM,
            ReminderType.DAYTIME_3PM
        )
    }

    @Test
    fun `skips past triggers`() {
        val due = LocalDate.of(2026, 8, 20)
            .atTime(LocalTime.of(18, 0))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val now = due - TimeDefaults.minutesToMillis(10)
        val planned = ReminderPlanner.planForTask("t1", due, true, now)
        assertThat(planned.map { it.type }).contains(ReminderType.T_MINUS_5M)
        assertThat(planned.map { it.type }).doesNotContain(ReminderType.T_MINUS_3H)
    }
}
