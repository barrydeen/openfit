package dev.openfit.app.coach

import dev.openfit.app.llm.ChatClient
import dev.openfit.app.llm.ChatMessage
import dev.openfit.app.llm.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Runs the coach tool-calling loop: send the conversation to the model, execute any requested
 * tool calls, feed the results back, and repeat until the model produces a final text answer.
 */
class CoachAgent(
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    tools: List<CoachTool>,
    private val onToolRun: ((String) -> Unit)? = null,
) {
    private val toolsByName: Map<String, CoachTool> = tools.associateBy { it.spec.function.name }
    private val history: ArrayDeque<ChatMessage> = ArrayDeque()

    fun reset() {
        history.clear()
    }

    /** Sends a user message and runs the loop, returning the final assistant text. */
    suspend fun reply(client: ChatClient, userText: String): String {
        history.addLast(ChatMessage.user(userText))
        trimHistory()

        repeat(MAX_ITERATIONS) {
            val result = client.chat(
                messages = listOf(ChatMessage.system(systemPrompt)) + history.toList(),
                tools = toolsByName.values.map { it.spec },
            )

            if (result.toolCalls.isEmpty()) {
                val text = result.content?.takeIf { it.isNotBlank() } ?: "Done."
                history.addLast(ChatMessage.assistant(text))
                return text
            }

            history.addLast(ChatMessage.assistant(content = null, toolCalls = result.toolCalls))
            for (call in result.toolCalls) {
                onToolRun?.invoke(call.name)
                val output = execute(call)
                history.addLast(ChatMessage.tool(call.id, output))
            }
            trimHistory()
        }

        val fallback = "I hit the step limit while working on that. Could you make the request a bit more specific?"
        history.addLast(ChatMessage.assistant(fallback))
        return fallback
    }

    private suspend fun execute(call: ToolCall): String {
        val tool = toolsByName[call.name]
            ?: return """{"error":"Unknown tool '${call.name}'"}"""
        val args = runCatching { Json.parseToJsonElement(call.arguments).jsonObject }.getOrElse {
            return """{"error":"Invalid arguments: ${it.message}"}"""
        }
        return try {
            tool.run(args)
        } catch (e: Exception) {
            """{"error":"${e.message ?: "Tool failed"}"}"""
        }
    }

    /** Drops oldest messages once the conversation grows too long for a single request. */
    private fun trimHistory() {
        while (history.size > MAX_HISTORY) {
            history.removeFirst()
        }
    }

    companion object {
        private const val MAX_ITERATIONS = 8
        private const val MAX_HISTORY = 60

        val DEFAULT_SYSTEM_PROMPT = """
            You are OpenFit's personal strength and nutrition coach. You help the user understand and improve
            their training and eating using only the data you fetch from tools.

            TOOLS
            - getNutrition(start, end): every logged meal with calories/protein/carbs/fat and foods.
            - getWorkouts(start, end): all finished workouts with exercises, sets (kg), and volume.
            - getExerciseProgress(exerciseName, start, end): best estimated 1RM per session for one exercise.
            - logMeal(dish, calories, protein, carbs, fat, date): add a nutrition entry (date is YYYY-MM-DD).
            - startWorkout(name): create a new active workout.
            - addSet(exerciseName, weightKg, reps, isWarmup, workoutId): add a completed set to a workout.

            RULES
            - Always fetch real data before giving numbers or advice; never invent figures.
            - Dates are YYYY-MM-DD. If no range is given, use a sensible default (today or the last 7 days).
            - Weights are stored and reported in kg. Note the unit when quoting them.
            - Be concise and concrete: summarize totals, trends, and give one actionable suggestion with reasons.
            - Before any write tool (logMeal, startWorkout, addSet), first briefly tell the user what you're about to log
              and confirm with them before calling it. After writing, confirm the change.
            - If a tool returns an error, explain it to the user and ask how they'd like to proceed.
        """.trimIndent()
    }
}
