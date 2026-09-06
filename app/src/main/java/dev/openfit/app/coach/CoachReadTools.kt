package dev.openfit.app.coach

import dev.openfit.app.data.ExerciseRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.entity.SetPoint
import dev.openfit.app.data.local.entity.WorkoutWithExercises
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.data.macro.MealRepository
import dev.openfit.app.domain.OneRepMax
import dev.openfit.app.llm.ToolFunctionSpec
import dev.openfit.app.llm.ToolSpec
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Shared JSON-schema for the date-range argument. */
private fun dateRangeSchema(): JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject {
        put("start", buildJsonObject {
            put("type", "string")
            put("description", "Start date (inclusive), YYYY-MM-DD")
        })
        put("end", buildJsonObject {
            put("type", "string")
            put("description", "End date (inclusive), YYYY-MM-DD. Defaults to start.")
        })
    })
}

/** Returns every logged meal in a date range, with full details and totals. */
class NutritionTool(private val meals: MealRepository) : CoachTool {

    override val spec = ToolSpec(
        function = ToolFunctionSpec(
            name = "getNutrition",
            description = "Get nutrition (meal) data for a date range. " +
                "Returns every logged meal with full details (dish, calories, protein, carbs, fat, individual foods) " +
                "and daily/summary totals. Dates are YYYY-MM-DD. If 'end' is omitted, only 'start' is returned; " +
                "if both are omitted, today is returned.",
            parameters = dateRangeSchema(),
        )
    )

    override suspend fun run(args: JsonObject): String {
        val (from, to) = CoachDates.range(CoachArgs.string(args, "start"), CoachArgs.string(args, "end"))
        val list = meals.betweenOnce(from, to)
        return buildJsonObject {
            put("from", CoachDates.toIso(from))
            put("to", CoachDates.toIso(to - 1))
            put("mealCount", list.size)
            put("totals", buildJsonObject {
                put("calories", formatNum(list.sumOf { it.calories }))
                put("protein", formatNum(list.sumOf { it.protein }))
                put("carbs", formatNum(list.sumOf { it.carbs }))
                put("fat", formatNum(list.sumOf { it.fat }))
            })
            putJsonArray("meals") { list.forEach { add(mealToJson(it)) } }
        }.toString()
    }
}

private fun mealToJson(meal: MealEntry): JsonObject = buildJsonObject {
    put("timestamp", meal.timestamp)
    put("date", CoachDates.toIso(meal.timestamp))
    put("dish", meal.dish)
    put("calories", formatNum(meal.calories))
    put("protein", formatNum(meal.protein))
    put("carbs", formatNum(meal.carbs))
    put("fat", formatNum(meal.fat))
    put("isAuto", meal.isAuto)
    val foodsJson = meal.foodsJson.ifBlank { null }
    if (foodsJson != null) {
        val parsed = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(foodsJson) }.getOrNull()
        put("foods", parsed ?: JsonArray(emptyList()))
    } else {
        put("foods", JsonArray(emptyList()))
    }
}

/** Returns finished workouts in a date range with full exercise/set detail and volume. */
class WorkoutTool(private val workouts: WorkoutRepository) : CoachTool {

    override val spec = ToolSpec(
        function = ToolFunctionSpec(
            name = "getWorkouts",
            description = "Get workout (lifting) data for a date range. Returns each finished workout with its " +
                "exercises and every set (weight in kg, reps, warm-up flag), plus total volume. " +
                "Dates are YYYY-MM-DD. If 'end' is omitted, only 'start' is returned; if both are omitted, today is returned.",
            parameters = dateRangeSchema(),
        )
    )

    override suspend fun run(args: JsonObject): String {
        val (from, to) = CoachDates.range(CoachArgs.string(args, "start"), CoachArgs.string(args, "end"))
        val list = workouts.finishedBetween(from, to)
        return buildJsonObject {
            put("from", CoachDates.toIso(from))
            put("to", CoachDates.toIso(to - 1))
            put("workoutCount", list.size)
            putJsonArray("workouts") { list.forEach { add(workoutToJson(it)) } }
        }.toString()
    }
}

