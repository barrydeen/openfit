package dev.openfit.app.di

import android.content.Context
import dev.openfit.app.coach.AddSetTool
import dev.openfit.app.coach.CoachTool
import dev.openfit.app.coach.ExerciseProgressTool
import dev.openfit.app.coach.LogMealTool
import dev.openfit.app.coach.NutritionTool
import dev.openfit.app.coach.StartWorkoutTool
import dev.openfit.app.coach.WorkoutTool
import dev.openfit.app.data.BackupManager
import dev.openfit.app.data.ExerciseRepository
import dev.openfit.app.data.ExerciseSeeder
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.AppDatabase
import dev.openfit.app.data.macro.MacroDatabase
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.data.macro.MealRepository
import dev.openfit.app.llm.ChatClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase = AppDatabase.build(appContext)

    val macroDatabase: MacroDatabase = MacroDatabase.build(appContext)

    val exerciseRepository = ExerciseRepository(database.exerciseDao())

    val workoutRepository = WorkoutRepository(database.workoutDao(), database.exerciseDao())

    val mealRepository = MealRepository(macroDatabase)

    val settingsRepository = SettingsRepository(appContext)

    val backupManager = BackupManager(appContext, database, macroDatabase)

    val coachTools: List<CoachTool> = listOf(
        NutritionTool(mealRepository),
        WorkoutTool(workoutRepository),
        ExerciseProgressTool(workoutRepository, exerciseRepository),
        LogMealTool(mealRepository),
        StartWorkoutTool(workoutRepository),
        AddSetTool(workoutRepository, exerciseRepository),
    )

    fun chatClient(settings: MacroSettings): ChatClient =
        ChatClient(baseUrl = settings.baseUrl, apiKey = settings.apiKey, model = settings.model)

    init {
        ExerciseSeeder(applicationScope, appContext, database).seedIfNeeded()
    }
}
