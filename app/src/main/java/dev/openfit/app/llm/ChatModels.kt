package dev.openfit.app.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/** A single tool-call request issued by the model. */
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

/** A message in the agent conversation. */
data class ChatMessage(
    val role: String,
    val content: String?,
    val toolCallId: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
) {
    companion object {
        const val SYSTEM = "system"
        const val USER = "user"
        const val ASSISTANT = "assistant"
        const val TOOL = "tool"

        fun system(content: String) = ChatMessage(SYSTEM, content)
        fun user(content: String) = ChatMessage(USER, content)
        fun assistant(content: String? = null, toolCalls: List<ToolCall> = emptyList()) =
            ChatMessage(ASSISTANT, content, toolCalls = toolCalls)
        fun tool(toolCallId: String, content: String) = ChatMessage(TOOL, content, toolCallId = toolCallId)
    }
}

/** A single LLM response for one turn of the tool loop. */
data class ChatResult(
    val content: String?,
    val toolCalls: List<ToolCall>,
    val finishReason: String?,
)

/** OpenAI-compatible function tool definition used in the request. */
@Serializable
data class ToolSpec(
    val type: String = "function",
    val function: ToolFunctionSpec,
)

@Serializable
data class ToolFunctionSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject = buildJsonObject { },
)

@Serializable
data class ToolCallWire(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunctionWire,
)

@Serializable
data class ToolCallFunctionWire(
    val name: String,
    val arguments: String,
)

@Serializable
data class ChatMessageWire(
    val role: String,
    val content: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallWire>? = null,
)

@Serializable
data class ChatRequestWire(
    val model: String,
    val messages: List<ChatMessageWire>,
    val tools: List<ToolSpec>? = null,
    @SerialName("tool_choice") val toolChoice: String = "auto",
    val temperature: Double? = null,
)

@Serializable
data class ChatResponseWire(
    val choices: List<ChoiceWire> = emptyList(),
)

@Serializable
data class ChoiceWire(
    val message: MessageWire = MessageWire(),
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class MessageWire(
    val role: String = "",
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallWire>? = null,
)
