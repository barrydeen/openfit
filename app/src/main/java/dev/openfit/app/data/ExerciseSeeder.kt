package dev.openfit.app.data

import android.content.Context
import dev.openfit.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ExerciseSeeder(
    private val scope: CoroutineScope,
    private val context: Context,
    private val database: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun seedIfNeeded() {
        scope.launch { runCatching { seed() } }
    }

    internal suspend fun seed() {
        val text = context.assets.open(ExerciseCatalog.ASSET).bufferedReader().use { it.readText() }
        val dtos = json.decodeFromString<List<ExerciseCatalogDto>>(text)
        val existing = database.exerciseDao().getAll().map { it.name.lowercase() }.toSet()
        val missing = ExerciseCatalog.toEntities(dtos).filter { it.name.lowercase() !in existing }
        if (missing.isNotEmpty()) {
            database.exerciseDao().insertAll(missing)
        }
    }
}
