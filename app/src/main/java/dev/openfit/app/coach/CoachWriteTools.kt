package dev.openfit.app.coach

import dev.openfit.app.data.ExerciseRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.data.macro.MealRepository
import dev.openfit.app.llm.ToolFunctionSpec
import dev.openfit.app.llm.ToolSpec
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Logs a nutrition/meal entry. */
class LogMealTool(private val meals: MealRepository) : CoachTool {

    override val spec = ToolSpec(
        function = ToolFunctionSpec(
            name = "logMeal",
            description = "Log a meal (calories, protein, carbs, fat in grams). Use 'date' as YYYY-MM-DD to backdate, " +
                "or omit for the current time. Returns the created meal id.",
            parameters = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("dish", buildJsonObject {
                        put("type", "string")
                        put("description", "Short meal name, e.g. 'Chicken & rice'")
                    })
                    put("calories", buildJsonObject { put("type", "number") })
                    put("protein", buildJsonObject { put("type", "number") })
                    put("carbs", buildJsonObject { put("type", "number") })
                    put("fat", buildJsonObject { put("type", "number") })
                    put("date", buildJsonObject {
                        put("type", "string")
                        put("description", "YYYY-MM-DD, optional")
                    })
                })
                put("required", JsonArray(listOf(JsonPrimitive("dish"))))
            },
        )
    )

    override suspend fun run(args: JsonObject): String {
        val dish = CoachArgs.string(args, "dish")
            ?: return """{"error":"dish is required"}"""
        if (CoachArgs.double(args, "calories") == null) {
            return """{"error":"calories is required"}"""
        }
        val entry = MealEntry(
            timestamp = CoachDates.timestampFor(CoachArgs.string(args, "date"), System.currentTimeMillis()),
            imagePath = "",
            dish = dish,
            calories = CoachArgs.double(args, "calories") ?: 0.0,
            protein = CoachArgs.double(args, "protein") ?: 0.0,
            carbs = CoachArgs.double(args, "carbs") ?: 0.0,
            fat = CoachArgs.double(args, "fat") ?: 0.0,
        )
        val id = meals.add(entry)
        return buildJsonObject {
            put("ok", true)
            put("id", id)
            put("dish", entry.dish)
            put("calories", formatNum(entry.calories))
        }.toString()
    }
}

/** Starts a new workout session. */
class StartWorkoutTool(private val workouts: WorkoutRepository) : CoachTool {

    override val spec = ToolSpec(
        function = ToolFunctionSpec(
            name = "startWorkout",
            description = "Start a new workout session (creates the workout and makes it active). " +
                "Returns the workout id, which can be passed to 'addSet'.",
            parameters = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("name", buildJsonObject {
                        put("type", "string")
                        put("description", "Workout name, e.g. 'Push Day'")
                    })
                })
            },
        )
    )

    override suspend fun run(args: JsonObject): String {
        val name = CoachArgs.string(args, "name") ?: "Workout"
        val id = workouts.createWorkout(name)
        return buildJsonObject {
            put("ok", true)
            put("workoutId", id)
            put("name", name)
        }.toString()
    }
}

/** Adds a completed set to a workout (reusing the active workout by default). */
class AddSetTool(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
) : CoachTool {

    override val spec = ToolSpec(
        function = ToolFunctionSpec(
            name = "addSet",
            description = "Add a completed working set to a workout. By default the active workout is used, or a new one " +
                "is started if none is active. Pass 'workoutId' to target a specific workout. The exercise is created in " +
                "the workout if not already present.",
            parameters = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("exerciseName", buildJsonObject {
                        put("type", "string")
                        put("description", "Exercise name, e.g. 'Bench Press'")
                    })
                    put("weightKg", buildJsonObject {
                        put("type", "number")
                        put("description", "Weight in kg")
                    })
                    put("reps", buildJsonObject {
                        put("type", "integer")
                    })
                    put("isWarmup", buildJsonObject {
                        put("type", "boolean")
                    })
                    put("workoutId", buildJsonObject {
                        put("type", "integer")
                        put("description", "Optional workout id")
                    })
                })
                put("required", JsonArray(listOf(JsonPrimitive("exerciseName"), JsonPrimitive("weightKg"), JsonPrimitive("reps"))))
            },
        )
    )

    override suspend fun run(args: JsonObject): String {
        val exerciseName = CoachArgs.string(args, "exerciseName")
            ?: return """{"error":"exerciseName is required"}"""
        val weightKg = CoachArgs.double(args, "weightKg")
            ?: return """{"error":"weightKg is required"}"""
        val reps = CoachArgs.int(args, "reps")
            ?: return """{"error":"reps is required"}"""

        val exercise = exercises.getByName(exerciseName)
            ?: return """{"error":"No exercise named '$exerciseName'"}"""

        val requestedId = CoachArgs.long(args, "workoutId")
        val workout = (requestedId?.let { workouts.workoutById(it) }
            ?: workouts.activeWorkout()
            ?: workouts.workoutById(workouts.createWorkout("Workout")))
            ?: error("Could not create a workout")

        val entryId = workouts.exercisesOf(workout.id)
            .firstOrNull { it.exerciseId == exercise.id }?.id
            ?: workouts.addExerciseEntry(workout.id, exercise.id)

        val set = workouts.addSet(entryId, weightKg, reps, CoachArgs.boolean(args, "isWarmup"))

        return buildJsonObject {
            put("ok", true)
            put("workoutId", workout.id)
            put("workoutName", workout.name)
            put("exerciseName", exercise.name)
            put("weightKg", formatNum(weightKg))
            put("reps", reps)
            put("setId", set)
        }.toString()
    }
}
