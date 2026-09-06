package dev.openfit.app.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FoodDto(
    val name: String = "",
    val grams: Double = 0.0,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
)

@Serializable
data class TotalDto(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
)

@Serializable
data class DraftDto(
    val dish: String = "Meal",
    val foods: List<FoodDto> = emptyList(),
    val total: TotalDto = TotalDto(),
)

data class Food(
    val name: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

data class Draft(
    val dish: String,
    val foods: List<Food>,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

object DraftJson {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    data class ChatResponse(
        val choices: List<Choice> = emptyList(),
    )
    @Serializable
    data class Choice(val message: Message = Message())
    @Serializable
    data class Message(val content: String = "")

    fun parseContent(content: String): DraftDto {
        val cleaned = content
            .trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        return json.decodeFromString(DraftDto.serializer(), cleaned)
    }

    fun parseChat(responseJson: String): DraftDto {
        val resp = json.decodeFromString(ChatResponse.serializer(), responseJson)
        val content = resp.choices.firstOrNull()?.message?.content ?: ""
        return parseContent(content)
    }

    fun foodsJson(foods: List<Food>): String {
        val dtos = foods.map { FoodDto(it.name, it.grams, it.calories, it.protein, it.carbs, it.fat) }
        return json.encodeToString(kotlinx.serialization.builtins.ListSerializer(FoodDto.serializer()), dtos)
    }
}
