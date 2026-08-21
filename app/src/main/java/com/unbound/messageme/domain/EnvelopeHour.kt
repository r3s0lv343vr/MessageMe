package com.unbound.messageme.domain

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** When overnight letters arrive if the user does not pick a clock time. */
data class EnvelopeHour(
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE
) {
    fun toLocalTime(): LocalTime =
        LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun clockLabel(): String = toLocalTime().format(CLOCK)

    companion object {
        const val DEFAULT_HOUR = 3
        const val DEFAULT_MINUTE = 0
        val DEFAULT = EnvelopeHour(DEFAULT_HOUR, DEFAULT_MINUTE)
        private val CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

        fun overnightCaption(hour: EnvelopeHour): String =
            "Overnight letter · arrives at ${hour.clockLabel()}"
    }
}
