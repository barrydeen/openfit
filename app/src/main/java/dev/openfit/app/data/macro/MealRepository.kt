package dev.openfit.app.data.macro

import kotlinx.coroutines.flow.Flow

class MealRepository(private val database: MacroDatabase) {

    private val dao = database.macroDao()

    val allMeals: Flow<List<MealEntry>> = dao.allMeals()

    fun between(start: Long, end: Long): Flow<List<MealEntry>> = dao.between(start, end)

    suspend fun add(entry: MealEntry): Long = dao.insert(entry)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun recent(limit: Int = 12): List<MealEntry> = dao.recent(limit)
}
