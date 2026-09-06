package dev.openfit.app.data

import dev.openfit.app.data.local.dao.ExerciseDao
import dev.openfit.app.data.local.entity.ExerciseEntity
import dev.openfit.app.data.local.entity.Equipment
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val dao: ExerciseDao) {

    fun observeAll(): Flow<List<ExerciseEntity>> = dao.observeAll()

    fun observeMostUsed(): Flow<List<ExerciseEntity>> = dao.observeMostUsed()

    fun search(query: String): Flow<List<ExerciseEntity>> = dao.search(query)

    suspend fun getById(id: Long): ExerciseEntity? = dao.getById(id)

    suspend fun getByName(name: String): ExerciseEntity? = dao.getByName(name)

    suspend fun usageCount(id: Long): Int = dao.usageCount(id)

    suspend fun createCustom(name: String, muscleGroup: String, equipment: Equipment): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Exercise name must not be empty" }
        val group = muscleGroup.trim().ifEmpty { "Other" }
        return dao.insert(
            ExerciseEntity(name = trimmed, muscleGroup = group, equipment = equipment.name, isCustom = true)
        )
    }
}
