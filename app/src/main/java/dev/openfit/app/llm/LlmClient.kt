package dev.openfit.app.llm

import dev.openfit.app.data.macro.MealEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.concurrent.TimeUnit

class LlmClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    suspend fun analyzeImage(imageBytes: ByteArray, mime: String, history: List<MealEntry>): Draft =
        withContext(Dispatchers.IO) {
            val b64 = Base64.getEncoder().encodeToString(imageBytes)
            val dataUrl = "data:$mime;base64,$b64"

            val historyText = history.joinToString("\n") {
                "- '${it.dish}': ${it.calories.toInt()} kcal, P ${it.protein.toInt()}g, C ${it.carbs.toInt()}g, F ${it.fat.toInt()}g"
            }

            val system = buildString {
                append(
                    "You are a nutrition analysis assistant. Given a photo of food, respond with ONLY a valid JSON object " +
                        "matching this exact schema (no markdown fences, no commentary): " +
                        "{\"dish\":\"short meal name\",\"foods\":[{\"name\":\"...\",\"grams\":N,\"calories\":N,\"protein\":N,\"carbs\":N,\"fat\":N}]," +
                        "\"total\":{\"calories\":N,\"protein\":N,\"carbs\":N,\"fat\":N}}."
                )
                if (historyText.isNotEmpty()) {
                    append(" These are meals the user has logged before (dish and macros) - if the photo matches one, reuse it or note the similarity:\n$historyText")
                }
                append(" Protein/carbs/fat are in grams, calories in kcal. Cover every visible component with a best-estimate portion weight.")
            }

            val body = buildString {
                append('{')
                append("\"model\":\"").append(escape(model)).append("\",")
                append("\"messages\":[")
                append("{\"role\":\"system\",\"content\":\"").append(escape(system)).append("\"},")
                append("{\"role\":\"user\",\"content\":[")
                append("{\"type\":\"text\",\"text\":\"Analyze the nutrients of the food in this photo.\"},")
                append("{\"type\":\"image_url\",\"image_url\":{\"url\":\"").append(escape(dataUrl)).append("\"}}")
                append("]}]}")
            }.toString()

            val request = Request.Builder()
                .url(trimTrailingSlash(baseUrl) + "/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body.toRequestBody(jsonType))
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw RuntimeException("LLM request failed (${resp.code}): ${resp.body?.string()?.take(300)}")
                }
                val responseJson = resp.body?.string() ?: throw RuntimeException("Empty LLM response")
                val dto = DraftJson.parseChat(responseJson)
                Draft(
                    dish = dto.dish,
                    foods = dto.foods.map { Food(it.name, it.grams, it.calories, it.protein, it.carbs, it.fat) },
                    calories = dto.total.calories,
                    protein = dto.total.protein,
                    carbs = dto.total.carbs,
                    fat = dto.total.fat,
                )
            }
        }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

    private fun trimTrailingSlash(s: String): String = s.trimEnd('/')
}
