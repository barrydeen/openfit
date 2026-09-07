package dev.openfit.app.coach

import dev.openfit.app.data.macro.MacroGoals
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** One finished workout within the report window, summarized. */
data class WorkoutDaySummary(
    val date: LocalDate,
    val name: String,
    val volumeKg: Double,
    val workingSets: Int,
)

/** Per-day nutrition totals within the report window. */
data class DayNutrition(
    val date: LocalDate,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val mealCount: Int,
)

/**
 * A snapshot of the last 7 days of training and nutrition, with the intelligence to describe it:
 * builds the LLM prompt and the deterministic fallback message when the AI endpoint is unavailable.
 * Pure Kotlin so it can be unit-tested on the JVM.
 */
data class DailyCoachReport(
    val today: LocalDate,
    val workouts: List<WorkoutDaySummary>,
    val days: List<DayNutrition>,
    val goals: MacroGoals,
) {

    /** Days since the most recent workout, or null if no workouts are logged in the window. */
    val daysSinceLastWorkout: Int?
        get() = workouts.maxOfOrNull { it.date }?.let { today.toEpochDay() - it.toEpochDay() }?.toInt()

    val workoutCount: Int get() = workouts.size

    /** Total working sets across all workouts in the window. */
    val workingSetCount: Int get() = workouts.sumOf { it.workingSets }

    val totalVolumeKg: Double get() = workouts.sumOf { it.volumeKg }

    /** Days in the window with no meals logged at all. */
    val daysWithoutMeals: Int get() = days.count { it.mealCount == 0 }

    val mealsLogged: Int get() = days.sumOf { it.mealCount }

    /** Average daily calories over logged days, or null when nothing was logged. */
    val averageDailyCalories: Double?
        get() = days.filter { it.mealCount > 0 }.takeIf { it.isNotEmpty() }
            ?.map { it.calories }?.average()

    /** Consecutive days trained, counting backwards from today or yesterday. */
    val consecutiveTrainingDays: Int
        get() {
            val dates = workouts.map { it.date }.toSet()
            var day = if (today in dates) today else today.minusDays(1)
            if (day !in dates) return 0
            var streak = 0
            while (day in dates) {
                streak++
                day = day.minusDays(1)
            }
            return streak
        }

    /**
     * A short deterministic coach message, used when no AI endpoint is configured or the request
     * fails. Rules roughly mirror what the AI is asked to do, prioritized from most to least urgent.
     */
    fun fallbackMessage(): String {
        val gap = daysSinceLastWorkout
        return when {
            workoutCount == 0 ->
                "No workouts in the last 7 days. Time to get back to the gym?"
            gap != null && gap >= BACK_TO_GYM_GAP_DAYS ->
                "It's been $gap days since your last workout. Time to get back to the gym."
            consecutiveTrainingDays >= REST_DAY_STREAK_DAYS || workoutCount >= REST_DAY_WEEKLY ->
                "You've trained ${if (consecutiveTrainingDays >= REST_DAY_STREAK_DAYS) "$consecutiveTrainingDays days in a row" else "$workoutCount times this week"}. A rest day will help you recover."
            daysWithoutMeals >= MISSED_MEAL_DAYS ->
                "No meals logged on $daysWithoutMeals of the last 7 days. Log your food to stay on track."
            calorieNudge() != null -> calorieNudge()!!
            else ->
                "You logged $workoutCount workouts and $mealsLogged meals this week. Keep it up - plan today's session."
        }
    }

    private fun calorieNudge(): String? {
        val average = averageDailyCalories ?: return null
        if (goals.calories <= 0.0) return null
        val ratio = average / goals.calories
        return when {
            ratio <= 1.0 - CALORIE_DRIFT_FRACTION -> {
                val pct = ((1.0 - ratio) * 100).roundToInt()
                "Your intake is averaging about $pct% below your calorie goal. Time to get back on track."
            }
            ratio >= 1.0 + CALORIE_DRIFT_FRACTION -> {
                val pct = ((ratio - 1.0) * 100).roundToInt()
                "Your intake is averaging about $pct% above your calorie goal. Time to get back on track."
            }
            else -> null
        }
    }

    /** The user-side prompt describing the last 7 days for the AI coach. */
    fun promptText(): String = buildString {
        appendLine("Write today's daily coach notification. Here is my data for the last 7 days (today is $today).")
        appendLine()
        append("WORKOUTS: ${workouts.size} in the last 7 days")
        if (daysSinceLastWorkout != null) append(", most recent one $daysSinceLastWorkout day(s) ago")
        appendLine(":")
        if (workouts.isEmpty()) {
            appendLine("(none)")
        } else {
            workouts.forEach { w ->
                appendLine(
                    "- ${w.date} ${w.name}: ${w.workingSets} working sets, " +
                        "${formatNum(w.volumeKg)} kg volume"
                )
            }
        }
        appendLine()
        appendLine("NUTRITION (daily goals: ${formatNum(goals.calories)} kcal, ${formatNum(goals.protein)} g protein):")
        days.forEach { d ->
            if (d.mealCount == 0) {
                appendLine("- ${d.date}: no meals logged")
            } else {
                appendLine(
                    "- ${d.date}: ${d.mealCount} meal(s), ${formatNum(d.calories)} kcal, " +
                        "${formatNum(d.protein)} g protein, ${formatNum(d.carbs)} g carbs, ${formatNum(d.fat)} g fat"
                )
            }
        }
        appendLine()
        appendLine(
            "Reply with ONE short push-notification message (1-2 sentences, at most 220 characters, plain " +
                "text, no markdown, no emoji, no quotation marks). Pick the most useful angle from the data, e.g. " +
                "suggest a rest day after several training days in a row, encourage getting back to the gym after " +
                "several days off, nudge nutrition back on track when logging is sparse or calories drift far from " +
                "goal, celebrate a consistent stretch, or reference concrete numbers (sets, volume, kcal, protein). " +
                "Be encouraging but honest."
        )
    }

    companion object {
        private const val BACK_TO_GYM_GAP_DAYS = 3
        private const val REST_DAY_STREAK_DAYS = 4
        private const val REST_DAY_WEEKLY = 6
        private const val MISSED_MEAL_DAYS = 3
        private const val CALORIE_DRIFT_FRACTION = 0.25

        /** Builds a report window covering the 7 days ending today, from raw epoch-milli data. */
        fun build(
            today: LocalDate,
            workouts: List<Triple<Long, String, Pair<Double, Int>>>,
            meals: List<MealPoint>,
            goals: MacroGoals,
            zone: ZoneId = ZoneId.systemDefault(),
        ): DailyCoachReport {
            val from = today.minusDays(6)
            val workoutSummaries = workouts
                .map { (startedAt, name, volumeSets) ->
                    WorkoutDaySummary(
                        date = startedAt.toLocalDate(zone),
                        name = name,
                        volumeKg = volumeSets.first,
                        workingSets = volumeSets.second,
                    )
                }
                .sortedBy { it.date }
            val days = (0L..6L).map { offset ->
                val date = from.plusDays(offset)
                val mealsOnDay = meals.filter { it.timestamp.toLocalDate(zone) == date }
                DayNutrition(
                    date = date,
                    calories = mealsOnDay.sumOf { it.calories },
                    protein = mealsOnDay.sumOf { it.protein },
                    carbs = mealsOnDay.sumOf { it.carbs },
                    fat = mealsOnDay.sumOf { it.fat },
                    mealCount = mealsOnDay.size,
                )
            }
            return DailyCoachReport(
                today = today,
                workouts = workoutSummaries,
                days = days,
                goals = goals,
            )
        }

        /** Max characters allowed for a notification message. */
        const val MAX_MESSAGE_LENGTH = 220
    }
}

/** Raw meal data point used to build [DailyCoachReport]. */
data class MealPoint(
    val timestamp: Long,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

private fun Long.toLocalDate(zone: ZoneId): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Trims/normalizes an AI-generated message into something notification-worthy. */
fun sanitizeCoachMessage(raw: String): String {
    var text = raw.trim()
        .replace("**", "")
        .replace("__", "")
        .removePrefix("\"").removeSuffix("\"")
        .replaceFirst("coach: ", "", ignoreCase = true)
        .trim()
    if (text.length > DailyCoachReport.MAX_MESSAGE_LENGTH) {
        text = text.take(DailyCoachReport.MAX_MESSAGE_LENGTH - 1).trimEnd() + "…"
    }
    return text
}
