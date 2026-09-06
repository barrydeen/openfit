package dev.openfit.app.di

import android.content.Context
import dev.openfit.app.data.BackupManager
import dev.openfit.app.data.ExerciseRepository
import dev.openfit.app.data.ExerciseSeeder
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.AppDatabase
import dev.openfit.app.data.macro.MacroDatabase
import dev.openfit.app.data.macro.MealRepository
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

    init {
        ExerciseSeeder(applicationScope, appContext, database).seedIfNeeded()
    }
}
