package dev.openfit.app.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * A chat client for an OpenAI-compatible endpoint that supports function/tool calling.
 * Unlike [LlmClient] (photo analysis), it streams multi-turn tool calls.
 */
open class ChatClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
    private val maxTokens: Int = 2048,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    open suspend fun chat(messages: List<ChatMessage>, tools: List<ToolSpec>): ChatResult =
        withContext(Dispatchers.IO) {
            val requestBody = buildRequestBody(messages, tools)
            val request = Request.Builder()
                .url(trimTrailingSlash(baseUrl) + "/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw RuntimeException("Chat request failed (${resp.code}): ${resp.body?.string()?.take(300)}")
                }
                val body = resp.body?.string() ?: throw RuntimeException("Empty chat response")
                val responseWire = json.decodeFromString(ChatResponseWire.serializer(), body)
                val choice = responseWire.choices.firstOrNull()
                    ?: throw RuntimeException("No choices in chat response")
                ChatResult(
                    content = choice.message.content,
                    toolCalls = choice.message.toolCalls.orEmpty().map { wire ->
                        ToolCall(id = wire.id, name = wire.function.name, arguments = wire.function.arguments)
                    },
                    finishReason = choice.finishReason,
                )
            }
        }

    private fun buildRequestBody(messages: List<ChatMessage>, tools: List<ToolSpec>): String {
        val wireMessages = messages.map { message ->
            ChatMessageWire(
                role = message.role,
                content = message.content,
                toolCallId = message.toolCallId,
                toolCalls = message.toolCalls.map { call ->
                    ToolCallWire(
                        id = call.id,
                        function = ToolCallFunctionWire(name = call.name, arguments = call.arguments)
                    )
                }.takeIf { it.isNotEmpty() },
            )
        }
        val request = ChatRequestWire(
            model = model,
            messages = wireMessages,
            tools = tools.takeIf { it.isNotEmpty() },
        )
        return json.encodeToString(ChatRequestWire.serializer(), request)
    }

    private fun trimTrailingSlash(s: String): String = s.trimEnd('/')
}
