package dev.openfit.app.domain

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Pure timing helpers for the daily coach schedule. */
object CoachTiming {

    /** The delay until the next occurrence of [minutesOfDay] since midnight, always positive. */
    fun nextDelay(minutesOfDay: Int, now: LocalDateTime = LocalDateTime.now()): Duration {
        val time = LocalTime.of(
            (minutesOfDay / 60).coerceIn(0, 23),
            (minutesOfDay % 60).coerceIn(0, 59),
        )
        val candidate = now.toLocalDate().atTime(time)
        val next = if (now < candidate) candidate else candidate.plusDays(1)
        return Duration.between(now, next)
    }
}
