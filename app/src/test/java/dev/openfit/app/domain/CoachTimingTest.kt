package dev.openfit.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class CoachTimingTest {

    private val now = LocalDateTime.of(2026, 9, 7, 10, 30)

    @Test
    fun `later today schedules for today`() {
        assertEquals(
            java.time.Duration.ofHours(7).plusMinutes(30),
            CoachTiming.nextDelay(18 * 60, now)
        )
    }

    @Test
    fun `earlier today schedules for tomorrow`() {
        val delay = CoachTiming.nextDelay(9 * 60, now)
        assertEquals(22, delay.toHours())
        assertEquals(30, delay.toMinutes() % 60)
    }

    @Test
    fun `exactly at the time rolls to tomorrow`() {
        val atNoon = LocalDateTime.of(2026, 9, 7, 12, 0)
        assertEquals(24, CoachTiming.nextDelay(12 * 60, atNoon).toHours())
    }

    @Test
    fun `out-of-range minutes are clamped to valid times`() {
        assertEquals(
            java.time.Duration.ofHours(12).plusMinutes(30),
            CoachTiming.nextDelay(24 * 60, now) // clamps to 23:00 today
        )
    }
}
