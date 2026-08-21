package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnvelopeHourTest {
    @Test
    fun `clock label is 12-hour`() {
        assertThat(EnvelopeHour(6, 30).clockLabel()).isEqualTo("6:30 AM")
        assertThat(EnvelopeHour(15, 0).clockLabel()).isEqualTo("3:00 PM")
    }

    @Test
    fun `overnight caption names the ritual`() {
        assertThat(EnvelopeHour.overnightCaption(EnvelopeHour(6, 30)))
            .isEqualTo("Overnight letter · arrives at 6:30 AM")
    }
}
