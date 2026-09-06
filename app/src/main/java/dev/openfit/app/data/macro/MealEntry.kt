package dev.openfit.app.data.macro

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "meals")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val imagePath: String,
    val dish: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val foodsJson: String = "",
    val isAuto: Boolean = false,
)

@Serializable
data class MacroGoals(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
) {
    companion object {
        val DEFAULT = MacroGoals(2000.0, 150.0, 250.0, 70.0)
    }
}

@Serializable
data class MacroSettings(
    val baseUrl: String = "http://192.168.0.117:4000/v1",
    val apiKey: String = "",
    val model: String = "deepseek-v4-flash-vision-exp",
    val goals: MacroGoals = MacroGoals.DEFAULT,
)
