package dev.openfit.app.coach

import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.entity.WorkoutWithExercises
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.llm.ChatClient
import dev.openfit.app.llm.ChatMessage
import kotlinx.coroutines.flow.first

/**
 * Fires a one-shot request to the AI coach right after a workout is finished, feeding it the
 * just-completed workout plus a few recent ones, asking for encouragement and tips for next time.
 */
class PostWorkoutCoach(
    private val workouts: WorkoutRepository,
    private val settings: SettingsRepository,
    private val makeClient: (MacroSettings) -> ChatClient,
) {

    /** Returns the coach's feedback, or null if the coach isn't configured or the request failed. */
    suspend fun feedbackFor(workoutId: Long): String? {
        val macro = settings.macroSettings.first()
        if (macro.apiKey.isBlank()) return null

        val latest = workouts.workoutDetail(workoutId) ?: return null
        val recent = workouts.recentFinishedWorkouts(RECENT_LIMIT)
            .filter { it.workout.id != latest.workout.id }
            .take(PREVIOUS_COUNT)

        val messages = listOf(
            ChatMessage.system(SYSTEM_PROMPT),
            ChatMessage.user(buildRequest(latest, recent)),
        )
        return runCatching {
            makeClient(macro).chat(messages, emptyList()).content
        }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun buildRequest(latest: WorkoutWithExercises, recent: List<WorkoutWithExercises>): String = buildString {
        appendLine("I just finished a workout. Here's what I did, plus a few of my recent workouts.")
        appendLine()
        appendLine("LATEST WORKOUT")
        appendLine(workoutToText(latest))
        if (recent.isNotEmpty()) {
            appendLine()
            appendLine("RECENT WORKOUTS")
            recent.forEach { appendLine(workoutToText(it)) }
        }
        appendLine()
        appendLine(
            "Give me a short, motivating coach note plus one or two specific, actionable tips to apply " +
                "in my next workout. Keep it concise and upbeat."
        )
    }

    private fun workoutToText(workout: WorkoutWithExercises): String = buildString {
        append("• ${workout.workout.name} (${CoachDates.toIso(workout.workout.startedAt)})")
        val lines = workout.exercises.mapIndexedNotNull { index, entry ->
            val working = entry.sets.filter { !it.isWarmup && it.completedAt != null }
            if (working.isEmpty()) {
                null
            } else {
                "${index + 1}. ${entry.exercise?.name ?: "Unknown"}: " +
                    working.joinToString(", ") { set ->
                        val weight = if (set.weightKg > 0.0) "${formatNum(set.weightKg)} kg" else "bodyweight"
                        "${set.reps} reps @ $weight"
                    }
            }
        }
        if (lines.isNotEmpty()) append("\n   " + lines.joinToString("\n   "))
    }

    companion object {
        private const val RECENT_LIMIT = 6
        private const val PREVIOUS_COUNT = 5

        private val SYSTEM_PROMPT = """
            You are OpenFit's personal strength coach. You are encouraging, specific, and concise.
            Only reference the workout data provided to you; never invent numbers or exercises.
            Give a motivating, upbeat message plus one or two concrete, actionable tips for the user's next session.
        """.trimIndent()
    }
}
