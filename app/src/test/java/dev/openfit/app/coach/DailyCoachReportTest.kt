package dev.openfit.app.coach

import dev.openfit.app.data.macro.MacroGoals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyCoachReportTest {

    private val today = LocalDate.of(2026, 9, 7)
    private val goals = MacroGoals(calories = 2000.0, protein = 150.0, carbs = 250.0, fat = 70.0)

    private fun workout(daysAgo: Int, volume: Double = 5000.0, sets: Int = 20) =
        WorkoutDaySummary(today.minusDays(daysAgo.toLong()), "Push Day", volume, sets)

    private fun day(daysAgo: Int, calories: Double = 2000.0, meals: Int = 3) =
        DayNutrition(today.minusDays(daysAgo.toLong()), calories, 150.0, 250.0, 70.0, meals)

    private fun fullWeekOfMeals() = (0..6).map { day(it) }

    private fun report(workouts: List<WorkoutDaySummary>, days: List<DayNutrition> = fullWeekOfMeals()) =
        DailyCoachReport(today, workouts, days, goals)

    @Test
    fun `days since last workout`() {
        val r = report(listOf(workout(0), workout(2), workout(4)))
        assertEquals(0, r.daysSinceLastWorkout)
        assertEquals(3, report(listOf(workout(3))).daysSinceLastWorkout)
        assertNull(report(emptyList()).daysSinceLastWorkout)
    }

    @Test
    fun `training streak counts backwards from today or yesterday`() {
        assertEquals(3, report(listOf(workout(0), workout(1), workout(2))).consecutiveTrainingDays)
        assertEquals(2, report(listOf(workout(1), workout(2))).consecutiveTrainingDays)
        assertEquals(0, report(listOf(workout(3))).consecutiveTrainingDays)
    }

    @Test
    fun `no workouts suggests getting back to the gym`() {
        assertTrue(report(emptyList()).fallbackMessage().contains("Time to get back to the gym"))
    }

    @Test
    fun `several days off suggests getting back to the gym`() {
        val msg = report(listOf(workout(4))).fallbackMessage()
        assertTrue(msg.contains("4 days"))
        assertTrue(msg.contains("gym"))
    }

    @Test
    fun `long training streak suggests a rest day`() {
        val r = report(listOf(workout(0), workout(1), workout(2), workout(3)))
        assertTrue(r.fallbackMessage().contains("rest day"))
        assertTrue(r.fallbackMessage().contains("4 days in a row"))
    }

    @Test
    fun `very high weekly workout count suggests a rest day`() {
        val r = report((0..5).map { workout(it) })
        assertTrue(r.fallbackMessage().contains("rest day"))
    }

    @Test
    fun `missing meal logs nudges nutrition`() {
        val sparse = listOf(day(0), day(1)) + (2..6).map { day(it, meals = 0) }
        val r = report(listOf(workout(1)), sparse)
        assertTrue(r.fallbackMessage().contains("No meals logged on 5 of the last 7 days"))
    }

    @Test
    fun `calorie drift nudges getting back on track`() {
        val low = (0..6).map { day(it, calories = 1200.0) }
        assertTrue(report(listOf(workout(1)), low).fallbackMessage().contains("below your calorie goal"))
        val high = (0..6).map { day(it, calories = 2900.0) }
        assertTrue(report(listOf(workout(1)), high).fallbackMessage().contains("above your calorie goal"))
    }

    @Test
    fun `consistent week gets an encouraging default`() {
        val msg = report(listOf(workout(1), workout(3))).fallbackMessage()
        assertTrue(msg.contains("2 workouts"))
        assertTrue(msg.contains("21 meals"))
    }

    @Test
    fun `prompt includes real numbers and instructions`() {
        val r = report(listOf(workout(2, volume = 4500.0, sets = 18)))
        val prompt = r.promptText()
        assertTrue(prompt.contains("1 in the last 7 days"))
        assertTrue(prompt.contains("4500 kg volume"))
        assertTrue(prompt.contains("no meals logged") || prompt.contains("meal(s)"))
        assertTrue(prompt.contains("2000 kcal"))
        assertTrue(prompt.contains("rest day"))
        assertTrue(prompt.contains("220 characters"))
    }

    @Test
    fun `sanitize trims quotes, markdown and length`() {
        assertEquals("Nice work.", sanitizeCoachMessage(" **\"Nice work.\"** "))
        val long = "a".repeat(300)
        val sanitized = sanitizeCoachMessage(long)
        assertTrue(sanitized.length <= DailyCoachReport.MAX_MESSAGE_LENGTH)
        assertTrue(sanitized.endsWith("…"))
    }
}
