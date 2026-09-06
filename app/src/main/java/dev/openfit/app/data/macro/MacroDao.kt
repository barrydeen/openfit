package dev.openfit.app.data.macro

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {

    @Insert
    suspend fun insert(entry: MealEntry): Long

    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun allMeals(): Flow<List<MealEntry>>

    @Query("SELECT * FROM meals ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MealEntry>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun byId(id: Long): MealEntry?

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM meals WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun between(start: Long, end: Long): Flow<List<MealEntry>>

    @Query("SELECT * FROM meals WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getBetween(start: Long, end: Long): List<MealEntry>

    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    suspend fun getAll(): List<MealEntry>

    @Query("DELETE FROM meals")
    suspend fun deleteAll()
}