private fun workoutToJson(workout: WorkoutWithExercises): JsonObject {
    var volumeKg = 0.0
    return buildJsonObject {
        put("id", workout.workout.id)
        put("name", workout.workout.name)
        put("startDate", CoachDates.toIso(workout.workout.startedAt))
        put("startTime", workout.workout.startedAt)
        put("endedTime", workout.workout.endedAt ?: 0L)
        putJsonArray("exercises") {
            workout.exercises.forEach { entry ->
                add(buildJsonObject {
                    put("name", entry.exercise?.name ?: "Unknown")
                    put("muscleGroup", entry.exercise?.muscleGroup ?: "")
                    val working = entry.sets.filter { !it.isWarmup && it.completedAt != null }
                    working.forEach { volumeKg += it.weightKg * it.reps }
                    putJsonArray("sets") {
                        entry.sets.forEach { set ->
                            add(buildJsonObject {
                                put("weightKg", formatNum(set.weightKg))
                                put("reps", set.reps)
                                put("isWarmup", set.isWarmup)
                                put("completed", set.completedAt != null)
                            })
                        }
                    }
                })
            }
        }
        put("exerciseCount", workout.exercises.size)
        put("volumeKg", formatNum(volumeKg))
    }
}

/** Returns estimated 1RM progress points for a specific exercise across sessions. */
class ExerciseProgressTool(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
) : CoachTool {

    override val spec = ToolSpec(
        function = ToolFunctionSpec(
            name = "getExerciseProgress",
            description = "Get estimated 1RM strength progress for one exercise across sessions. " +
                "Requires 'exerciseName'; returns one point per session (date and best e1RM in kg). " +
                "Dates are YYYY-MM-DD. If 'end' is omitted, only 'start' is returned; if both omitted, all history is returned.",
            parameters = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("exerciseName", buildJsonObject {
                        put("type", "string")
                        put("description", "Exercise name, e.g. 'Bench Press'")
                    })
                    put("start", buildJsonObject {
                        put("type", "string")
                        put("description", "Start date (inclusive), YYYY-MM-DD")
                    })
                    put("end", buildJsonObject {
                        put("type", "string")
                        put("description", "End date (inclusive), YYYY-MM-DD")
                    })
                })
                put("required", JsonArray(listOf(JsonPrimitive("exerciseName"))))
            },
        )
    )

    override suspend fun run(args: JsonObject): String {
        val name = CoachArgs.string(args, "exerciseName")
            ?: return """{"error":"exerciseName is required"}"""
        val exercise = exercises.getByName(name)
            ?: return """{"error":"No exercise named '$name'"}"""

        val start = CoachArgs.string(args, "start")
        val end = CoachArgs.string(args, "end")
        val (from, to) = CoachDates.range(start, end)
        val inRange = start == null && end == null

        val points = workouts.setsExerciseHistory(exercise.id)
            .filter { inRange || it.date in from until to }

        return buildJsonObject {
            put("exercise", exercise.name)
            put("muscleGroup", exercise.muscleGroup)
            putJsonArray("sessions") {
                points
                    .groupBy { it.workoutId }
                    .values
                    .map { sets ->
                        val date = sets.minOf { it.date }
                        val e1rm = OneRepMax.formatE1rm(
                            OneRepMax.maxOf(sets.map { it.weightKg to it.reps })
                        )
                        Triple(date, e1rm, sets.size)
                    }
                    .sortedBy { it.first }
                    .forEach { (date, e1rm, count) ->
                        add(buildJsonObject {
                            put("date", CoachDates.toIso(date))
                            put("e1rmKg", formatNum(e1rm))
                            put("workingSetCount", count)
                        })
                    }
            }
        }.toString()
    }
}
