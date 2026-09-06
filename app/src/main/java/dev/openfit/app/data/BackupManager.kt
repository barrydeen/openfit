package dev.openfit.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dev.openfit.app.data.local.AppDatabase
import dev.openfit.app.data.local.entity.ExerciseEntity
import dev.openfit.app.data.local.entity.WorkoutEntity
import dev.openfit.app.data.local.entity.WorkoutExerciseEntity
import dev.openfit.app.data.local.entity.WorkoutSetEntity
import dev.openfit.app.data.macro.MacroDatabase
import dev.openfit.app.data.macro.MealEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupExercise(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val isCustom: Boolean = false
)

@Serializable
data class BackupWorkout(
    val id: Long,
    val name: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val note: String? = null
)

@Serializable
data class BackupWorkoutExercise(
    val id: Long,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int
)

@Serializable
data class BackupWorkoutSet(
    val id: Long,
    val workoutExerciseId: Long,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
    val completedAt: Long? = null,
    val isWarmup: Boolean = false
)

@Serializable
data class BackupMeal(
    val id: Long,
    val timestamp: Long,
    val imagePath: String,
    val dish: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val foodsJson: String = "",
    val isAuto: Boolean = false
)

@Serializable
data class BackupData(
    val format: Int = 2,
    val exportedAt: Long,
    val exercises: List<BackupExercise>,
    val workouts: List<BackupWorkout>,
    val workoutExercises: List<BackupWorkoutExercise>,
    val workoutSets: List<BackupWorkoutSet>,
    val meals: List<BackupMeal> = emptyList()
)

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val macroDatabase: MacroDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportTo(uri: Uri): BackupData {
        val backup = BackupData(
            exportedAt = System.currentTimeMillis(),
            exercises = database.exerciseDao().getAll().map {
                BackupExercise(it.id, it.name, it.muscleGroup, it.equipment, it.isCustom)
            },
            workouts = database.workoutDao().getAllWorkouts().map {
                BackupWorkout(it.id, it.name, it.startedAt, it.endedAt, it.note)
            },
            workoutExercises = database.workoutDao().getAllWorkoutExercises().map {
                BackupWorkoutExercise(it.id, it.workoutId, it.exerciseId, it.position)
            },
            workoutSets = database.workoutDao().getAllSets().map {
                BackupWorkoutSet(it.id, it.workoutExerciseId, it.position, it.weightKg, it.reps, it.completedAt, it.isWarmup)
            },
            meals = macroDatabase.macroDao().getAll().map {
                BackupMeal(it.id, it.timestamp, it.imagePath, it.dish, it.calories, it.protein, it.carbs, it.fat, it.foodsJson, it.isAuto)
            }
        )
        val text = json.encodeToString(backup)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: error("Unable to open destination for backup")
        stream.use { it.write(text.toByteArray()) }
        return backup
    }

    suspend fun importFrom(uri: Uri): BackupData {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Unable to open backup file")
        val text = stream.bufferedReader().use { it.readText() }
        val backup = json.decodeFromString<BackupData>(text)
        database.withTransaction {
            database.workoutDao().deleteAllSets()
            database.workoutDao().deleteAllWorkoutExercises()
            database.workoutDao().deleteAllWorkouts()
            database.exerciseDao().deleteAll()

            database.exerciseDao().insertBackup(
                backup.exercises.map {
                    ExerciseEntity(it.id, it.name, it.muscleGroup, it.equipment, it.isCustom)
                }
            )
            database.workoutDao().insertBackupWorkouts(
                backup.workouts.map {
                    WorkoutEntity(it.id, it.name, it.startedAt, it.endedAt, it.note)
                }
            )
            database.workoutDao().insertBackupExercises(
                backup.workoutExercises.map {
                    WorkoutExerciseEntity(it.id, it.workoutId, it.exerciseId, it.position)
                }
            )
            database.workoutDao().insertBackupSets(
                backup.workoutSets.map {
                    WorkoutSetEntity(it.id, it.workoutExerciseId, it.position, it.weightKg, it.reps, it.completedAt, it.isWarmup)
                }
            )
        }
        macroDatabase.withTransaction {
            macroDatabase.macroDao().deleteAll()
            backup.meals.forEach {
                macroDatabase.macroDao().insert(
                    MealEntry(it.id, it.timestamp, it.imagePath, it.dish, it.calories, it.protein, it.carbs, it.fat, it.foodsJson, it.isAuto)
                )
            }
        }
        return backup
    }
}
