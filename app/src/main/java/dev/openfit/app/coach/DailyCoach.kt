package dev.openfit.app.coach

import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.macro.MacroGoals
import dev.openfit.app.data.macro.MealRepository
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.llm.ChatClient
import dev.openfit.app.llm.ChatMessage
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/** A daily coach message and whether it came from the AI endpoint or the offline fallback. */
data class DailyCoachMessage(val text: String, val fromAi: Boolean)

/**
 * Produces the once-a-day coach notification message by summarizing the last 7 days of meals and
 * workouts and asking the user's configured AI endpoint for a short, targeted note. Falls back to
 * deterministic advice (rest day / get back to the gym / back on track) when AI is unavailable.
 */
class DailyCoach(
    private val workouts: WorkoutRepository,
    private val meals: MealRepository,
    private val settings: SettingsRepository,
    private val makeClient: (MacroSettings) -> ChatClient,
) {

    suspend fun isEnabled(): Boolean = settings.coachEnabled.first()

    /** Generates the message; never throws - falls back when anything fails. */
    suspend fun generate(): DailyCoachMessage {
        val macro = settings.macroSettings.first()
        val report = buildReport(macro.goals)

        if (macro.apiKey.isBlank()) {
            return DailyCoachMessage(report.fallbackMessage(), fromAi = false)
        }

        val messages = listOf(
            ChatMessage.system(SYSTEM_PROMPT),
            ChatMessage.user(report.promptText()),
        )
        val text = runCatching {
            makeClient(macro).chat(messages, emptyList()).content
        }.getOrNull()?.let(::sanitizeCoachMessage)?.takeIf { it.isNotBlank() }

        return if (text != null) {
            DailyCoachMessage(text, fromAi = true)
        } else {
            DailyCoachMessage(report.fallbackMessage(), fromAi = false)
        }
    }

    private suspend fun buildReport(goals: MacroGoals): DailyCoachReport {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val from = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val workoutSummaries = workouts.finishedBetween(from, to).map { detail ->
            var volume = 0.0
            var sets = 0
            detail.exercises.forEach { entry ->
                entry.sets.filter { !it.isWarmup && it.completedAt != null }.forEach {
                    volume += it.weightKg * it.reps
                    sets++
                }
            }
            Triple(detail.workout.startedAt, detail.workout.name, volume to sets)
        }

        val mealPoints = meals.betweenOnce(from, to).map { meal ->
            MealPoint(meal.timestamp, meal.calories, meal.protein, meal.carbs, meal.fat)
        }

        return DailyCoachReport.build(today, workoutSummaries, mealPoints, goals, zone)
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are OpenFit's personal strength and nutrition coach writing a daily push notification.
            The user sends you their last 7 days of workouts and meals once per day.
            Follow the user's instructions for length and tone exactly. Only use the numbers provided;
            never invent data. If the data is empty, encourage starting today.
        """.trimIndent()
    }
}
